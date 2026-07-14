package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.SecurityConfig
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    sealed class Mode {
        object Main : Mode()
        data class PinSetup(
            val isApp: Boolean,
            val step: Int,           // 0 = verify old, 1 = enter new, 2 = confirm
            val tempPin: String = ""
        ) : Mode()
    }

    private val _mode  = MutableStateFlow<Mode>(Mode.Main)
    val mode: StateFlow<Mode> = _mode

    private val _pin   = MutableStateFlow("")
    val pin: StateFlow<String> = _pin

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    fun startPin(isApp: Boolean, hasExisting: Boolean) {
        _pin.value   = ""
        _error.value = ""
        _mode.value  = Mode.PinSetup(isApp, if (hasExisting) 0 else 1)
    }

    fun appendDigit(d: String) { _pin.value += d }
    fun clearPin()              { _pin.value = ""; _error.value = "" }
    fun backPin()               { _pin.value = _pin.value.dropLast(1); _error.value = "" }
    fun goMain()                { _mode.value = Mode.Main; _pin.value = ""; _error.value = "" }
    fun consumeToast()          { _toast.value = "" }

    fun handleComplete(ctx: android.content.Context) {
        val m      = _mode.value as? Mode.PinSetup ?: return
        val stored = if (m.isApp) SecurityConfig.get(ctx).appPin else SecurityConfig.get(ctx).advPin
        val maxLen = if (m.isApp) 4 else 6
        val p      = _pin.value

        when (m.step) {
            0 -> {
                if (p == stored) {
                    _pin.value   = ""
                    _error.value = ""
                    _mode.value  = m.copy(step = 1)
                } else {
                    _error.value = "Incorrect PIN"
                    _pin.value   = ""
                }
            }
            1 -> {
                _pin.value   = ""
                _error.value = ""
                _mode.value  = m.copy(step = 2, tempPin = p)
            }
            2 -> {
                if (p == m.tempPin) {
                    viewModelScope.launch {
                        if (m.isApp) SecurityConfig.setAppPin(ctx, p)
                        else         SecurityConfig.setAdvPin(ctx, p)
                        _toast.value = "${if (m.isApp) "App" else "Advanced"} PIN saved."
                        goMain()
                    }
                } else {
                    _error.value = "PINs don't match. Try again."
                    _pin.value   = ""
                    _mode.value  = m.copy(step = 1, tempPin = "")
                }
            }
        }
    }

    fun removePin(ctx: android.content.Context, isApp: Boolean) = viewModelScope.launch {
        if (isApp) SecurityConfig.setAppPin(ctx, "") else SecurityConfig.setAdvPin(ctx, "")
        _toast.value = "${if (isApp) "App" else "Advanced"} PIN removed."
    }

    fun saveTitle(ctx: android.content.Context, name: String) = viewModelScope.launch {
        if (name.isBlank()) { _toast.value = "Title cannot be empty."; return@launch }
        SecurityConfig.setCompanyName(ctx, name)
        _toast.value = "Dashboard title saved."
    }

    fun toggleTheme(ctx: android.content.Context, current: String) = viewModelScope.launch {
        val next = if (current == "Dark") "Light" else "Dark"
        SecurityConfig.setTheme(ctx, next)
        _toast.value = "${if (next == "Dark") "AMOLED Black" else "Light"} theme applied."
    }

    fun toggleMatrixAnimation(ctx: android.content.Context, current: Boolean) = viewModelScope.launch {
        SecurityConfig.setMatrixAnimationEnabled(ctx, !current)
        _toast.value = "Background animation ${if (!current) "enabled" else "disabled"}."
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    vm: SettingsViewModel = viewModel()
) {
    val ctx       = LocalContext.current
    val cfg       by SecurityConfig.getFlow(ctx).collectAsState(initial = SecurityConfig.Cfg())
    val mode      by vm.mode.collectAsState()
    val pin       by vm.pin.collectAsState()
    val error     by vm.error.collectAsState()
    val toast     by vm.toast.collectAsState()
    val appColors  = LocalAppColors.current

    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    val m = mode
    if (m is SettingsViewModel.Mode.PinSetup) {
        val maxLen = if (m.isApp) 4 else 6
        val title  = if (m.isApp) "App PIN Setup" else "Advanced PIN Setup"
        val sub    = when (m.step) {
            0    -> "Enter your current PIN."
            1    -> "Enter a new $maxLen-digit PIN."
            else -> "Confirm your new PIN."
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.bg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            Text(title, color = TealCol, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            BodyText(sub, SubText)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(maxLen) { i ->
                    Text(
                        text     = if (i < pin.length) "●" else "○",
                        fontSize = 32.sp,
                        color    = if (i < pin.length) TealCol else SubText
                    )
                }
            }
            if (error.isNotEmpty()) Text(error, color = RedCol, fontSize = 13.sp)
            NumberPad(
                enteredDigits = pin,
                maxLen        = maxLen,
                onDigit       = { d ->
                    vm.appendDigit(d)
                    if ((pin + d).length == maxLen) vm.handleComplete(ctx)
                },
                onClear = { vm.clearPin() },
                onBack  = { vm.backPin() }
            )
            TextButton(onClick = { vm.goMain() }) { Text("Cancel", color = SubText) }
        }
        return
    }

    var companyName by remember(cfg.companyName) { mutableStateOf(cfg.companyName) }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Global Settings", CyanCol, 20)

            // Appearance
            AppCard {
                SectionLabel("Appearance", PurpleCol)
                BodyText("Switch between AMOLED Black and Light Theme.", SubText)
                Text(
                    "Current: ${if (cfg.theme == "Light") "Light Theme" else "AMOLED Black"}",
                    color = PurpleCol, fontWeight = FontWeight.Bold
                )
                ActionButton(
                    if (cfg.theme == "Dark") "Switch to Light Theme" else "Switch to AMOLED Black",
                    PurpleCol
                ) { vm.toggleTheme(ctx, cfg.theme) }
            }

            // Dashboard title
            AppCard {
                SectionLabel("Dashboard Title", BlueCol)
                BodyText("Set the company or app name displayed on the home screen.", SubText)
                AppTextField(companyName, { companyName = it }, "Company Name")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { companyName = "" }) { Text("Clear", color = RedCol) }
                }
                ActionButton("Save Title", BlueCol) { vm.saveTitle(ctx, companyName) }
            }

            // Background animation
            AppCard {
                SectionLabel("Background Animation", TealCol)
                BodyText("Toggle the animated matrix background on the home screen. Turning it off saves battery.", SubText)
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (cfg.matrixAnimationEnabled) "Currently ON" else "Currently OFF",
                        color      = if (cfg.matrixAnimationEnabled) TealCol else SubText,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = cfg.matrixAnimationEnabled,
                        onCheckedChange = { vm.toggleMatrixAnimation(ctx, cfg.matrixAnimationEnabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = TealCol,
                            checkedTrackColor   = TealCol.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // App PIN
            AppCard {
                SectionLabel("App Access Lock", GreenCol)
                val active = cfg.appPin.isNotEmpty()
                Text(
                    if (active) "PIN IS ACTIVE - Tap to modify" else "Tap to set a new 4-digit PIN",
                    color = if (active) GreenCol else SubText
                )
                ActionButton("Set / Change App PIN", GreenCol) { vm.startPin(true, active) }
                if (active) ActionButton("Remove App PIN", RedCol) { vm.removePin(ctx, true) }
            }

            // Advanced PIN
            AppCard {
                SectionLabel("Advanced Action Security", RedCol)
                val active = cfg.advPin.isNotEmpty()
                Text(
                    if (active) "PIN IS ACTIVE - Tap to modify"
                    else "Tap to set a 6-digit PIN for sensitive actions",
                    color = if (active) RedCol else SubText
                )
                ActionButton("Set / Change Advanced PIN", RedCol) { vm.startPin(false, active) }
                if (active) ActionButton("Remove Advanced PIN", RedCol) { vm.removePin(ctx, false) }
            }

            // SHV Store Config
            AppCard {
                SectionLabel("SHV Store Admin", TealCol)
                BodyText("Supabase credentials for the SHV Store Admin section.", SubText)
                val storeSet = cfg.storeEmail.isNotEmpty()
                Text(
                    if (storeSet) "Credentials saved ✓" else "No credentials set",
                    color = if (storeSet) TealCol else SubText
                )
                ActionButton("Configure Store Credentials", TealCol) { onNavigate("store_config") }
            }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }
}
