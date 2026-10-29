package com.aacheck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val avatarPalette = listOf(
    Color(0xFFE5734A), Color(0xFF5C9E8C), Color(0xFF3A8FDE), Color(0xFF9A6BD6),
    Color(0xFFE39A2B), Color(0xFFD9538F), Color(0xFF4DB6AC), Color(0xFF7986CB),
)

@Composable
fun Avatar(name: String, size: Dp = 36.dp) {
    val color = avatarPalette[((name.hashCode() % avatarPalette.size) + avatarPalette.size) % avatarPalette.size]
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            color = Color.White,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(modifier = Modifier, contentAlignment = Alignment.Center) {
        Text(text = text, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
    }
}
