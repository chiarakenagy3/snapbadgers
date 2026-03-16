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
fun InferenceStatusCard(
    steps: InferenceSteps,
    isLoading: Boolean,
    encoderLabel: String,
    isModelBackedEncoder: Boolean,
    hasVisionInput: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inference Pipeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isLoading) "RUNNING" else "READY",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Text(
                text = "Text encoder: $encoderLabel",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (isModelBackedEncoder) {
                    "Mode: model-backed"
                } else {
                    "Mode: fallback stub"
                },
                style = MaterialTheme.typography.bodySmall
            )

            StepRow("Text encoding", status = if (steps.textEncoded) "Done" else "Pending")
            StepRow("Sensor encoding", status = if (steps.sensorEncoded) "Done" else "Pending")
            StepRow(
                "Vision encoding",
                status = when {
                    !hasVisionInput -> "Skipped"
                    steps.visionEncoded -> "Done"
                    else -> "Pending"
                }
            )
            StepRow("Fusion", status = if (steps.fused) "Done" else "Pending")
            StepRow("Projection", status = if (steps.projected) "Done" else "Pending")
            StepRow("Similarity search", status = if (steps.ranked) "Done" else "Pending")
        }
    }
}

@Composable
fun StepRow(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
