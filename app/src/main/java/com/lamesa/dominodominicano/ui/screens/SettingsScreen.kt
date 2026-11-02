package com.lamesa.dominodominicano.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lamesa.dominodominicano.R
import com.lamesa.dominodominicano.data.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val currentLang by viewModel.language.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Text(
                text = stringResource(R.string.language),
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            LanguageOption(
                text = stringResource(R.string.language_spanish),
                selected = currentLang == "es",
                onClick = { viewModel.setLanguage("es") }
            )
            LanguageOption(
                text = stringResource(R.string.language_english),
                selected = currentLang == "en",
                onClick = { viewModel.setLanguage("en") }
            )
        }
    }
}

@Composable
private fun LanguageOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        modifier = Modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.RadioButton
        )
    )
}
