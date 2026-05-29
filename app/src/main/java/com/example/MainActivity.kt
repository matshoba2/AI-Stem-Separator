package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.database.SongProject
import com.example.data.repository.SongRepository
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate Room Database manually without complex DI frameworks
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SongRepository(database.songDao())
        
        setContent {
            MyApplicationTheme {
                val viewModel: StemViewModel = viewModel(
                    factory = StemViewModelFactory(repository)
                )

                // Seed sample songs on cold-start
                LaunchedEffect(Unit) {
                    viewModel.seedSampleSongs()
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = BackgroundDark
                ) { innerPadding ->
                    MainStudioContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// --- Responsive Layout Core ---
@Composable
fun MainStudioContent(
    viewModel: StemViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val savedSongs by viewModel.savedSongsList.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    // Screen Dimensions for Adaptive Layout selection
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    // Auto-select first song if none is loaded yet
    LaunchedEffect(savedSongs) {
        if (currentSong == null && savedSongs.isNotEmpty()) {
            viewModel.selectSong(context, savedSongs.first())
        }
    }

    if (isWideScreen) {
        // --- Tablet / Wide Landscape Mode: List-Detail Canonical Layout ---
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: The Interactive Stem Mixer & Player Control (55% width)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StemMixerPanel(viewModel = viewModel, modifier = Modifier.weight(1f))
                TransportControlsCard(viewModel = viewModel)
            }

            // Right Column: AI Analysis Pane and Library Browser (45% width)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AcousticAiCard(viewModel = viewModel, modifier = Modifier.weight(1f))
                TrackLibraryCard(viewModel = viewModel, modifier = Modifier.height(260.dp))
            }
        }
    } else {
        // --- Mobile Portrait Mode: Optimized Scrolling Unified Console ---
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Brand Logo
            StudioHeader()

            // Stem Mixer Area
            StemMixerPanel(viewModel = viewModel, modifier = Modifier.height(440.dp))

            // Transport Control
            TransportControlsCard(viewModel = viewModel)

            // Acoustic AI Advisor Card
            AcousticAiCard(viewModel = viewModel, modifier = Modifier.heightIn(min = 300.dp, max = 500.dp))

            // Library Drawer
            TrackLibraryCard(viewModel = viewModel, modifier = Modifier.height(300.dp))
        }
    }
}

