package com.example.ui.screens.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelMetadata
import com.example.engine.ModelLoadingState
import com.example.ui.components.LocalAIEmblem
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.CanvasSurface
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryDustyRose
import com.example.ui.theme.SecondaryMutedCoral
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import java.util.Locale

@Composable
fun ModelSetupFlowScreen(
    model: ModelMetadata,
    telemetry: HardwareTelemetry,
    loadingState: ModelLoadingState?,
    onCancel: () -> Unit,
    onStartChatting: () -> Unit
) {
    val scrollState = rememberScrollState()

    val rawProgress = loadingState?.progress ?: 0.05f
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "loading_prog")
    val percentInt = (animatedProgress * 100).toInt().coerceIn(0, 100)
    val isComplete = loadingState?.isComplete == true || percentInt >= 100

    val mappedGB = String.format(Locale.US, "%.2f", (loadingState?.mappedBytes ?: 0L) / 1_000_000_000.0)
    val totalGB = String.format(Locale.US, "%.2f", (if ((loadingState?.totalBytes ?: 0L) > 0L) loadingState!!.totalBytes else model.fileSize) / 1_000_000_000.0)
    val remainingSec = loadingState?.estimatedRemainingSeconds ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasSurface)
    ) {
        SubScreenHeader(
            title = "Model Setup Flow",
            onBack = onCancel,
            onClose = onCancel
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sheet Handle
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest)
            )

            // Main Setup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(SurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = PrimaryDustyRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Preparing Your AI",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                                .clickable { onCancel() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    // Circular Progress Area
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            drawCircle(
                                color = SurfaceContainer,
                                radius = size.minDimension / 2 - 8.dp.toPx(),
                                style = Stroke(width = 8.dp.toPx())
                            )
                            drawArc(
                                color = PrimaryContainer,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            LocalAIEmblem(size = 64.dp)

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainerLowest)
                                    .shadow(elevation = 1.dp, shape = CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = model.quantization,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryDustyRose,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = 4.dp)
                                .clip(CircleShape)
                                .background(PrimaryDustyRose)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$percentInt%",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress Titles
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isComplete) "${model.name} is ready in memory" else "Loading ${model.name} into memory...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Mapping neural weights into high-speed unified RAM for instantaneous, private responses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // Memory Mapped Metrics Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerLow)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryDustyRose)
                                )
                                Text(
                                    text = "Memory Mapped",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurface
                                )
                            }
                            Text(
                                text = "$mappedGB GB / $totalGB GB",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryDustyRose,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainer)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        SetupDetailRow(
                            icon = Icons.Default.Description,
                            label = "Model Weight",
                            value = model.fileName
                        )
                        SetupDetailRow(
                            icon = Icons.Default.Memory,
                            label = "Memory Target",
                            value = telemetry.hostDevice
                        )
                        SetupDetailRow(
                            icon = Icons.Default.Speed,
                            label = "Throughput",
                            value = "${loadingState?.throughputMBps ?: 184} MB/s (Direct DMA)"
                        )
                        SetupDetailRow(
                            icon = Icons.Default.Schedule,
                            label = "Est. Remaining",
                            value = if (isComplete) "Ready" else "~$remainingSec seconds",
                            isValuePrimary = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SecondaryMutedCoral)
                        )
                        Text(
                            text = "Airplane mode safe · Operating 100% offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartChatting,
                            enabled = isComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryDustyRose,
                                contentColor = OnPrimary,
                                disabledContainerColor = PrimaryContainer.copy(alpha = 0.8f),
                                disabledContentColor = OnPrimary.copy(alpha = 0.9f)
                            )
                        ) {
                            if (!isComplete) {
                                CircularProgressIndicator(
                                    color = OnPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Chatting (Ready in a moment)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Text(
                                    text = "Start Chatting",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable { onCancel() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel loading",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = PrimaryDustyRose,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Your sanctuary remains sealed",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Weights are initialized strictly in volatile on-device RAM. No telemetry or conversational logs leave this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun SetupDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isValuePrimary: Boolean = false
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
                imageVector = icon,
                contentDescription = null,
                tint = OutlineColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = if (isValuePrimary) PrimaryDustyRose else OnSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
