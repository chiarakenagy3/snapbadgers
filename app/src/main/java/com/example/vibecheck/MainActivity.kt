package com.example.vibecheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---- Models ----

data class Song(
    val title: String,
    val artist: String,
    val similarity: Float,
    val inferenceTimeMs: Long
)

data class InferenceSteps(
    val textEncoded: Boolean = false,
    val fused: Boolean = false,
    val projected: Boolean = false,
    val ranked: Boolean = false
)

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val song: Song) : UiState()
    data class Error(val message: String) : UiState()
}

// ---- Activity ----

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    VibeCheckDemoScreen()
                }
            }
        }
    }
}

// ---- Screen ----

@Composable
fun VibeCheckDemoScreen() {
    var input by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var steps by remember { mutableStateOf(InferenceSteps()) }

    val scope = rememberCoroutineScope()

    // Derived UI flags.
    val isLoading = state is UiState.Loading
    val canSubmit = input.isNotBlank() && !isLoading

    // Keep padding & spacing consistent for a demo-friendly look.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Header()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Describe your vibe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        // If user edits text after an error, return to Idle to reduce "stuck" feeling.
                        if (state is UiState.Error) state = UiState.Idle
                    },
                    label = { Text("e.g., calm rainy night, focused studying…") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = false,
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                // Reset state for a clean run.
                                steps = InferenceSteps()
                                state = UiState.Loading

                                // End-to-end time measurement for demo performance display.
                                val startMs = System.currentTimeMillis()

                                // --- Demo stub pipeline (swap with real on-device modules later) ---
                                delay(140); steps = steps.copy(textEncoded = true)
                                delay(140); steps = steps.copy(fused = true)
                                delay(140); steps = steps.copy(projected = true)
                                delay(180); steps = steps.copy(ranked = true)
                                // ---------------------------------------------------------------

                                val endMs = System.currentTimeMillis()
                                val inferenceMs = endMs - startMs

                                // Fake top-1 recommendation.
                                val song = Song(
                                    title = "Blinding Lights",
                                    artist = "The Weeknd",
                                    similarity = 0.82f,
                                    inferenceTimeMs = inferenceMs
                                )

                                state = UiState.Success(song)
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isLoading) "Running..." else "Recommend")
                    }

                    OutlinedButton(
                        onClick = {
                            input = ""
                            steps = InferenceSteps()
                            state = UiState.Idle
                        },
                        enabled = !isLoading,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text("Reset")
                    }
                }

                // Small helper line for demo clarity.
                Text(
                    text = if (isLoading) "Processing locally on the device (stub)…" else "Tip: short phrases work best for demos.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Inference steps shown in a compact, neat card.
        InferenceStatusCard(steps = steps, isLoading = isLoading)

        // Main result area: cleanly separated.
        when (val s = state) {
            is UiState.Idle -> EmptyResultHint()
            is UiState.Loading -> LoadingResultHint()
            is UiState.Success -> RecommendationCard(song = s.song)
            is UiState.Error -> ErrorCard(message = s.message)
        }
    }
}

// ---- UI Components ----

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "VibeCheck",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
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

@Composable
private fun InferenceStatusCard(steps: InferenceSteps, isLoading: Boolean) {
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
private fun StepRow(label: String, done: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (done) "✓" else "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RecommendationCard(song: Song) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎵 Recommendation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Divider()

            KeyValueRow("Title", song.title)
            KeyValueRow("Artist", song.artist)
            KeyValueRow("Similarity", "%.2f".format(song.similarity))
            KeyValueRow("Inference time", "${song.inferenceTimeMs} ms")

            Text(
                text = "Demo note: This is a stubbed pipeline. Swap in your on-device models later.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyResultHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "No recommendation yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Enter a short description and press Recommend.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LoadingResultHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Working…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Running on-device inference steps (stub).",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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