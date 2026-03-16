package com.example.snapbadgers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapbadgers.model.RecommendationResult
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.ui.theme.Zinc400
import com.example.snapbadgers.ui.theme.Zinc800
import com.example.snapbadgers.ui.theme.Zinc900
import com.example.snapbadgers.ui.theme.Zinc950

@Composable
fun RecommendationCard(
    result: RecommendationResult,
    encoderLabel: String,
    isModelBackedEncoder: Boolean,
    onReset: () -> Unit
) {
    val topRecommendation = result.topRecommendation
    val additionalRecommendations = result.recommendations.drop(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Music Player",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (topRecommendation == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Zinc900)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("No recommendations available.", color = Color.White)
                    Text("Text encoder: $encoderLabel", color = Zinc400)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Zinc900)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Zinc800, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = topRecommendation.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = topRecommendation.artist,
                                color = Zinc400,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    KeyValueRow("Similarity", "%.3f".format(topRecommendation.similarity))
                    KeyValueRow("Inference time", "${result.inferenceTimeMs} ms")
                    KeyValueRow("Photo input", if (result.usedVisionInput) "Attached" else "Not attached")
                    KeyValueRow(
                        "Text encoder",
                        if (isModelBackedEncoder) encoderLabel else "$encoderLabel (fallback)"
                    )
                }
            }

            if (additionalRecommendations.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Zinc900)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        additionalRecommendations.forEachIndexed { index, song ->
                            RecommendationListRow(rank = index + 2, song = song)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text("Analyze Another Scene", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecommendationListRow(rank: Int, song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Zinc800, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$rank. ${song.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = Zinc400
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
            fontWeight = FontWeight.Medium,
            color = Zinc400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}
