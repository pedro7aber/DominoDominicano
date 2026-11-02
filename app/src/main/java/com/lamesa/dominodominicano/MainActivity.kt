package com.lamesa.dominodominicano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamesa.dominodominicano.data.SettingsRepository
import com.lamesa.dominodominicano.data.SettingsViewModel
import com.lamesa.dominodominicano.data.SettingsViewModelFactory
import com.lamesa.dominodominicano.ui.screens.AppNavHost
import com.lamesa.dominodominicano.ui.theme.DominoDominicanoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = SettingsRepository(applicationContext)
        val factory = SettingsViewModelFactory(settingsRepo)

        setContent {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            DominoDominicanoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
