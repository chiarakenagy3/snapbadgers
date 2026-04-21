package com.example.snapbadgers.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.ui.i18n.AppStrings

@Composable
fun InferenceStatusCard(
    steps: InferenceSteps,
    isLoading: Boolean,
    encoderLabel: String,
    isModelBackedEncoder: Boolean,
    hasVisionInput: Boolean,
    strings: AppStrings
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.inferencePipeline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                Text(
                    text = if (isLoading) strings.running else strings.ready,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            Text(
                text = "${strings.textEncoder}: $encoderLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isModelBackedEncoder) strings.modeModelBacked else strings.modeFallbackStub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            StepRow(strings.stepTextEncoding, stepState(steps.textEncoded), strings)
            StepRow(strings.stepSensorEncoding, stepState(steps.sensorEncoded), strings)
            StepRow(
                strings.stepVisionEncoding,
                when {
                    !hasVisionInput -> StepState.Skipped
                    steps.visionEncoded -> StepState.Done
                    else -> StepState.Pending
                },
                strings
            )
            StepRow(strings.stepFusion, stepState(steps.fused), strings)
            StepRow(strings.stepProjection, stepState(steps.projected), strings)
            StepRow(strings.stepSimilarity, stepState(steps.ranked), strings)
        }
    }
}

private enum class StepState { Done, Pending, Skipped }

private fun stepState(done: Boolean): StepState =
    if (done) StepState.Done else StepState.Pending

@Composable
private fun StepRow(label: String, state: StepState, strings: AppStrings) {
    val statusLabel = when (state) {
        StepState.Done -> strings.statusDone
        StepState.Pending -> strings.statusPending
        StepState.Skipped -> strings.statusSkipped
    }
    val statusColor = when (state) {
        StepState.Done -> MaterialTheme.colorScheme.primary
        StepState.Pending, StepState.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
            maxLines = 1
        )
    }
}