@Composable
fun StudioHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ACOUSTIC AI",
                color = GlowAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "High-Fidelity Stem Sep",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
        
        // Active Status Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .background(SurfaceDark, CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Green, CircleShape)
            )
            Text(
                text = "DSP DIRECT",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- 1. Interactive Stem Mixer Card ---
@Composable
fun StemMixerPanel(
    viewModel: StemViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    // Level State Monitors
    val vocalsVal by viewModel.vocalsLevel.collectAsStateWithLifecycle()
    val drumsVal by viewModel.drumsLevel.collectAsStateWithLifecycle()
    val bassVal by viewModel.bassLevel.collectAsStateWithLifecycle()
    val melodyVal by viewModel.melodyLevel.collectAsStateWithLifecycle()

    // Mute/Solo Monitors
    val mutedStems by viewModel.mutedStems.collectAsStateWithLifecycle()
    val soloStem by viewModel.soloStem.collectAsStateWithLifecycle()

    // Animated Heights
    val vocalHeights by viewModel.vocalWaveHeight.collectAsStateWithLifecycle()
    val drumHeights by viewModel.drumWaveHeight.collectAsStateWithLifecycle()
    val bassHeights by viewModel.bassWaveHeight.collectAsStateWithLifecycle()
    val melodyHeights by viewModel.melodyWaveHeight.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OutlineGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Heading Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentSong?.title ?: "No Song Loaded",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Artist: ${currentSong?.artist ?: "N/A"}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // BPM / Key Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, OutlineGrey, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "${currentSong?.bpm ?: 120} BPM",
                            color = GlowAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, OutlineGrey, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = currentSong?.keySignature ?: "C Maj",
                            color = StemBass,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = OutlineGrey)
            Spacer(modifier = Modifier.height(16.dp))

            // The Four Stem Mixing Channels side-by-side
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Channel 1: Vocals
                MixerChannelColumn(
                    title = "vocals",
                    value = vocalsVal,
                    onValueChange = { viewModel.setVocalsLevel(it) },
                    waveHeights = vocalHeights,
                    stemColor = StemVocal,
                    isMuted = mutedStems.contains("Vocals"),
                    isSoloed = soloStem == "Vocals",
                    onMuteClick = { viewModel.toggleMute("Vocals") },
                    onSoloClick = { viewModel.toggleSolo("Vocals") },
                    modifier = Modifier.weight(1f)
                )

                // Channel 2: Drums
                MixerChannelColumn(
                    title = "drums",
                    value = drumsVal,
                    onValueChange = { viewModel.setDrumsLevel(it) },
                    waveHeights = drumHeights,
                    stemColor = StemDrum,
                    isMuted = mutedStems.contains("Drums"),
                    isSoloed = soloStem == "Drums",
                    onMuteClick = { viewModel.toggleMute("Drums") },
                    onSoloClick = { viewModel.toggleSolo("Drums") },
                    modifier = Modifier.weight(1f)
                )

                // Channel 3: Bass
                MixerChannelColumn(
                    title = "bass",
                    value = bassVal,
                    onValueChange = { viewModel.setBassLevel(it) },
                    waveHeights = bassHeights,
                    stemColor = StemBass,
                    isMuted = mutedStems.contains("Bass"),
                    isSoloed = soloStem == "Bass",
                    onMuteClick = { viewModel.toggleMute("Bass") },
                    onSoloClick = { viewModel.toggleSolo("Bass") },
                    modifier = Modifier.weight(1f)
                )

                // Channel 4: Melody
                MixerChannelColumn(
                    title = "melody",
                    value = melodyVal,
                    onValueChange = { viewModel.setMelodyLevel(it) },
                    waveHeights = melodyHeights,
                    stemColor = StemMelody,
                    isMuted = mutedStems.contains("Melody"),
                    isSoloed = soloStem == "Melody",
                    onMuteClick = { viewModel.toggleMute("Melody") },
                    onSoloClick = { viewModel.toggleSolo("Melody") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MixerChannelColumn(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    waveHeights: List<Float>,
    stemColor: Color,
    isMuted: Boolean,
    isSoloed: Boolean,
    onMuteClick: () -> Unit,
    onSoloClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim by animateFloatAsState(
        targetValue = if (isMuted) 0.35f else 1f,
        label = "alphaMuteAnim"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.5f))
            .border(1.dp, if (isSoloed) stemColor.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Label Head
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = if (isMuted) TextSecondary else stemColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = if (isMuted) TextSecondary else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Animated VU Waveform Canvas
        StemWaveformCanvas(
            heights = waveHeights,
            color = if (isMuted) Color.Gray else stemColor,
            modifier = Modifier
                .height(55.dp)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        )

        // Custom High-Quality Fader Slider
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background line of fader
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(0.85f)
                    .clip(CircleShape)
                    .background(OutlineGrey)
            )

            // Slider Component Custom Styled
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .testTag("slider_${title}")
                    .graphicsLayer {
                        rotationZ = -90f // Rotate standard slider vertically!
                    },
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = if (isMuted) Color.Gray else stemColor
                ),
                valueRange = 0f..1f,
                steps = 0
            )
        }

        // Action Buttons: Mute (M) & Solo (S)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Mute Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isMuted) Color(0xFFE63946).copy(alpha = 0.3f) else SurfaceElevated)
                    .border(
                        1.dp,
                        if (isMuted) Color(0xFFE63946) else OutlineGrey,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onMuteClick() }
                    .testTag("mute_${title}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = if (isMuted) Color(0xFFE63946) else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Solo Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSoloed) stemColor.copy(alpha = 0.3f) else SurfaceElevated)
                    .border(
                        1.dp,
                        if (isSoloed) stemColor else OutlineGrey,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSoloClick() }
                    .testTag("solo_${title}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = if (isSoloed) stemColor else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// --- High-Performance Custom Waveform Drawing Engine ---
@Composable
fun StemWaveformCanvas(
    heights: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val numPoints = heights.size
        if (numPoints == 0) return@Canvas

        val step = width / (numPoints - 1).coerceAtLeast(1)
        val path = Path()

        // Symmetrical mirror representation
        path.moveTo(0f, centerY)
        for (i in 0 until numPoints) {
            val x = i * step
            val waveVal = heights[i].dp.toPx()
            path.lineTo(x, centerY - waveVal)
        }
        for (i in numPoints - 1 downTo 0) {
            val x = i * step
            val waveVal = heights[i].dp.toPx()
            path.lineTo(x, centerY + waveVal)
        }
        path.close()

        drawPath(
            path = path,
            color = color.copy(alpha = 0.5f),
            style = Fill
        )

        // Wave edge highlights
        val strokePath = Path()
        strokePath.moveTo(0f, centerY)
        for (i in 0 until numPoints) {
            val x = i * step
            val waveVal = heights[i].dp.toPx()
            strokePath.lineTo(x, centerY - waveVal)
        }
        drawPath(
            path = strokePath,
            color = color,
            style = Stroke(width = 1.dp.toPx())
        )

        // Center zero-crossing timeline line
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// --- 2. Master Tracking & Transport Deck ---
@Composable
fun TransportControlsCard(
    viewModel: StemViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val currentTime by viewModel.playbackTimeText.collectAsStateWithLifecycle()
    val durationTime by viewModel.playbackDurationText.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OutlineGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Live Playback Timeline Progress slider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTime,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                // Track Progress Slider
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = GlowAccent,
                    trackColor = OutlineGrey,
                )

                Text(
                    text = durationTime,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transport Actions (Reset, Play, Stop, Auto Metronome)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) GlowAccent else Color.Gray)
                    )
                    Text(
                        text = if (isPlaying) "PLAYING" else "PAUSED",
                        color = if (isPlaying) GlowAccent else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Core Transport Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Levels Button
                    IconButton(
                        onClick = {
                            viewModel.setVocalsLevel(0.8f)
                            viewModel.setDrumsLevel(0.8f)
                            viewModel.setBassLevel(0.8f)
                            viewModel.setMelodyLevel(0.8f)
                        },
                        modifier = Modifier.background(SurfaceElevated, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Mix Levels",
                            tint = TextPrimary
                        )
                    }

                    // Main Play/Pause Button
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        containerColor = GlowAccent,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp).testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause Song",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Stop Button
                    IconButton(
                        onClick = { viewModel.stopPlayback() },
                        modifier = Modifier.background(SurfaceElevated, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Playback",
                            tint = TextPrimary
                        )
                    }
                }

                // Info Section: Source Label
                Text(
                    text = if (currentSong?.filePath?.startsWith("internal://") == true) "DEMO TRACK" else "DEVICE MP3",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .border(1.dp, OutlineGrey, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// --- 3. Acoustic AI Analytics Pane (Powered by Gemini) ---
@Composable
fun AcousticAiCard(
    viewModel: StemViewModel,
    modifier: Modifier = Modifier
) {
    val geminiState by viewModel.geminiState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OutlineGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header with AI Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Analysis Icon",
                        tint = GlowAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ACOUSTIC AI ADVISOR",
                        color = GlowAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                
                // Trigger Button
                if (currentSong != null && geminiState !is GeminiState.Loading) {
                    Button(
                        onClick = { viewModel.triggerGeminiAnalysis() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OutlineGrey),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("analyze_acoustics_button")
                    ) {
                        Text(
                            text = if (geminiState is GeminiState.Success) "RE-ANALYZE" else "ANALYZE PROFILE",
                            color = GlowAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = OutlineGrey)
            Spacer(modifier = Modifier.height(12.dp))

            // Body Area with status switcher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark.copy(alpha = 0.6f))
                    .padding(10.dp),
                contentAlignment = Alignment.TopStart
            ) {
                when (val state = geminiState) {
                    is GeminiState.Idle -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Acoustics Sandbox Is Empty",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Click 'ANALYZE PROFILE' above. Gemini will parse structural frequencies, recommend key crossovers, and provide a flawless separation report.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    is GeminiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = GlowAccent, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Interactive loader words
                            Text(
                                text = "GEMINI ANALYZING WAVEFORMS",
                                color = GlowAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("gemini_loading_text")
                            )
                            Text(
                                text = "Separating sub-harmonics & transients cleanly...",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    is GeminiState.Success -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Rich Markdown scroll text
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = state.report,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    is GeminiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (state.message == "API_KEY_MISSING") {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "API Key Missing",
                                    tint = StemVocal,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Gemini Key Required",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "To access flawless AI analysis, please add your GEMINI_API_KEY in the AI Studio Secrets panel. This unlocks advanced crossovers immediately.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Connection issue",
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Acoustics Extraction Limit",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.message,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 4,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Track Browser & Audio Library Panel ---
@Composable
fun TrackLibraryCard(
    viewModel: StemViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedSongs by viewModel.savedSongsList.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    // Activity launcher for standard Android audio file imports (GetContent)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val filename = resolveUriFilename(context, it) ?: "Imported Track"
            viewModel.importUserSong(
                context = context,
                uri = it,
                title = filename,
                artist = "Device Audio",
                genre = "Acoustic Sample"
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OutlineGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicVideo,
                        contentDescription = "Library Icon",
                        tint = StemBass,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "CONSOLE LIBRARY",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "(${savedSongs.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Import Button
                Button(
                    onClick = { documentPickerLauncher.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = StemBass),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("import_audio_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import icon",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "IMPORT SONG",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = OutlineGrey)
            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable list of tracks
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (savedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs loaded yet.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedSongs) { song ->
                            val isSelected = song.id == currentSong?.id
                            TrackItemRow(
                                song = song,
                                isSelected = isSelected,
                                onSelect = { viewModel.selectSong(context, song) },
                                onDelete = { viewModel.deleteCurrentSong(context) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItemRow(
    song: SongProject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceElevated else BackgroundDark.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = if (isSelected) StemBass.copy(alpha = 0.5f) else OutlineGrey,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("track_${song.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Equalizer animation/Icon on left
            Icon(
                imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isSelected) StemBass else TextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Metadata Layout
            Column {
                Text(
                    text = song.title,
                    color = if (isSelected) StemBass else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Source label
            Text(
                text = if (song.filePath.startsWith("internal://")) "SAMP" else "USER",
                color = if (song.filePath.startsWith("internal://")) StemMelody else TextSecondary,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(SurfaceDark, RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Delete button for custom files (We prevent deleting the initial preseeded ones to remain safe)
            if (!song.filePath.startsWith("internal://") && isSelected) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove song",
                        tint = StemVocal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- Content URI Filename Resolution Helper ---
fun resolveUriFilename(context: Context, uri: Uri): String? {
    var nameToReturn: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    nameToReturn = it.getString(nameIndex)
                }
            }
        }
    }
    if (nameToReturn == null) {
        nameToReturn = uri.path
        val cut = nameToReturn?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            nameToReturn = nameToReturn?.substring(cut + 1)
        }
    }
    // Clean file extension if present
    nameToReturn?.let {
        val dotIndex = it.lastIndexOf('.')
        if (dotIndex != -1) {
            return it.substring(0, dotIndex)
        }
    }
    return nameToReturn
}
