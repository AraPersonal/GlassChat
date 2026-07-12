package com.glasschat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glasschat.app.ui.theme.GlassBorder
import com.glasschat.app.ui.theme.GlassWhite
import com.glasschat.app.ui.theme.GlassWhiteStrong
import com.glasschat.app.ui.theme.TextPrimary

/**
 * A frosted "liquid glass" panel: soft translucent fill, subtle top-light
 * gradient and a thin glowing border, similar to iOS 26 / newest Telegram
 * surfaces. Works on every Android version (no RenderEffect dependency),
 * so it renders identically on old and new devices.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.verticalGradient(
                    listOf(GlassWhiteStrong, GlassWhite)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), GlassBorder)
                ),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xCC2AABEE), Color(0xCC7C6FF0))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
            .then(Modifier.padding(0.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(text = text, color = TextPrimary)
        }
    }
}

@Composable
fun GlassIconBadge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(GlassWhiteStrong, GlassWhite)))
            .border(1.dp, GlassBorder, CircleShape)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
