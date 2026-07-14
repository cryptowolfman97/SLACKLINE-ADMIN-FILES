package com.shvertex.supaadmin

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.shvertex.supaadmin.data.SupabaseStorage
import com.shvertex.supaadmin.ui.screens.SupaAdminApp
import com.shvertex.supaadmin.ui.theme.SupaAdminTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialise storage — must happen before any SupabaseStorage call
        // This also triggers the one-time migration from plaintext credentials
        SupabaseStorage.init(applicationContext)
        com.shvertex.supaadmin.license.SupaAccount.init(applicationContext)

        // MANAGE_EXTERNAL_STORAGE required on Android 11+ to write to Downloads/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        setContent {
            val settings = SupabaseStorage.loadSettings()
            SupaAdminTheme(darkTheme = settings.dark_mode) {
                SupaAdminApp()
            }
        }
    }
}
