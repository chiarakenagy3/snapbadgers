package com.example.snapbadgers.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.snapbadgers.ui.i18n.AppI18n
import com.example.snapbadgers.ui.i18n.AppStrings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.launch

@Composable
fun SnapBadgersDemoScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val pipeline = remember(context) { RecommendationPipeline(context) }
    val scope = rememberCoroutineScope()
    val language by settingsRepository.language
    val strings = AppI18n.forLanguage(language)

    DisposableEffect(pipeline) {
        onDispose { pipeline.close() }
    }

    var activeTab by remember { mutableStateOf("analyze") }
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var steps by remember { mutableStateOf(InferenceSteps()) }
    var input by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val history = remember { mutableStateListOf<HistoryItem>() }
    val allSongs = remember(pipeline) { pipeline.getAllSongs() }

    LaunchedEffect(pipeline) {
        try {
            pipeline.warmUp()
        } catch (_: Exception) {
            // Warm-up failure is non-fatal; first real inference will handle initialization
        }
    }

    val hasResult by remember { derivedStateOf { state is UiState.Success } }
    val renderTab by remember { derivedStateOf { if (hasResult && (activeTab == "analyze" || activeTab == "player")) "player" else activeTab } }

    val onAnalyze: () -> Unit = {
        scope.launch {
            if (input.isBlank() && capturedBitmap == null) {
                state = UiState.Error(strings.pleaseProvideInput)
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
                history.add(0, HistoryItem(query = input.ifBlank { strings.visualSearch }, result = result))
                if (history.size > 50) history.removeRange(50, history.size)
                activeTab = "player"
            } catch (e: Exception) {
                state = UiState.Error(e.message ?: strings.genericError)
            }
        }
    }

    val onTabSelected: (String) -> Unit = { tab ->
        activeTab = tab
        if (tab != "analyze" && tab != "player") {
            state = UiState.Idle
        }
    }

    val content: @Composable () -> Unit = {
        Crossfade(targetState = renderTab, label = "content") { tab ->
            when (tab) {
                "analyze" -> SceneAnalyzer(
                    strings = strings,
                    input = input,
                    onInputChange = { input = it; if (state is UiState.Error) state = UiState.Idle },
                    capturedBitmap = capturedBitmap,
                    onBitmapCaptured = { capturedBitmap = it; if (state is UiState.Error) state = UiState.Idle },
                    isLoading = state is UiState.Loading,
                    errorMessage = (state as? UiState.Error)?.message,
                    onDismissError = { state = UiState.Idle },
                    steps = steps,
                    pipeline = pipeline,
                    onAnalyze = onAnalyze
                )

                "player" -> {
                    val successState = state as? UiState.Success
                    if (successState != null) {
                        RecommendationCard(
                            result = successState.result,
                            encoderLabel = pipeline.textEncoderLabel,
                            isModelBackedEncoder = pipeline.isModelBackedTextEncoder,
                            strings = strings,
                            onReset = {
                                state = UiState.Idle
                                activeTab = "analyze"
                            }
                        )
                    } else {
                        activeTab = "analyze"
                    }
                }

                "library" -> LibraryScreen(songs = allSongs, strings = strings)
                "activity" -> HistoryScreen(history = history, strings = strings)
                "settings" -> SettingsScreen(settingsRepository = settingsRepository)
                else -> SceneAnalyzer(
                    strings = strings,
                    input = input,
                    onInputChange = { input = it; if (state is UiState.Error) state = UiState.Idle },
                    capturedBitmap = capturedBitmap,
                    onBitmapCaptured = { capturedBitmap = it; if (state is UiState.Error) state = UiState.Idle },
                    isLoading = state is UiState.Loading,
                    errorMessage = (state as? UiState.Error)?.message,
                    onDismissError = { state = UiState.Idle },
                    steps = steps,
                    pipeline = pipeline,
                    onAnalyze = onAnalyze
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompact = maxWidth < 600.dp
        val navTab = if (activeTab == "player") "analyze" else activeTab

        if (isCompact) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) { content() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                BottomNav(activeTab = navTab, strings = strings, onTabSelected = onTabSelected)
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(activeTab = navTab, strings = strings, onTabSelected = onTabSelected)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                ) { content() }
            }
        }
    }
}

@Composable
private fun BottomNav(activeTab: String, strings: AppStrings, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(Icons.Default.Search, "analyze", "Analyze", strings.sceneAnalyzer, activeTab == "analyze", onTabSelected)
        BottomNavItem(Icons.Default.LibraryMusic, "library", "Library", strings.musicLibrary, activeTab == "library", onTabSelected)
        BottomNavItem(Icons.Default.History, "activity", "History", strings.activityHistory, activeTab == "activity", onTabSelected)
        BottomNavItem(Icons.Default.Settings, "settings", "Settings", strings.settings, activeTab == "settings", onTabSelected)
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    id: String,
    shortLabel: String,
    accessibilityLabel: String,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(id) }
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = accessibilityLabel,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = shortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun Sidebar(activeTab: String, strings: AppStrings, onTabSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        SidebarItem(Icons.Default.Search, "analyze", strings.sceneAnalyzer, activeTab == "analyze", onTabSelected)
        SidebarItem(Icons.Default.LibraryMusic, "library", strings.musicLibrary, activeTab == "library", onTabSelected)
        SidebarItem(Icons.Default.History, "activity", strings.activityHistory, activeTab == "activity", onTabSelected)

        Spacer(modifier = Modifier.weight(1f))

        SidebarItem(Icons.Default.Settings, "settings", strings.settings, activeTab == "settings", onTabSelected)
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    id: String,
    accessibilityLabel: String,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    IconButton(
        onClick = { onClick(id) },
        modifier = Modifier
            .size(48.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            icon,
            contentDescription = accessibilityLabel,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SceneAnalyzer(
    strings: AppStrings,
    input: String,
    onInputChange: (String) -> Unit,
    capturedBitmap: Bitmap?,
    onBitmapCaptured: (Bitmap?) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    steps: InferenceSteps,
    pipeline: RecommendationPipeline,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = strings.sceneAnalyzer,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = strings.describeYourVibe,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.inputPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    enabled = !isLoading,
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Button(
                    onClick = onAnalyze,
                    enabled = !isLoading && (input.isNotBlank() || capturedBitmap != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = if (isLoading) strings.analyzing else strings.analyzeScene,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismissError,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(
                            text = strings.dismiss,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        CameraInputCard(
            capturedBitmap = capturedBitmap,
            enabled = !isLoading,
            strings = strings,
            onBitmapCaptured = onBitmapCaptured
        )

        InferenceStatusCard(
            steps = steps,
            isLoading = isLoading,
            encoderLabel = pipeline.textEncoderLabel,
            isModelBackedEncoder = pipeline.isModelBackedTextEncoder,
            hasVisionInput = capturedBitmap != null,
            strings = strings
        )
    }
}

