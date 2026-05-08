package com.kaijen.btpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kaijen.btpower.ui.navigation.AppNavGraph
import com.kaijen.btpower.ui.theme.BtPowerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BtPowerTheme {
                AppNavGraph()
            }
        }
    }
}
