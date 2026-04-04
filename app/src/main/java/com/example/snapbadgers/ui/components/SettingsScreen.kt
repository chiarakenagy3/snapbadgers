package com.example.snapbadgers.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapbadgers.data.SettingsRepository
import androidx.compose.foundation.layout.ColumnScope
import com.example.snapbadgers.ui.i18n.AppI18n
import com.example.snapbadgers.ui.theme.Zinc400
import com.example.snapbadgers.ui.theme.Zinc500
import com.example.snapbadgers.ui.theme.Zinc800

@Composable
fun SettingsScreen(settingsRepository: SettingsRepository) {
    val spotifyKey = settingsRepository.spotifyKey.value
    val displayName = settingsRepository.displayName.value
    val email = settingsRepository.email.value
    val themeMode = settingsRepository.themeMode.value
    val language = settingsRepository.language.value
    val notificationsEnabled = settingsRepository.notificationsEnabled.value
    val personalizationEnabled = settingsRepository.personalizationEnabled.value
    val strings = AppI18n.forLanguage(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = strings.settings,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        SettingsSection(title = strings.spotifyApi) {
            SettingsInputField(
                label = strings.spotifyClientId,
                value = spotifyKey,
                onValueChange = { settingsRepository.updateSpotifyKey(it) },
                placeholder = "Enter your Spotify Client ID"
            )
        }

        SettingsSection(title = strings.account) {
            SettingsInputField(
                label = strings.displayName,
                value = displayName,
                onValueChange = { settingsRepository.updateDisplayName(it) },
                placeholder = "Enter your name"
            )
            SettingsInputField(
                label = strings.email,
                value = email,
                onValueChange = { settingsRepository.updateEmail(it) },
                placeholder = "Enter your email"
            )
        }

        SettingsSection(title = strings.appearance) {
            SettingsOptionSelector(
                label = strings.theme,
                description = "Choose app brightness and color preference",
                options = listOf("System", "Dark", "Light"),
                selectedOption = themeMode,
                onOptionSelected = { settingsRepository.updateThemeMode(it) }
            )
        }

        SettingsSection(title = strings.language) {
            SettingsOptionSelector(
                label = strings.language,
                description = "Set your preferred display language",
                options = listOf("English", "Chinese", "Spanish"),
                selectedOption = language,
                onOptionSelected = { settingsRepository.updateLanguage(it) }
            )
        }

        SettingsSection(title = strings.preferences) {
            SettingsToggle(
                label = strings.pushNotifications,
                description = "Receive alerts for fresh recommendations",
                checked = notificationsEnabled,
                onCheckedChange = { settingsRepository.updateNotificationsEnabled(it) }
            )
            SettingsToggle(
                label = strings.personalizedRecommendations,
                description = "Use your history and profile to improve suggestions",
                checked = personalizationEnabled,
                onCheckedChange = { settingsRepository.updatePersonalizationEnabled(it) }
            )
        }

        SettingsSection(title = "About") {
            Text(
                text = "SnapBadgers v1.0.4",
                color = Zinc400,
                fontSize = 14.sp
            )
            Text(
                text = "Multimodal music recommendation demo",
                color = Zinc500,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Zinc500) },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun SettingsOptionSelector(
    label: String,
    description: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(text = description, color = Zinc500, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionSelected(option) }
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

