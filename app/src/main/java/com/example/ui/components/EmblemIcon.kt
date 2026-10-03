package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Local AI Brand Emblem - Exact representation of the Warm Sanctuary orbit motif.
 */
@Composable
fun LocalAIEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showContainer: Boolean = true
) {
    if (showContainer) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_local_ai_emblem),
                contentDescription = "Local AI Emblem",
                tint = Color.Unspecified,
                modifier = Modifier.size(size)
            )
        }
    } else {
        Icon(
            painter = painterResource(id = R.drawable.ic_local_ai_emblem),
            contentDescription = "Local AI Emblem",
            tint = Color.Unspecified,
            modifier = modifier.size(size)
        )
    }
}
