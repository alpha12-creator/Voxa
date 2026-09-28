package com.example.voxa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voxa.data.model.Conversation
import com.example.voxa.ui.components.ConversationActionDialog
import com.example.voxa.ui.components.ConversationItem
import com.example.voxa.ui.components.ConversationListKind
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun ArchiveScreen(
    conversations: List<Conversation>,
    onBack: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onRestoreArchive: (Long) -> Unit,
    onToggleKeepArchived: (Long) -> Unit,
    onMoveToBin: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val colors = LocalVoxaColors.current
    var selectedConvForMenu by remember { mutableStateOf<Conversation?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("archive_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }
            Text(
                text = "Archived",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            if (conversations.isNotEmpty()) {
                Text(
                    text = "${conversations.size}",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No archived conversations.",
                        color = colors.textSecondary,
                        fontSize = 14.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("archive_list")
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { onOpenConversation(conv.id) },
                            onLongClick = { selectedConvForMenu = conv }
                        )
                    }
                }
            }

            selectedConvForMenu?.let { conv ->
                ConversationActionDialog(
                    conversation = conv,
                    kind = ConversationListKind.ARCHIVE,
                    onDismiss = { selectedConvForMenu = null },
                    onTogglePin = {},
                    onToggleMute = {},
                    onToggleUnread = {},
                    onArchive = {},
                    onRestoreArchive = { onRestoreArchive(conv.id) },
                    onToggleKeepArchived = { onToggleKeepArchived(conv.id) },
                    onMoveToBin = { onMoveToBin(conv.id) },
                    onRestoreFromBin = {},
                    onDeletePermanently = {},
                    onToggleBlock = {}
                )
            }
        }
    }
}
