package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelMetadata
import com.example.ui.theme.CanvasSurface
import com.example.ui.theme.DisabledWarmContent
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.OutlineVariantColor
import com.example.ui.theme.PrimaryDustyRose
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SecondaryMutedCoral
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryForest
import com.example.ui.theme.WarmMutedText
import com.example.ui.theme.WarmTrackBackground
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    activeModel: ModelMetadata?,
    telemetry: HardwareTelemetry,
    onActiveModelClicked: () -> Unit,
    onClearConversations: () -> Unit,
    onUpdateContextLength: (Int) -> Unit,
    onUpdateTemperature: (Float) -> Unit,
    onToggleHistory: (Boolean) -> Unit,
    onToggleThinking: (Boolean) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val contextSteps = listOf(2048, 4096, 8192)
    val initialIdx = contextSteps.indexOf(settings.contextLength).coerceAtLeast(0)
    var contextIndex by remember(settings.contextLength) { mutableIntStateOf(if (initialIdx != -1) initialIdx else 1) }
    var currentTemperature by remember(settings.temperature) { mutableStateOf(settings.temperature) }
    var historyEnabled by remember(settings.conversationHistoryEnabled) { mutableStateOf(settings.conversationHistoryEnabled) }
    var thinkingEnabled by remember(settings.enableThinking) { mutableStateOf(settings.enableThinking) }

    val modelsMB = telemetry.modelsStorageBytes.toDouble() / (1024.0 * 1024.0)
    val chatsMB = telemetry.chatsStorageBytes.toDouble() / (1024.0 * 1024.0)
    val totalUsedMB = modelsMB + chatsMB
    val totalUsedStr = if (totalUsedMB >= 1024.0) {
        String.format(Locale.US, "%.2f GB", totalUsedMB / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", totalUsedMB)
    }
    val modelsStr = if (modelsMB >= 1024.0) {
        String.format(Locale.US, "Models: %.2f GB", modelsMB / 1024.0)
    } else {
        String.format(Locale.US, "Models: %.1f MB", modelsMB)
    }
    val chatsStr = String.format(Locale.US, "Chats: %.1f MB", chatsMB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasSurface)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Screen Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SecondaryMutedCoral)
                )
                Text(
                    text = "Hardware Accelerated",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Preferences and device configuration",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }

        // Section 1: Model Inference
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                icon = Icons.Default.Memory,
                title = "Model Inference",
                badgeText = "NPU Active"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Active Model Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActiveModelClicked() }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Model",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (activeModel != null) "${activeModel.name} · ${activeModel.quantization}" else "No active model · Tap to select",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (activeModel != null) PrimaryDustyRose else OutlineColor)
                                )
                                Text(
                                    text = if (activeModel != null) "Loaded" else "None",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeModel != null) PrimaryDustyRose else OnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OutlineColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Context Length Stepper
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Context Length",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allocated memory window",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%,d", contextSteps[contextIndex])} tokens",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PrimaryDustyRose,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (contextIndex > 0) {
                                        contextIndex--
                                        onUpdateContextLength(contextSteps[contextIndex])
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = OnSurface, modifier = Modifier.size(18.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(WarmTrackBackground)
                            ) {
                                val fillFraction = (contextIndex + 1).toFloat() / contextSteps.size.toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fillFraction)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryDustyRose)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (contextIndex < contextSteps.size - 1) {
                                        contextIndex++
                                        onUpdateContextLength(contextSteps[contextIndex])
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = OnSurface, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Temperature Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Temperature",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", currentTemperature)} · Balanced creativity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }

                            Text(
                                text = "Precise ↔ Wild",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant
                            )
                        }

                        Slider(
                            value = currentTemperature,
                            onValueChange = {
                                currentTemperature = it
                                onUpdateTemperature(it)
                            },
                            valueRange = 0.0f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryDustyRose,
                                activeTrackColor = PrimaryDustyRose,
                                inactiveTrackColor = WarmTrackBackground
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Max Response Tokens
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Maximum Response",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Generation cap per turn",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${settings.maxResponseTokens} tokens",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // CPU Threads
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Inference Threads",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Auto-detected for ${telemetry.cpuCoreCount} core CPU",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryMutedCoral)
                            )
                            Text(
                                text = "${settings.cpuThreads} Threads",
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Thinking / Reasoning Mode (Default: Disabled)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Thinking / Reasoning Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Disable for direct answers without internal reasoning",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Switch(
                            checked = thinkingEnabled,
                            onCheckedChange = {
                                thinkingEnabled = it
                                onToggleThinking(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = PrimaryDustyRose,
                                uncheckedThumbColor = DisabledWarmContent,
                                uncheckedTrackColor = SurfaceContainerHigh
                            )
                        )
                    }
                }
            }
        }

        // Section 2: Privacy Promise (Delight Highlight Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(PrimaryFixed.copy(alpha = 0.3f))
                )

                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = PrimaryDustyRose,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "100% Private & Offline",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = SecondaryMutedCoral,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Your models, prompts, and personal notes never leave this smartphone. No cloud telemetry, no analytics, no external servers. Ever.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Section 3: Chat & Storage
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                icon = Icons.Default.FolderSpecial,
                title = "Chat & Storage"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Conversation History
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Conversation History",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Store local chat transcripts",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Switch(
                            checked = historyEnabled,
                            onCheckedChange = {
                                historyEnabled = it
                                onToggleHistory(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = PrimaryDustyRose,
                                uncheckedThumbColor = SurfaceContainerLowest,
                                uncheckedTrackColor = WarmTrackBackground
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Export Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Conversations exported to internal storage", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Export Conversations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Plaintext, Markdown, or JSON bundle",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export",
                                tint = OnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Storage Breakdown (Dynamic)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Internal Storage Used",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = totalUsedStr,
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Multi-segment Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh)
                                .padding(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val modelsRatio = if (totalUsedMB > 0) (modelsMB / totalUsedMB).toFloat().coerceIn(0.1f, 0.9f) else 0.88f
                            val chatsRatio = 1.0f - modelsRatio

                            Box(
                                modifier = Modifier
                                    .weight(modelsRatio)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryDustyRose)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(chatsRatio)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryMutedCoral)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryDustyRose)
                                )
                                Text(
                                    text = modelsStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryMutedCoral)
                                )
                                Text(
                                    text = chatsStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Clear Conversations Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClearConversations()
                                Toast.makeText(context, "Memory cleared", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Clear All Conversations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryMutedCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Deletes session context from memory",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = SecondaryMutedCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Section 4: Appearance
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                icon = Icons.Default.Palette,
                title = "Appearance"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Warm ivory aesthetic",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryFixed)
                            )
                            Text(
                                text = settings.themeName,
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reading Text Size",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Optimal conversational scale",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "A", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(OutlineVariantColor))
                            Text(text = settings.readingTextSize, style = MaterialTheme.typography.labelMedium, color = PrimaryDustyRose, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(OutlineVariantColor))
                            Text(text = "A", style = MaterialTheme.typography.headlineSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section 5: Device & About
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                icon = Icons.Default.Info,
                title = "Device & About"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Target Device
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = OnSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Host Device",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = telemetry.hostDevice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TertiaryForest,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Version
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local AI Version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Build 418 · llama.cpp runtime backend",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "v1.2.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = SurfaceContainerHigh, thickness = 1.dp)

                    // Licenses Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "llama.cpp (MIT) · Qwen3 (Apache 2.0)", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open Source Licenses & Credits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = OutlineColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footnote
            Text(
                text = "Grounded in pure local silicon. Designed for zero leakage.",
                style = MaterialTheme.typography.labelSmall,
                color = WarmMutedText,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp).navigationBarsPadding())
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryDustyRose,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        if (badgeText != null) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryMutedCoral,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
