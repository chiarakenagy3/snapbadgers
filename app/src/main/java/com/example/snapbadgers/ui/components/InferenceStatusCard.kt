package com.example.snapbadgers.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snapbadgers.model.InferenceSteps

@Composable
fun InferenceStatusCard(steps: InferenceSteps, isLoading: Boolean) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.Companion.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = "Inference Pipeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Companion.SemiBold
                )

                // Status pill text (simple, readable for demos).
                Text(
                    text = if (isLoading) "RUNNING" else "READY",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            StepRow("Text encoding", steps.textEncoded)
            StepRow("Fusion", steps.fused)
            StepRow("Projection", steps.projected)
            StepRow("Similarity search", steps.ranked)
        }
    }
}

@Composable
fun StepRow(label: String, done: Boolean) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (done) "✓" else "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Companion.SemiBold
        )
    }
}