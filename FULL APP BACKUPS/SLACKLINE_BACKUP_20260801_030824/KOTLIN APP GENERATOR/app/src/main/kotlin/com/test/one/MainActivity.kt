package com.test.one

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.test.one.databinding.ActivityMainBinding
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(null)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_more))
        binding.navView.setupWithNavController(navController)

        // Back: go to Home if not there, else show exit dialog
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeId = R.id.navigation_home
                if (navController.currentDestination?.id != homeId) {
                    navController.navigate(homeId)
                } else {
                    showExitDialog()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                navController.navigate(R.id.navigation_more)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun isDarkMode() = prefs.getBoolean("dark_mode", true)

    fun toggleTheme(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
        recreate()
    }

    private fun applyTheme() {
        val isDark = prefs.getBoolean("dark_mode", true)
        setTheme(if (isDark) R.style.Theme_AppTheme else R.style.Theme_AppTheme)
        // Theme.AppTheme in values-night/ handles AMOLED automatically via DayNight
        if (isDark) {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_title))
            .setMessage(getString(R.string.exit_message))
            .setPositiveButton(getString(R.string.exit_yes)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.exit_no), null)
            .show()
    }
}