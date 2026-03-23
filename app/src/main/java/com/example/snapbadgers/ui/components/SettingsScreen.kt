package com.example.snapbadgers.ui.components

import androidx.compose.foundation.background
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
import com.example.snapbadgers.ui.theme.Zinc400
import com.example.snapbadgers.ui.theme.Zinc500
import com.example.snapbadgers.ui.theme.Zinc800
import com.example.snapbadgers.ui.theme.Zinc900
import androidx.compose.foundation.layout.ColumnScope

@Composable
fun SettingsScreen(settingsRepository: SettingsRepository) {
    val spotifyKey = settingsRepository.spotifyKey.value
    val aiHubKey = settingsRepository.aiHubKey.value
    val useHighPrecision = settingsRepository.useHighPrecision.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsSection(title = "API Configuration") {
            SettingsInputField(
                label = "Spotify API Client ID",
                value = spotifyKey,
                onValueChange = { settingsRepository.updateSpotifyKey(it) },
                placeholder = "Enter your Spotify Client ID"
            )
            SettingsInputField(
                label = "Qualcomm AI Hub Key",
                value = aiHubKey,
                onValueChange = { settingsRepository.updateAiHubKey(it) },
                placeholder = "Enter AI Hub API Token"
            )
        }

        SettingsSection(title = "Inference Engine") {
            SettingsToggle(
                label = "High Precision Mode",
                description = "Use FP32 instead of INT8 for text encoders",
                checked = useHighPrecision,
                onCheckedChange = { settingsRepository.updateUseHighPrecision(it) }
            )
        }

        SettingsSection(title = "About") {
            Text(
                text = "SnapBadgers v1.0.4",
                color = Zinc400,
                fontSize = 14.sp
            )
            Text(
                text = "Powered by Qualcomm AI Hub & Snapdragon 8 Gen 3",
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
            color = Zinc400,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Zinc900),
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
        Text(text = label, color = Color.White, fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Zinc500) },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Zinc800,
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black
            )
        )
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
            Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = description, color = Zinc500, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Zinc800,
                uncheckedThumbColor = Zinc500,
                uncheckedTrackColor = Color.Black
            )
        )
    }
}

