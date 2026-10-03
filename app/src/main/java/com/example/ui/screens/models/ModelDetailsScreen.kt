package com.example.ui.screens.models

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompatibilityLevel
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelMetadata
import com.example.data.model.ModelStatus
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.CanvasSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryDustyRose
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.SecondaryCoralAccent
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import java.util.Locale

@Composable
fun ModelDetailsScreen(
    model: ModelMetadata,
    telemetry: HardwareTelemetry,
    onBack: () -> Unit,
    onStartChatting: () -> Unit,
    onLoadModel: () -> Unit,
    onUnloadFromRAM: () -> Unit,
    onRemoveModel: () -> Unit,
    onLinkProjector: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isLoaded = model.status == ModelStatus.LOADED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasSurface)
    ) {
        SubScreenHeader(
            title = "Model Details",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Model Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(SecondaryContainer.copy(alpha = 0.15f))
                    )

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = PrimaryDustyRose,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${model.architecture.replaceFirstChar { it.uppercase() }} · ${model.parameterCount} parameters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        // Status pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isLoaded) PrimaryDustyRose else OutlineColor)
                            )
                            Text(
                                text = if (isLoaded) "Loaded in Memory · Ready" else "Stored on Device · Unloaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLoaded) PrimaryDustyRose else OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Compatibility Callout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (model.compatibilityStatus == CompatibilityLevel.INCOMPATIBLE) ErrorRed.copy(alpha = 0.12f) else SurfaceContainerLow)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (model.compatibilityStatus == CompatibilityLevel.COMPATIBLE) Icons.Default.AutoAwesome else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (model.compatibilityStatus == CompatibilityLevel.INCOMPATIBLE) ErrorRed else PrimaryDustyRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = model.compatibilityMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface,
                                lineHeight = 17.sp
                            )
                        }

                        // File path box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = OutlineColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = model.filePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("path", model.filePath))
                                        Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy path",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Key Specifications Grid (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SPECIFICATIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpecTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Save,
                        label = "File Size",
                        value = model.formattedSize,
                        subtext = "${model.parameterCount} params"
                    )
                    SpecTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Tune,
                        label = "Quantization",
                        value = model.quantization,
                        subtext = "${model.format} format"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpecTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FormatAlignLeft,
                        label = "Context Window",
                        value = String.format(Locale.US, "%,d", model.contextLength),
                        subtext = "Tokens maximum"
                    )
                    SpecTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Memory,
                        label = "Architecture",
                        value = model.architecture.replaceFirstChar { it.uppercase() },
                        subtext = "On-device JNI",
                        isValuePrimary = true
                    )
                }
            }

            // Model Capabilities Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryDustyRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Supported Capabilities",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Hardware features dynamically supported by this model's architecture on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        model.capabilities.forEach { capability ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceContainerLow)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = capability.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Multimodal Projector Association
            if (model.requiredProjector) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = null,
                                tint = PrimaryDustyRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Multimodal Projector",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (model.projectorPath != null) {
                                "Auxiliary projector linked: ${model.projectorPath}"
                            } else {
                                "No auxiliary projector (.mmproj GGUF) is currently linked. Vision and audio require a projector file."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Button(
                            onClick = onLinkProjector,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceContainerLow,
                                contentColor = OnSurface
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (model.projectorPath != null) "Change Projector File" else "Link Projector File (.mmproj)"
                            )
                        }
                    }
                }
            }

            // Performance & Hardware Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = PrimaryDustyRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Runtime Configuration",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Tuned",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryDustyRose,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    ConfigRow(
                        icon = Icons.Default.DeveloperBoard,
                        title = "Inference Mode",
                        subtitle = "Hardware target",
                        value = "CPU / NPU Accelerated"
                    )

                    HorizontalDivider(color = SurfaceContainer, thickness = 1.dp)

                    ConfigRow(
                        icon = Icons.Default.Reorder,
                        title = "Active CPU Threads",
                        subtitle = "${telemetry.hostDevice.substringBefore(" ·")} ${telemetry.cpuCoreCount}-Core",
                        value = "${model.cpuThreads} Threads"
                    )

                    HorizontalDivider(color = SurfaceContainer, thickness = 1.dp)

                    ConfigRow(
                        icon = Icons.Default.Thermostat,
                        title = "Temperature",
                        subtitle = "Sampling randomness",
                        value = "${String.format(Locale.US, "%.1f", model.temperature)} (Balanced)"
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoaded) {
                    Button(
                        onClick = onStartChatting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryDustyRose,
                            contentColor = OnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Chatting",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onUnloadFromRAM,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerLow,
                            contentColor = OnSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eject,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unload from RAM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = onLoadModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryDustyRose,
                            contentColor = OnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Load Model into RAM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Danger Remove Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .clickable { onRemoveModel() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Remove Model from Storage",
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun SpecTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String,
    isValuePrimary: Boolean = false
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OutlineColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isValuePrimary) PrimaryDustyRose else OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfigRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
