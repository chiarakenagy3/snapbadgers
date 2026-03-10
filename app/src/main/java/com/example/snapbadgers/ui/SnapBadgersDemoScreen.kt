package com.example.snapbadgers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.UiState
import com.example.snapbadgers.ui.components.EmptyResultHint
import com.example.snapbadgers.ui.components.ErrorCard
import com.example.snapbadgers.ui.components.Header
import com.example.snapbadgers.ui.components.InferenceStatusCard
import com.example.snapbadgers.ui.components.LoadingResultHint
import com.example.snapbadgers.ui.components.RecommendationCard
import kotlinx.coroutines.launch

@Composable
fun SnapBadgersDemoScreen() {
    val context = LocalContext.current
    val pipeline = remember(context) { RecommendationPipeline(context) }

    var input by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var steps by remember { mutableStateOf(InferenceSteps()) }

    val scope = rememberCoroutineScope()

    val isLoading = state is UiState.Loading
    val canSubmit = input.isNotBlank() && !isLoading

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
                                if (input.isBlank()) {
                                    state = UiState.Error("Please enter a vibe description.")
                                    return@launch
                                }

                                steps = InferenceSteps()
                                state = UiState.Loading

                                try {
                                    val song = pipeline.runPipeline(
                                        input = input,
                                        onStepUpdate = { updatedSteps ->
                                            steps = updatedSteps
                                        }
                                    )
                                    state = UiState.Success(song)
                                } catch (e: Exception) {
                                    state = UiState.Error(e.message ?: "Unknown error")
                                }
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

                Text(
                    text = if (isLoading) {
                        "Processing locally on the device..."
                    } else {
                        "Tip: short phrases work best for demos."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        InferenceStatusCard(steps = steps, isLoading = isLoading)

        when (val s = state) {
            is UiState.Idle -> EmptyResultHint()
            is UiState.Loading -> LoadingResultHint()
            is UiState.Success -> RecommendationCard(song = s.song)
            is UiState.Error -> ErrorCard(message = s.message)
        }
    }
}