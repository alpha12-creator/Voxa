package com.example.voxa.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.data.model.Conversation
import com.example.voxa.data.model.Message
import com.example.voxa.data.model.MessageType
import com.example.voxa.data.preferences.VoxaPreferences
import com.example.voxa.sms.SmsHelper
import com.example.voxa.ui.components.AvatarView
import com.example.voxa.ui.components.EmojiPicker
import com.example.voxa.ui.components.MessageBubble
import com.example.voxa.ui.components.ScheduleDialog
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun ThreadScreen(
    conversation: Conversation,
    messages: List<Message>,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: MessageType, dataUri: String?, fileName: String?, fileSize: String?, contactName: String?, contactNumber: String?) -> Unit,
    onScheduleMessage: (text: String, scheduledTime: Long) -> Unit,
    onResendMessage: (Long) -> Unit = {},
    onToggleStar: (msgId: Long) -> Unit,
    onDeleteMessage: (msgId: Long) -> Unit,
    onClearChatHistory: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onArchive: () -> Unit,
    onMoveToBin: () -> Unit,
    onToggleBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val colors = LocalVoxaColors.current
    val voxaPreferences = remember { VoxaPreferences(context) }

    var inputText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }
    var showQuickReplySheet by remember { mutableStateOf(false) }
    var showContactInfoDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    var isSearchInChatOpen by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }

    val filteredMessages = remember(messages, chatSearchQuery) {
        if (chatSearchQuery.isBlank()) messages
        else messages.filter { it.text.contains(chatSearchQuery, ignoreCase = true) }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear Chat History?") },
            text = { Text("All messages in this conversation will be deleted. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryConfirm = false
                        onClearChatHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("thread_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }

            // Contact header
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showContactInfoDialog = true }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(initials = conversation.initials, size = 40.dp)

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = conversation.displayName,
                        color = colors.text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = conversation.number,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            // Call button
            IconButton(
                onClick = {
                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conversation.number}"))
                    context.startActivity(callIntent)
                },
                modifier = Modifier.testTag("thread_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call Contact",
                    tint = colors.accent
                )
            }

            // In-Chat Search
            IconButton(
                onClick = {
                    isSearchInChatOpen = !isSearchInChatOpen
                    if (!isSearchInChatOpen) chatSearchQuery = ""
                },
                modifier = Modifier.testTag("thread_search_btn")
            ) {
                Icon(
                    imageVector = if (isSearchInChatOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search in conversation",
                    tint = if (isSearchInChatOpen) colors.accent else colors.textSecondary
                )
            }

            // More Options Menu
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.testTag("thread_more_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = colors.textSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Contact info") },
                        onClick = {
                            showMoreMenu = false
                            showContactInfoDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (conversation.isPinned) "Unpin" else "Pin") },
                        onClick = {
                            showMoreMenu = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (conversation.isMuted) "Unmute notifications" else "Mute notifications") },
                        onClick = {
                            showMoreMenu = false
                            onToggleMute()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = {
                            showMoreMenu = false
                            onArchive()
                            onBack()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear chat history") },
                        onClick = {
                            showMoreMenu = false
                            showClearHistoryConfirm = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete (Move to Bin)", color = colors.danger) },
                        onClick = {
                            showMoreMenu = false
                            onMoveToBin()
                            onBack()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (conversation.isBlocked) "Unblock contact" else "Block contact", color = colors.danger) },
                        onClick = {
                            showMoreMenu = false
                            onToggleBlock()
                        }
                    )
                }
            }
        }

        // In-Chat Search Bar
        if (isSearchInChatOpen) {
            OutlinedTextField(
                value = chatSearchQuery,
                onValueChange = { chatSearchQuery = it },
                placeholder = { Text("Search in this conversation") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface2,
                    unfocusedContainerColor = colors.surface2
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("in_chat_search_input")
            )
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // Messages area with Day headers
        Box(modifier = Modifier.weight(1f)) {
            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chatSearchQuery.isNotBlank()) "No messages found matching \"$chatSearchQuery\"" else "Send a message to start conversation",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                        .testTag("thread_messages_list")
                ) {
                    itemsIndexed(filteredMessages, key = { _, msg -> msg.id }) { index, msg ->
                        // Show date separator if first message or day changed
                        val showDateSeparator = index == 0 || !SmsHelper.isSameDay(msg.time, filteredMessages[index - 1].time)
                        if (showDateSeparator) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surface2)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = SmsHelper.formatDateHeader(msg.time),
                                        color = colors.textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        MessageBubble(
                            message = msg,
                            onToggleStar = { onToggleStar(msg.id) },
                            onDeleteMessage = { onDeleteMessage(msg.id) },
                            onResendMessage = { onResendMessage(msg.id) }
                        )
                    }
                }
            }
        }

        // Emoji Picker if opened
        if (showEmojiPicker) {
            HorizontalDivider(color = colors.border, thickness = 0.5.dp)
            EmojiPicker(
                onEmojiSelected = { emoji ->
                    inputText += emoji
                }
            )
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // Clean, Clutter-Free Composer Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "+" Button: reveals Schedule, Quick Reply, Attachments
            Box {
                IconButton(
                    onClick = { showPlusMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .testTag("composer_plus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "More actions",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showPlusMenu,
                    onDismissRequest = { showPlusMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        },
                        text = { Text("Quick reply") },
                        onClick = {
                            showPlusMenu = false
                            showQuickReplySheet = true
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        },
                        text = { Text("Schedule SMS") },
                        onClick = {
                            showPlusMenu = false
                            showScheduleDialog = true
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        },
                        text = { Text("Photo") },
                        onClick = {
                            showPlusMenu = false
                            onSendMessage("Photo attachment", MessageType.IMAGE, "sample_uri", null, null, null, null)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        },
                        text = { Text("Document") },
                        onClick = {
                            showPlusMenu = false
                            onSendMessage("Document attachment", MessageType.FILE, null, "Document.pdf", "140 KB", null, null)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.ContactPage, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        },
                        text = { Text("Contact card") },
                        onClick = {
                            showPlusMenu = false
                            onSendMessage("Contact Card", MessageType.CONTACT, null, null, null, "Contact", "+1 234 567 890")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Emoji toggle
            IconButton(
                onClick = { showEmojiPicker = !showEmojiPicker },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("composer_emoji_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = "Emoji",
                    tint = if (showEmojiPicker) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Message Input
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message (SMS)", fontSize = 14.5.sp) },
                maxLines = 4,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface2,
                    unfocusedContainerColor = colors.surface2
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("composer_input")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Send or Voice Note button
            if (inputText.trim().isEmpty()) {
                IconButton(
                    onClick = {
                        onSendMessage("Voice note", MessageType.VOICE, null, null, "0:07", null, null)
                        Toast.makeText(context, "Voice note sent", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .testTag("composer_mic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Voice Message",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendMessage(trimmed, MessageType.TEXT, null, null, null, null, null)
                            inputText = ""
                            showEmojiPicker = false
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .testTag("composer_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send SMS",
                        tint = colors.accentText,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Quick Reply Selection Dialog
        if (showQuickReplySheet) {
            val replies = remember { voxaPreferences.getQuickReplies() }
            Dialog(onDismissRequest = { showQuickReplySheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Replies",
                            color = colors.text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showQuickReplySheet = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(replies.size) { idx ->
                            val r = replies[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showQuickReplySheet = false
                                        onSendMessage(r, MessageType.TEXT, null, null, null, null, null)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = r,
                                    color = colors.text,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Schedule Dialog
        if (showScheduleDialog) {
            ScheduleDialog(
                onDismiss = { showScheduleDialog = false },
                onConfirm = { scheduledTime ->
                    val text = inputText.trim().ifEmpty { "Scheduled greeting" }
                    onScheduleMessage(text, scheduledTime)
                    inputText = ""
                    showScheduleDialog = false
                    Toast.makeText(context, "Message scheduled", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Contact Info Dialog
        if (showContactInfoDialog) {
            Dialog(onDismissRequest = { showContactInfoDialog = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarView(initials = conversation.initials, size = 64.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = conversation.displayName,
                        color = colors.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = conversation.number,
                        color = colors.textSecondary,
                        fontSize = 13.5.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conversation.number}"))
                                context.startActivity(callIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.accent)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call")
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Phone", conversation.number))
                                Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text)
                        ) {
                            Text("Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleBlock()
                                showContactInfoDialog = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (conversation.isBlocked) "Unblock this contact" else "Block this contact",
                            color = colors.danger,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showContactInfoDialog = false
                                showClearHistoryConfirm = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Clear conversation history",
                            color = colors.danger,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showContactInfoDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
