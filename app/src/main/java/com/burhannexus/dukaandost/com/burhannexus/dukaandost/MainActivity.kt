package com.burhannexus.dukaandost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.burhannexus.dukaandost.ui.DukaanDostRoot
import com.burhannexus.dukaandost.ui.theme.DukaanDostTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            DukaanDostTheme {
                DukaanDostRoot()
            }
        }
    }
}
