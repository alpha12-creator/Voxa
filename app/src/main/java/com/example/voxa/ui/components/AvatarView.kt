package com.example.voxa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun AvatarView(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val colors = LocalVoxaColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surface2)
            .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = colors.textSecondary,
            fontSize = (size.value * 0.35).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
