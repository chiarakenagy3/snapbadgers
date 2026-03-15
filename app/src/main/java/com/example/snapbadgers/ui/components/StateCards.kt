package com.example.snapbadgers.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
public fun EmptyResultHint() {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.Companion.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "No recommendation yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Companion.SemiBold
            )
            Text(
                text = "Enter a short description and press Recommend.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
public fun LoadingResultHint() {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.Companion.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Working…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Companion.SemiBold
            )
            LinearProgressIndicator(modifier = Modifier.Companion.fillMaxWidth())
            Text(
                text = "Running on-device inference steps (stub).",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
public fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.Companion.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Companion.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Tip: Make sure the input is not empty. Later, network/model errors can be shown here too.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}