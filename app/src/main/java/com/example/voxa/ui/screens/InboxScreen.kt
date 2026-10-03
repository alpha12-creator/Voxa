package com.example.voxa.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voxa.data.model.Conversation
import com.example.voxa.ui.CategoryFilter
import com.example.voxa.ui.components.ConversationActionDialog
import com.example.voxa.ui.components.ConversationItem
import com.example.voxa.ui.components.ConversationListKind
import com.example.voxa.ui.components.NewConversationDialog
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun InboxScreen(
    conversations: List<Conversation>,
    selectedCategory: CategoryFilter,
    onSelectCategory: (CategoryFilter) -> Unit,
    onOpenConversation: (Long) -> Unit,
    onStartNewConversation: (name: String, number: String) -> Unit,
    onTogglePin: (Long) -> Unit,
    onToggleMute: (Long) -> Unit,
    onToggleUnread: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onMoveToBin: (Long) -> Unit,
    onToggleBlock: (Long) -> Unit,
    selectedConversationIds: Set<Long> = emptySet(),
    onToggleSelectConversation: (Long) -> Unit = {},
    onSelectAll: (List<Conversation>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    onMoveToBinSelected: () -> Unit = {},
    onMarkSelectedAsRead: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSyncDeviceMessages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalVoxaColors.current
    var selectedConvForMenu by remember { mutableStateOf<Conversation?>(null) }
    var showNewConvDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedConversationIds.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        if (isSelectionMode) {
            // Multi-Selection Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClearSelection, modifier = Modifier.testTag("clear_selection_btn")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection",
                        tint = colors.text
                    )
                }

                Text(
                    text = "${selectedConversationIds.size} selected",
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onSelectAll(conversations) },
                    modifier = Modifier.testTag("select_all_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select all",
                        tint = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onMarkSelectedAsRead,
                    modifier = Modifier.testTag("mark_read_selected_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkChatRead,
                        contentDescription = "Mark as read",
                        tint = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onArchiveSelected,
                    modifier = Modifier.testTag("archive_selected_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive selected",
                        tint = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onMoveToBinSelected,
                    modifier = Modifier.testTag("bin_selected_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Move selected to Bin",
                        tint = colors.danger
                    )
                }
            }
            HorizontalDivider(color = colors.border, thickness = 0.5.dp)
        } else {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search conversations or contacts...",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotBlank()) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
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
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("conversation_search_bar")
            )

            // Category Chips: All, Unread, Personal, Transactions, OTP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryFilter.entries.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colors.accent else colors.surface2)
                            .clickable { onSelectCategory(cat) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_chip_${cat.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSelected) colors.accentText else colors.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

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
                    if (searchQuery.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No messages found",
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No conversations match \"$searchQuery\".",
                            color = colors.textSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onSearchQueryChange("") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surface2,
                                contentColor = colors.accent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("empty_search_clear_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Search", fontSize = 13.sp)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedCategory != CategoryFilter.ALL)
                                "No messages in ${selectedCategory.label}"
                            else
                                "No messages yet",
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedCategory != CategoryFilter.ALL)
                                "Conversations matching ${selectedCategory.label} will appear here."
                            else
                                "Tap + to start a new conversation.",
                            color = colors.textSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        if (selectedCategory == CategoryFilter.ALL) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onSyncDeviceMessages,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.surface2,
                                    contentColor = colors.accent
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("sync_device_sms_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Messages", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("inbox_list")
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isSelected = selectedConversationIds.contains(conv.id)
                        ConversationItem(
                            conversation = conv,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelectConversation(conv.id)
                                } else {
                                    onOpenConversation(conv.id)
                                }
                            },
                            onLongClick = {
                                if (isSelectionMode) {
                                    onToggleSelectConversation(conv.id)
                                } else {
                                    selectedConvForMenu = conv
                                }
                            },
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected
                        )
                    }
                }
            }

            // Floating Action Button: + New Message
            FloatingActionButton(
                onClick = { showNewConvDialog = true },
                containerColor = colors.accent,
                contentColor = colors.accentText,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_new_message")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Message",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (showNewConvDialog) {
            NewConversationDialog(
                onDismiss = { showNewConvDialog = false },
                onConfirm = { name, number ->
                    showNewConvDialog = false
                    onStartNewConversation(name, number)
                }
            )
        }

        selectedConvForMenu?.let { conv ->
            ConversationActionDialog(
                conversation = conv,
                kind = ConversationListKind.INBOX,
                onDismiss = { selectedConvForMenu = null },
                onTogglePin = { onTogglePin(conv.id) },
                onToggleMute = { onToggleMute(conv.id) },
                onToggleUnread = { onToggleUnread(conv.id) },
                onArchive = { onArchive(conv.id) },
                onRestoreArchive = {},
                onToggleKeepArchived = {},
                onMoveToBin = { onMoveToBin(conv.id) },
                onRestoreFromBin = {},
                onDeletePermanently = {},
                onToggleBlock = { onToggleBlock(conv.id) },
                onSelectMultiple = {
                    onToggleSelectConversation(conv.id)
                }
            )
        }
    }
}
