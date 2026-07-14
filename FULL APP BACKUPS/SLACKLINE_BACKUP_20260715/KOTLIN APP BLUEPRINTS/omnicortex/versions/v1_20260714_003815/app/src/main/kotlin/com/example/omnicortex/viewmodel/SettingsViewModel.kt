package com.example.omnicortex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnicortex.data.prefs.AegisPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    val appPin        = AegisPreferences.appPinFlow(ctx)
    val biometric     = AegisPreferences.biometricFlow(ctx)
    val notifications = AegisPreferences.notificationsFlow(ctx)
    val darkTheme     = AegisPreferences.darkThemeFlow(ctx)

    // ── PIN setup flow states ─────────────────────────────────────────────────
    sealed class PinSetupState {
        object Idle         : PinSetupState()
        object EnterCurrent : PinSetupState()
        object EnterNew     : PinSetupState()
        object ConfirmNew   : PinSetupState()
        object Success      : PinSetupState()
        data class Error(val msg: String) : PinSetupState()
    }

    private val _pinSetupState = MutableStateFlow<PinSetupState>(PinSetupState.Idle)
    val pinSetupState: StateFlow<PinSetupState> = _pinSetupState

    private var _tempNewPin = ""

    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast
    fun consumeToast() { _toast.value = "" }

    fun startPinSetup(currentPin: String) {
        _pinSetupState.value = if (currentPin.isNotEmpty()) PinSetupState.EnterCurrent
                               else PinSetupState.EnterNew
    }

    fun submitCurrentPin(entered: String, actual: String) {
        _pinSetupState.value = if (entered == actual) PinSetupState.EnterNew
                               else PinSetupState.Error("Incorrect PIN")
    }

    fun submitNewPin(pin: String) {
        if (pin.length < 4) {
            _pinSetupState.value = PinSetupState.Error("PIN must be at least 4 digits")
            return
        }
        _tempNewPin = pin
        _pinSetupState.value = PinSetupState.ConfirmNew
    }

    fun confirmNewPin(pin: String) {
        if (pin == _tempNewPin) {
            viewModelScope.launch {
                AegisPreferences.setAppPin(ctx, _tempNewPin)
                // Also clear biometric flag in case it was previously set
                AegisPreferences.setBiometric(ctx, false)
                _tempNewPin = ""
                _pinSetupState.value = PinSetupState.Success
                _toast.value = "PIN set successfully."
            }
        } else {
            _pinSetupState.value = PinSetupState.Error("PINs do not match")
        }
    }

    fun removePin() {
        viewModelScope.launch {
            AegisPreferences.setAppPin(ctx, "")
            AegisPreferences.setBiometric(ctx, false)
            _toast.value = "PIN removed."
        }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            AegisPreferences.setBiometric(ctx, enabled)
        }
    }

    fun resetPinSetup() { _pinSetupState.value = PinSetupState.Idle; _tempNewPin = "" }

    fun setNotifications(v: Boolean) {
        viewModelScope.launch { AegisPreferences.setNotifications(ctx, v) }
    }

    fun setDarkTheme(v: Boolean) {
        viewModelScope.launch { AegisPreferences.setDarkTheme(ctx, v) }
    }
}
