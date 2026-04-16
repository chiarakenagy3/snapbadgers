package com.example.snapbadgers.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapbadgers.ai.pipeline.RecommendationPipeline
import com.example.snapbadgers.data.SettingsRepository
import com.example.snapbadgers.model.HistoryItem
import com.example.snapbadgers.model.InferenceSteps
import com.example.snapbadgers.model.Song
import com.example.snapbadgers.model.UiState
import com.example.snapbadgers.ui.components.HistoryScreen
import com.example.snapbadgers.ui.components.InferenceStatusCard
import com.example.snapbadgers.ui.components.LibraryScreen
import com.example.snapbadgers.ui.components.RecommendationCard
import com.example.snapbadgers.ui.components.SettingsScreen
import com.example.snapbadgers.ui.i18n.AppI18n
import com.example.snapbadgers.ui.i18n.AppStrings
import com.example.snapbadgers.ui.theme.Zinc500
import com.example.snapbadgers.ui.theme.Zinc800
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SnapBadgersDemoScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val pipeline = remember(context) { RecommendationPipeline(context) }
    val isCameraAvailable = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    val scope = rememberCoroutineScope()
    val language by settingsRepository.language
    val strings = AppI18n.forLanguage(language)

    var activeTab by remember { mutableStateOf("analyze") }
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var steps by remember { mutableStateOf(InferenceSteps()) }
    var input by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val history = remember { mutableStateListOf<HistoryItem>() }
    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLibraryLoading by remember { mutableStateOf(false) }

    fun loadSongsIfNeeded() {
        if (allSongs.isNotEmpty() || isLibraryLoading) return
        scope.launch {
            isLibraryLoading = true
            allSongs = withContext(Dispatchers.Default) {
                runCatching { pipeline.getAllSongs() }
                    .onFailure { throwable ->
                        Log.e("SnapBadgersDemoScreen", "Failed to load song catalog", throwable)
                    }
                    .getOrDefault(emptyList())
            }
            isLibraryLoading = false
        }
    }

    val hasResult = state is UiState.Success
    val renderTab = if (hasResult && (activeTab == "analyze" || activeTab == "player")) "player" else activeTab

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Sidebar(
            activeTab = if (activeTab == "player") "analyze" else activeTab,
            onTabSelected = { tab ->
                activeTab = tab
                if (tab == "library") loadSongsIfNeeded()
                if (tab != "analyze" && tab != "player") {
                    state = UiState.Idle
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Crossfade(targetState = renderTab, label = "content") { tab ->
                when (tab) {
                    "analyze" -> SceneAnalyzer(
                        strings = strings,
                        input = input,
                        onInputChange = { input = it },
                        capturedBitmap = capturedBitmap,
                        onBitmapCaptured = { capturedBitmap = it },
                        showCameraInput = isCameraAvailable,
                        songs = allSongs,
                        isLibraryLoading = isLibraryLoading,
                        isLoading = state is UiState.Loading,
                        steps = steps,
                        pipeline = pipeline,
                        onOpenLibrary = {
                            activeTab = "library"
                            loadSongsIfNeeded()
                        },
                        onAnalyze = {
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

                    "library" -> LibraryScreen(songs = allSongs, language = language)
                    "activity" -> HistoryScreen(history = history, language = language)
                    "settings" -> SettingsScreen(settingsRepository = settingsRepository)
                    else -> Unit
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
            tint = if (isSelected) MaterialTheme.colorScheme.onBackground else Zinc500,
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
    showCameraInput: Boolean,
    songs: List<Song>,
    isLibraryLoading: Boolean,
    isLoading: Boolean,
    steps: InferenceSteps,
    pipeline: RecommendationPipeline,
    onOpenLibrary: () -> Unit,
    onAnalyze: () -> Unit
) {
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        onBitmapCaptured(bitmap)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = strings.sceneAnalyzer,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.describeYourVibe,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )

                    if (showCameraInput) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (capturedBitmap != null) {
                                TextButton(
                                    onClick = { onBitmapCaptured(null) },
                                    enabled = !isLoading
                                ) {
                                    Text("Clear")
                                }
                            }
                            FilledTonalIconButton(
                                onClick = { cameraLauncher.launch(null) },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = if (capturedBitmap == null) "Capture photo" else "Retake photo"
                                )
                            }
                        }
                    }
                }

                if (showCameraInput) {
                    Text(
                        text = if (capturedBitmap == null) {
                            "Photo is optional. You can directly analyze text."
                        } else {
                            "Photo attached. It will be used together with text."
                        },
                        color = Zinc500,
                        fontSize = 12.sp
                    )
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.inputPlaceholder, color = Zinc500) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Zinc800,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Captured camera photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Zinc800),
                        contentScale = ContentScale.Crop
                    )
                }

                Button(
                    onClick = onAnalyze,
                    enabled = !isLoading && (input.isNotBlank() || capturedBitmap != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isLoading) strings.analyzing else strings.analyzeScene, fontWeight = FontWeight.Bold)
                }
            }
        }

        InferenceStatusCard(
            steps = steps,
            isLoading = isLoading,
            encoderLabel = pipeline.textEncoderLabel,
            isModelBackedEncoder = pipeline.isModelBackedTextEncoder,
            hasVisionInput = capturedBitmap != null
        )

        CurrentLibraryCard(
            songs = songs,
            isLoading = isLibraryLoading,
            onOpenLibrary = onOpenLibrary
        )
    }
}

@Composable
private fun CurrentLibraryCard(
    songs: List<Song>,
    isLoading: Boolean,
    onOpenLibrary: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Library",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                TextButton(onClick = onOpenLibrary) {
                    Text("View All")
                }
            }

            Text(
                text = if (isLoading) "Loading current catalog..." else "${songs.size} tracks loaded",
                color = Zinc500,
                fontSize = 12.sp
            )

            if (isLoading) {
                Text(
                    text = "Please wait while songs are loaded.",
                    color = Zinc500,
                    fontSize = 13.sp
                )
            } else {
                val previewSongs = songs.take(5)
                if (previewSongs.isEmpty()) {
                    Text(
                        text = "No songs in the current catalog.",
                        color = Zinc500,
                        fontSize = 13.sp
                    )
                } else {
                    previewSongs.forEach { song ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Zinc500,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${song.title} - ${song.artist}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
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
