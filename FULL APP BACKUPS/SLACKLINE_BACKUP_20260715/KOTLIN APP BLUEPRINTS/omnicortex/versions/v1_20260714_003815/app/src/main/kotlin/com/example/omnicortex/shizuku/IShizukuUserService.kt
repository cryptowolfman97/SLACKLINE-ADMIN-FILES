package com.example.omnicortex.shizuku

import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

/**
 * Hand-written replacement for what `aidl` would normally generate from an
 * .aidl file. We do this by hand because the `aidl` compiler binary in this
 * AndroidIDE/ARM64 toolchain fails on invocation regardless of file content
 * (same category of issue as Room/KSP on this environment) — so we skip
 * codegen entirely and implement the Binder contract directly.
 *
 * This is exactly what `aidl` would have produced under the hood: an
 * IInterface, a Stub base class servers extend, and a Proxy the client side
 * uses to talk to the remote Binder.
 */
interface IShizukuUserService : IInterface {

    fun setUidNetworkBlocked(uid: Int, blocked: Boolean): Boolean
    fun isUidNetworkBlocked(uid: Int): Boolean
    fun grantPermission(packageName: String, permission: String): Boolean
    fun revokePermission(packageName: String, permission: String): Boolean
    fun getNetstatsDump(): String
    fun ping(): Int
    fun destroy()

    companion object {
        const val DESCRIPTOR = "com.example.omnicortex.shizuku.IShizukuUserService"

        const val TRANSACTION_setUidNetworkBlocked = IBinder.FIRST_CALL_TRANSACTION
        const val TRANSACTION_isUidNetworkBlocked   = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_grantPermission       = IBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_revokePermission      = IBinder.FIRST_CALL_TRANSACTION + 3
        const val TRANSACTION_getNetstatsDump       = IBinder.FIRST_CALL_TRANSACTION + 4
        const val TRANSACTION_ping                  = IBinder.FIRST_CALL_TRANSACTION + 5
        const val TRANSACTION_destroy               = IBinder.FIRST_CALL_TRANSACTION + 6

        fun asInterface(binder: IBinder?): IShizukuUserService? {
            if (binder == null) return null
            val iin = binder.queryLocalInterface(DESCRIPTOR)
            if (iin is IShizukuUserService) return iin
            return Proxy(binder)
        }
    }

    abstract class Stub : android.os.Binder(), IShizukuUserService {
        init { attachInterface(this, DESCRIPTOR) }

        override fun asBinder(): IBinder = this

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                android.os.IBinder.INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    return true
                }
                TRANSACTION_setUidNetworkBlocked -> {
                    data.enforceInterface(DESCRIPTOR)
                    val uid = data.readInt()
                    val blocked = data.readInt() != 0
                    val result = setUidNetworkBlocked(uid, blocked)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    return true
                }
                TRANSACTION_isUidNetworkBlocked -> {
                    data.enforceInterface(DESCRIPTOR)
                    val uid = data.readInt()
                    val result = isUidNetworkBlocked(uid)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    return true
                }
                TRANSACTION_grantPermission -> {
                    data.enforceInterface(DESCRIPTOR)
                    val pkg = data.readString() ?: ""
                    val perm = data.readString() ?: ""
                    val result = grantPermission(pkg, perm)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    return true
                }
                TRANSACTION_revokePermission -> {
                    data.enforceInterface(DESCRIPTOR)
                    val pkg = data.readString() ?: ""
                    val perm = data.readString() ?: ""
                    val result = revokePermission(pkg, perm)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    return true
                }
                TRANSACTION_getNetstatsDump -> {
                    data.enforceInterface(DESCRIPTOR)
                    val result = getNetstatsDump()
                    reply?.writeNoException()
                    reply?.writeString(result)
                    return true
                }
                TRANSACTION_ping -> {
                    data.enforceInterface(DESCRIPTOR)
                    val result = ping()
                    reply?.writeNoException()
                    reply?.writeInt(result)
                    return true
                }
                TRANSACTION_destroy -> {
                    data.enforceInterface(DESCRIPTOR)
                    destroy()
                    reply?.writeNoException()
                    return true
                }
                else -> return super.onTransact(code, data, reply, flags)
            }
        }
    }

    class Proxy(private val remote: IBinder) : IShizukuUserService {
        override fun asBinder(): IBinder = remote

        private fun transactBoolean(code: Int, block: (Parcel) -> Unit): Boolean {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                block(data)
                remote.transact(code, data, reply, 0)
                reply.readException()
                return reply.readInt() != 0
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun setUidNetworkBlocked(uid: Int, blocked: Boolean): Boolean =
            transactBoolean(TRANSACTION_setUidNetworkBlocked) {
                it.writeInt(uid)
                it.writeInt(if (blocked) 1 else 0)
            }

        override fun isUidNetworkBlocked(uid: Int): Boolean =
            transactBoolean(TRANSACTION_isUidNetworkBlocked) { it.writeInt(uid) }

        override fun grantPermission(packageName: String, permission: String): Boolean =
            transactBoolean(TRANSACTION_grantPermission) {
                it.writeString(packageName)
                it.writeString(permission)
            }

        override fun revokePermission(packageName: String, permission: String): Boolean =
            transactBoolean(TRANSACTION_revokePermission) {
                it.writeString(packageName)
                it.writeString(permission)
            }

        override fun getNetstatsDump(): String {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                remote.transact(TRANSACTION_getNetstatsDump, data, reply, 0)
                reply.readException()
                return reply.readString() ?: ""
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun ping(): Int {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                remote.transact(TRANSACTION_ping, data, reply, 0)
                reply.readException()
                return reply.readInt()
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        override fun destroy() {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                remote.transact(TRANSACTION_destroy, data, reply, 0)
                reply.readException()
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
    }
}