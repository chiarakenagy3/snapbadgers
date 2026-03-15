package com.example.snapbadgers.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snapbadgers.model.RecommendationResult
import com.example.snapbadgers.model.Song

@Composable
fun RecommendationCard(
    result: RecommendationResult,
    encoderLabel: String,
    isModelBackedEncoder: Boolean
) {
    val topRecommendation = result.topRecommendation

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider()

            if (topRecommendation == null) {
                Text(
                    text = "No recommendations available.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Text encoder: $encoderLabel",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Top Match",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                KeyValueRow("Title", topRecommendation.title)
                KeyValueRow("Artist", topRecommendation.artist)
                KeyValueRow("Similarity", "%.3f".format(topRecommendation.similarity))
                KeyValueRow("Inference time", "${result.inferenceTimeMs} ms")
                KeyValueRow("Photo input", if (result.usedVisionInput) "Attached" else "Not attached")
                KeyValueRow(
                    "Text encoder",
                    if (isModelBackedEncoder) encoderLabel else "$encoderLabel (fallback)"
                )

                val additionalRecommendations = result.recommendations.drop(1)
                if (additionalRecommendations.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Other Suggestions",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    additionalRecommendations.forEachIndexed { index, song ->
                        RecommendationListRow(rank = index + 2, song = song)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationListRow(rank: Int, song: Song) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$rank. ${song.title}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
