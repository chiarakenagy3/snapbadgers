package com.example.snapbadgers.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import com.example.snapbadgers.data.SettingsRepository
import com.example.snapbadgers.model.HistoryItem
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.UiState
import com.example.snapbadgers.ui.components.CameraInputCard
import com.example.snapbadgers.ui.components.HistoryScreen
import com.example.snapbadgers.ui.components.InferenceStatusCard
import com.example.snapbadgers.ui.components.LibraryScreen
import com.example.snapbadgers.ui.components.RecommendationCard
import com.example.snapbadgers.ui.components.SettingsScreen
import com.example.snapbadgers.ui.theme.Zinc500
import com.example.snapbadgers.ui.theme.Zinc800
import com.example.snapbadgers.ui.theme.Zinc900
import com.example.snapbadgers.ui.theme.Zinc950
import kotlinx.coroutines.launch

@Composable
fun SnapBadgersDemoScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val pipeline = remember(context) { RecommendationPipeline(context) }
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("analyze") }
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var steps by remember { mutableStateOf(InferenceSteps()) }
    var input by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val history = remember { mutableStateListOf<HistoryItem>() }
    val allSongs = remember(pipeline) { pipeline.getAllSongs() }

    LaunchedEffect(pipeline) {
        pipeline.warmUp()
    }

    val hasResult = state is UiState.Success
    val renderTab = if (hasResult && (activeTab == "analyze" || activeTab == "player")) "player" else activeTab

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Sidebar(
            activeTab = if (activeTab == "player") "analyze" else activeTab,
            onTabSelected = { tab ->
                activeTab = tab
                if (tab != "analyze" && tab != "player") {
                    state = UiState.Idle
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Zinc950)
        ) {
            Crossfade(targetState = renderTab, label = "content") { tab ->
                when (tab) {
                    "analyze" -> SceneAnalyzer(
                        input = input,
                        onInputChange = { input = it },
                        capturedBitmap = capturedBitmap,
                        onBitmapCaptured = { capturedBitmap = it },
                        isLoading = state is UiState.Loading,
                        steps = steps,
                        pipeline = pipeline,
                        onAnalyze = {
                            scope.launch {
                                if (input.isBlank() && capturedBitmap == null) {
                                    state = UiState.Error("Please provide input")
                                    return@launch
                                }
                                steps = InferenceSteps()
                                state = UiState.Loading
                                try {
                                    val result = pipeline.runPipeline(
                                        input = input,
                                        imageBitmap = capturedBitmap,
                                        onStepUpdate = { steps = it }
                                    )
                                    state = UiState.Success(result)
                                    history.add(0, HistoryItem(query = input.ifBlank { "Visual Search" }, result = result))
                                    activeTab = "player"
                                } catch (e: Exception) {
                                    state = UiState.Error(e.message ?: "Error")
                                }
                            }
                        }
                    )

                    "player" -> {
                        val successState = state as? UiState.Success
                        if (successState != null) {
                            RecommendationCard(
                                result = successState.result,
                                encoderLabel = pipeline.textEncoderLabel,
                                isModelBackedEncoder = pipeline.isModelBackedTextEncoder,
                                onReset = {
                                    state = UiState.Idle
                                    activeTab = "analyze"
                                }
                            )
                        } else {
                            activeTab = "analyze"
                        }
                    }

                    "library" -> LibraryScreen(songs = allSongs)
                    "activity" -> HistoryScreen(history = history)
                    "settings" -> SettingsScreen(settingsRepository = settingsRepository)
                    else -> SceneAnalyzer(
                        input = input,
                        onInputChange = { input = it },
                        capturedBitmap = capturedBitmap,
                        onBitmapCaptured = { capturedBitmap = it },
                        isLoading = state is UiState.Loading,
                        steps = steps,
                        pipeline = pipeline,
                        onAnalyze = {
                            scope.launch {
                                if (input.isBlank() && capturedBitmap == null) {
                                    state = UiState.Error("Please provide input")
                                    return@launch
                                }
                                steps = InferenceSteps()
                                state = UiState.Loading
                                try {
                                    val result = pipeline.runPipeline(
                                        input = input,
                                        imageBitmap = capturedBitmap,
                                        onStepUpdate = { steps = it }
                                    )
                                    state = UiState.Success(result)
                                    history.add(0, HistoryItem(query = input.ifBlank { "Visual Search" }, result = result))
                                    activeTab = "player"
                                } catch (e: Exception) {
                                    state = UiState.Error(e.message ?: "Error")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(activeTab: String, onTabSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(Color.Black)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        SidebarItem(Icons.Default.Search, "analyze", activeTab == "analyze", onTabSelected)
        SidebarItem(Icons.Default.LibraryMusic, "library", activeTab == "library", onTabSelected)
        SidebarItem(Icons.Default.History, "activity", activeTab == "activity", onTabSelected)

        Spacer(modifier = Modifier.weight(1f))

        SidebarItem(Icons.Default.Settings, "settings", activeTab == "settings", onTabSelected)
    }
}

@Composable
private fun SidebarItem(icon: ImageVector, id: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (isSelected) Zinc800 else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick(id) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = id,
            tint = if (isSelected) Color.White else Zinc500,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SceneAnalyzer(
    input: String,
    onInputChange: (String) -> Unit,
    capturedBitmap: Bitmap?,
    onBitmapCaptured: (Bitmap?) -> Unit,
    isLoading: Boolean,
    steps: InferenceSteps,
    pipeline: RecommendationPipeline,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Scene Analyzer",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Zinc900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Describe your vibe",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. A rainy afternoon in a cozy cafe", color = Zinc500) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp),
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

                Button(
                    onClick = onAnalyze,
                    enabled = !isLoading && (input.isNotBlank() || capturedBitmap != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isLoading) "Analyzing..." else "Analyze Scene", fontWeight = FontWeight.Bold)
                }
            }
        }

        CameraInputCard(
            capturedBitmap = capturedBitmap,
            enabled = !isLoading,
            onBitmapCaptured = onBitmapCaptured
        )

        InferenceStatusCard(
            steps = steps,
            isLoading = isLoading,
            encoderLabel = pipeline.textEncoderLabel,
            isModelBackedEncoder = pipeline.isModelBackedTextEncoder,
            hasVisionInput = capturedBitmap != null
        )
    }
}

@Composable
private fun PlaceholderContent(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Zinc500,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
