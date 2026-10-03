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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onToggleUnread: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val colors = LocalVoxaColors.current
    var selectedConvForMenu by remember { mutableStateOf<Conversation?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }

    val filteredList = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.number.contains(searchQuery, ignoreCase = true) ||
            it.lastMessageText.contains(searchQuery, ignoreCase = true)
        }
    }

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

            IconButton(
                onClick = {
                    isSearchOpen = !isSearchOpen
                    if (!isSearchOpen) searchQuery = ""
                },
                modifier = Modifier.testTag("archive_search_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search archive",
                    tint = if (isSearchOpen) colors.accent else colors.textSecondary
                )
            }

            if (conversations.isNotEmpty()) {
                Text(
                    text = "${conversations.size}",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        if (isSearchOpen) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search archived conversations", fontSize = 13.5.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = colors.surface2,
                    unfocusedContainerColor = colors.surface2
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("archive_search_input")
            )
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredList.isEmpty()) {
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
                        text = if (searchQuery.isNotBlank()) "No archived conversations found" else "Your archived conversations will appear here.",
                        color = colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try searching for a different name or number." else "Archived conversations are removed from the Inbox without being deleted.",
                        color = colors.textSecondary,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("archive_list")
                ) {
                    items(filteredList, key = { it.id }) { conv ->
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
                    onToggleUnread = { onToggleUnread(conv.id) },
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
