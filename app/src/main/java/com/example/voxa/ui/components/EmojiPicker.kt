package com.example.voxa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voxa.ui.theme.LocalVoxaColors

val DEFAULT_EMOJIS = listOf(
    "😀", "😂", "😍", "👍", "🙏", "🔥", "🎉", "😢",
    "😅", "❤️", "😎", "🤔", "👏", "🙌", "💯", "😴",
    "😇", "🤝", "📌", "⭐", "😡", "🥳", "👀", "✅"
)

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVoxaColors.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(colors.surface),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(DEFAULT_EMOJIS) { emoji ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
            }
        }
    }
}
