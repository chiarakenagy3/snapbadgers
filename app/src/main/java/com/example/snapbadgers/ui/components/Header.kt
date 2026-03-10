package com.example.snapbadgers.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "SnapBadgers",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Companion.Bold
        )

        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // On-device badge: important for your capstone requirement.
            AssistChip(
                onClick = {},
                label = { Text("⚡ On-device AI") },
                enabled = false
            )

            Text(
                text = "Sprint 1 demo: Text → Top-1 song",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}