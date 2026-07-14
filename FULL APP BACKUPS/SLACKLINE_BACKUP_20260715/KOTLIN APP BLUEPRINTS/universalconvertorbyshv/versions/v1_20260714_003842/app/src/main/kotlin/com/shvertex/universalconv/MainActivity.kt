package com.shvertex.universalconv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shvertex.universalconv.navigation.AppNavGraph
import com.shvertex.universalconv.ui.theme.SHVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SHVTheme {
                AppNavGraph()
            }
        }
    }
}
