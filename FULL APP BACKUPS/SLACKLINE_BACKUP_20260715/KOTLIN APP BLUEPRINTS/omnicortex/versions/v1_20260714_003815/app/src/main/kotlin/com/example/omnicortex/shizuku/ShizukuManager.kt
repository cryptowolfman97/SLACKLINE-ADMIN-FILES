package com.example.omnicortex.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnBinderDeadListener
import rikka.shizuku.Shizuku.OnBinderReceivedListener
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener

/**
 * ShizukuManager
 *
 * Single entry point the rest of the app (ViewModels, screens) talks to
 * for anything "Shizuku Mode" related. Wraps:
 *  - Detecting whether the Shizuku app/service is even installed & running
 *    (works identically whether the user granted it via root OR via
 *    wireless debugging / ADB pairing - Shizuku abstracts that difference)
 *  - Requesting the Shizuku permission (separate from Android runtime perms)
 *  - Binding the privileged IShizukuUserService and exposing clean suspend
 *    functions instead of raw Binder calls
 *
 * This is a Pro+ gated feature - callers should check the user's tier
 * before surfacing "Shizuku Mode" in the UI at all.
 */
object ShizukuManager {

    private const val REQUEST_CODE = 9001

    sealed class Availability {
        data object NotInstalled : Availability()   // Shizuku app not present
        data object NotRunning : Availability()      // installed but service not started
        data object PermissionDenied : Availability()
        data object Ready : Availability()
    }

    private val _availability = MutableStateFlow<Availability>(Availability.NotInstalled)
    val availability: StateFlow<Availability> = _availability.asStateFlow()

    private var service: IShizukuUserService? = null
    private var bindingDeferred: CompletableDeferred<IShizukuUserService?>? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.example.omnicortex", ShizukuUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_service")
        .debuggable(false)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = if (binder?.pingBinder() == true) IShizukuUserService.asInterface(binder) else null
            bindingDeferred?.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val permissionResultListener = OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            _availability.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindService()
                Availability.Ready
            } else {
                Availability.PermissionDenied
            }
        }
    }

    private val binderReceivedListener = OnBinderReceivedListener {
        refreshAvailability()
    }

    private val binderDeadListener = OnBinderDeadListener {
        service = null
        _availability.value = Availability.NotRunning
    }

    /** Call once, e.g. from Application.onCreate(), before any screen uses ShizukuManager. */
    fun init() {
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        refreshAvailability()
    }

    fun refreshAvailability() {
        _availability.value = when {
            !isShizukuAppInstalled() -> Availability.NotInstalled
            !Shizuku.pingBinder()    -> Availability.NotRunning
            checkSelfPermission()    -> Availability.Ready.also { bindService() }
            else                     -> Availability.PermissionDenied
        }
    }

    /**
     * Works identically whether the user's Shizuku is running via root OR
     * via "adb shell" over wireless debugging - Shizuku's own service
     * abstracts that away, so this manager (and the rest of the app) never
     * needs to know or care which backend the user chose.
     */
    private fun isShizukuAppInstalled(): Boolean = try {
        Shizuku.pingBinder()
        true
    } catch (_: Throwable) {
        false
    }

    private fun checkSelfPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    /** Triggers the Shizuku permission dialog. Result comes back via the listener above. */
    fun requestPermission() {
        if (Shizuku.isPreV11()) return // unsupported ancient Shizuku version
        if (checkSelfPermission()) {
            _availability.value = Availability.Ready
            bindService()
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun bindService() {
        if (service != null) return
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (_: Throwable) {
            _availability.value = Availability.NotRunning
        }
    }

    private suspend fun awaitService(): IShizukuUserService? {
        service?.let { return it }
        if (_availability.value != Availability.Ready) return null
        val deferred = CompletableDeferred<IShizukuUserService?>()
        bindingDeferred = deferred
        bindService()
        return deferred.await()
    }

    // ── Public suspend API used by ViewModels ──────────────────────────────

    suspend fun setAppNetworkBlocked(uid: Int, blocked: Boolean): Boolean = withContext(Dispatchers.IO) {
        awaitService()?.setUidNetworkBlocked(uid, blocked) ?: false
    }

    suspend fun isAppNetworkBlocked(uid: Int): Boolean = withContext(Dispatchers.IO) {
        awaitService()?.isUidNetworkBlocked(uid) ?: false
    }

    suspend fun grantPermission(packageName: String, permission: String): Boolean = withContext(Dispatchers.IO) {
        awaitService()?.grantPermission(packageName, permission) ?: false
    }

    suspend fun revokePermission(packageName: String, permission: String): Boolean = withContext(Dispatchers.IO) {
        awaitService()?.revokePermission(packageName, permission) ?: false
    }

    suspend fun getNetstatsDump(): String = withContext(Dispatchers.IO) {
        awaitService()?.getNetstatsDump() ?: ""
    }

    fun teardown(context: Context) {
        try { service?.destroy() } catch (_: Throwable) {}
        try { Shizuku.unbindUserService(userServiceArgs, connection, true) } catch (_: Throwable) {}
        service = null
    }
}