package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.CanvasSurface
import com.example.ui.theme.DisabledWarmContent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSecondaryContainer
import com.example.ui.theme.OnSecondaryFixedVariant
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.OutlineVariantColor
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryDustyRose
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.SecondaryCoralAccent
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.SecondaryMutedCoral
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SubtleWarmDivider
import com.example.ui.theme.TertiaryForest
import com.example.ui.theme.WarmIconBackground
import com.example.ui.theme.WarmMutedText

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    inputText: String,
    isGenerating: Boolean,
    tokenCount: Int,
    activeModelName: String,
    npuTemp: Int = 44,
    isVisionSupported: Boolean = false,
    isAudioSupported: Boolean = false,
    onInputChanged: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onStopGenerating: () -> Unit,
    onAttachImage: () -> Unit = {},
    onRecordAudio: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val isImeOpen = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasSurface)
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Privacy & Hardware Health Micro-banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
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
                    text = "100% Offline · Zero Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "NPU $npuTemp°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SecondaryMutedCoral,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Messages or Empty State
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ChatEmptyState(
                    onPromptSelected = { prompt ->
                        onSendMessage(prompt)
                    }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.isUser) {
                        UserMessageBubble(message = message)
                    } else {
                        AssistantMessageTurn(
                            message = message,
                            onCopyCode = { code ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
                                Toast.makeText(context, "Snippet copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                if (messages.lastOrNull { !it.isUser }?.codeSnippet != null) {
                    item {
                        // Quick Context Suggestion Chips for code
                        QuickSuggestionChips(
                            onSelectChip = { chipText ->
                                onSendMessage(chipText)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Bottom Floating Composer Capsule
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isModelActive = activeModelName.isNotBlank() && activeModelName != "No Model Active"

            // Active Model Overhead Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statusDotColor = if (isGenerating) SecondaryMutedCoral else if (isModelActive) StatusActiveGreen else DisabledWarmContent
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                val overheadStatusText = when {
                    !isModelActive -> "No Model Active · Import a GGUF model to start"
                    isGenerating -> "$activeModelName · Generating"
                    else -> "$activeModelName · Completed"
                }
                Text(
                    text = overheadStatusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmMutedText,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Unified Input Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = PrimaryDustyRose.copy(alpha = 0.08f),
                        spotColor = PrimaryDustyRose.copy(alpha = 0.12f)
                    )
                    .border(
                        width = 1.dp,
                        color = SubtleWarmDivider,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .background(SurfaceContainerLowest, RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Add Context / Vision Trigger (aligned inside left side)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isVisionSupported) PrimaryDustyRose.copy(alpha = 0.15f) else WarmIconBackground)
                        .clickable {
                            if (isVisionSupported) {
                                onAttachImage()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Image input requires a vision-capable model (e.g. Qwen-VL, LLaVA, Moondream).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach context",
                        tint = if (isVisionSupported) PrimaryDustyRose else DisabledWarmContent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Input Field (vertically centered placeholder and text)
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    textStyle = TextStyle(
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 14.sp,
                        color = OnSurface
                    ),
                    cursorBrush = SolidColor(PrimaryDustyRose),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (inputText.isEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isGenerating) {
                                        val pulseAnim = rememberInfiniteTransition(label = "composerPulse")
                                        val dotAlpha by pulseAnim.animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "composerDotAlpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SecondaryMutedCoral.copy(alpha = dotAlpha))
                                        )
                                        Text(
                                            text = "Generating response...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WarmMutedText.copy(alpha = 0.85f)
                                        )
                                    } else {
                                        Text(
                                            text = "Ask anything...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WarmMutedText.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                            innerTextField()
                        }
                    }
                )

                // Composer internal divider
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(SubtleWarmDivider)
                )

                // Mic Icon (aligned inside right side)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WarmIconBackground)
                        .clickable {
                            if (isAudioSupported) {
                                onRecordAudio()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Voice input requires an audio-capable model (e.g. Qwen-Omni, Whisper).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = if (isAudioSupported) PrimaryDustyRose else WarmMutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Send / Stop Action Button (aligned inside right edge)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGenerating) SecondaryContainer
                            else if (inputText.isNotBlank() && isModelActive) PrimaryDustyRose
                            else PrimaryDustyRose.copy(alpha = 0.5f)
                        )
                        .clickable {
                            if (isGenerating) {
                                onStopGenerating()
                            } else if (isModelActive && inputText.isNotBlank()) {
                                onSendMessage(null)
                            } else if (!isModelActive) {
                                Toast.makeText(
                                    context,
                                    "Please load a model from the Models tab first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGenerating) Icons.Default.Stop else Icons.Default.ArrowUpward,
                        contentDescription = if (isGenerating) "Stop" else "Send prompt",
                        tint = if (isGenerating) OnSecondaryContainer else OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isImeOpen) 2.dp else 6.dp))

            // Zero Cloud Egress Footnote (hidden when keyboard is open to keep composer compact and directly above IME)
            if (!isImeOpen) {
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = WarmMutedText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Zero cloud egress · Compute stays on hardware",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmMutedText
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Badge
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = PrimaryDustyRose,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = "Running on-device",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurface,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(OutlineVariantColor)
            )
            Text(
                text = "100% Private & Offline",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        // Hero Motif & Editorial Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(PrimaryFixed.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLowest)
                        .shadow(elevation = 6.dp, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryDustyRose,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SecondaryFixed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = SecondaryMutedCoral,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "How can I help?",
                style = MaterialTheme.typography.displayMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your personal assistant, thinking privately on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Suggestion Bento Grid (2x2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Psychology,
                    iconTint = PrimaryDustyRose,
                    title = "Explain simply",
                    description = "Break down complex topics",
                    onClick = { onPromptSelected("Explain how quantum superposition works in simple terms") }
                )
                BentoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Terminal,
                    iconTint = SecondaryMutedCoral,
                    title = "Help me code",
                    description = "Debug & write scripts",
                    onClick = { onPromptSelected("Can you write a concise Python function to calculate the Fibonacci sequence with memoization?") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.EditNote,
                    iconTint = PrimaryDustyRose,
                    title = "Write cleanly",
                    description = "Draft emails & essays",
                    onClick = { onPromptSelected("Draft a polite update email to my team about local AI model performance") }
                )
                BentoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Lightbulb,
                    iconTint = SecondaryMutedCoral,
                    title = "Brainstorm",
                    description = "Fresh design ideas",
                    onClick = { onPromptSelected("Brainstorm 5 creative ideas for on-device mobile intelligence apps") }
                )
            }
        }
    }
}

@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(WarmIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(SurfaceContainerLow)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            val displayTimestamp = message.timestamp.substringBefore(" ·")
            Text(
                text = displayTimestamp,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessageTurn(
    message: ChatMessage,
    onCopyCode: (String) -> Unit
) {
    val cursorTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Assistant Header & Live Inference Metronome
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PrimaryDustyRose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = message.modelName.ifBlank { "Assistant" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Generating badge - only show while actively streaming; disappears once final answer is available
            if (message.isStreaming) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecondaryFixed)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeTransition = rememberInfiniteTransition(label = "badgePulse")
                    val pulseAlpha by badgeTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "badgePulseAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SecondaryMutedCoral.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "Generating",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSecondaryFixedVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message Body
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sanitizedText = if (message.text.contains("<think>")) {
                message.text.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
            } else {
                message.text
            }

            if (message.isStreaming && sanitizedText.isBlank() && message.codeSnippet == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    val streamTransition = rememberInfiniteTransition(label = "streamPulse")
                    val streamAlpha by streamTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "streamAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SecondaryMutedCoral.copy(alpha = streamAlpha))
                    )
                    Text(
                        text = "Generating response...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmMutedText
                    )
                }
            }

            val headerMatch = Regex("^###\\s+(.+)$", RegexOption.MULTILINE).find(sanitizedText)
            val bodyText = if (headerMatch != null) {
                sanitizedText.replace(headerMatch.value, "").trim()
            } else {
                sanitizedText
            }

            if (headerMatch != null) {
                Text(
                    text = headerMatch.groupValues[1],
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val textParts = bodyText.split("\n\n")
            textParts.forEach { part ->
                if (part.isNotBlank()) {
                    Text(
                        text = part,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            // Code Block if present
            if (message.codeSnippet != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainer)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainer.copy(alpha = 0.5f))
                            )
                            Text(
                                text = (message.codeLanguage ?: "python").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onCopyCode(message.codeSnippet) }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy snippet",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    // Code Body
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = message.codeSnippet,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = OnSurface
                            )
                        )
                    }
                }
            }

            // Trailing cursor if streaming
            if (message.isStreaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 16.dp)
                            .alpha(cursorAlpha)
                            .background(PrimaryDustyRose)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Assistant Quick Action Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Thumbs up",
                        tint = WarmMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Thumbs down",
                        tint = WarmMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = WarmMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainer)
                    .clickable { }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuickSuggestionChips(
    onSelectChip: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SuggestionChip(
            icon = Icons.Default.Speed,
            iconTint = PrimaryDustyRose,
            text = "Add @functools.lru_cache variant",
            onClick = { onSelectChip("Show the @functools.lru_cache equivalent") }
        )
        SuggestionChip(
            icon = Icons.Default.AutoAwesome,
            iconTint = SecondaryCoralAccent,
            text = "Explain Call Stack",
            onClick = { onSelectChip("Explain the recursion call stack in detail") }
        )
        SuggestionChip(
            icon = Icons.Default.Terminal,
            iconTint = SecondaryMutedCoral,
            text = "Iterative O(1) space",
            onClick = { onSelectChip("Write an iterative O(1) space Fibonacci function") }
        )
    }
}

@Composable
private fun SuggestionChip(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .clickable { onClick() }
            .shadow(elevation = 1.dp, shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface
        )
    }
}
