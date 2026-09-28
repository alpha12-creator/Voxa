package com.example.voxa.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.data.model.Conversation
import com.example.voxa.data.model.Message
import com.example.voxa.data.model.MessageType
import com.example.voxa.data.preferences.VoxaPreferences
import com.example.voxa.ui.components.AvatarView
import com.example.voxa.ui.components.EmojiPicker
import com.example.voxa.ui.components.MessageBubble
import com.example.voxa.ui.components.ScheduleDialog
import com.example.voxa.ui.theme.LocalVoxaColors

val PREDEFINED_QUICK_REPLIES = listOf(
    "Yes",
    "No",
    "Talk later",
    "OK",
    "On my way!",
    "Can't talk now",
    "Call you later",
    "Thanks!",
    "Sounds good"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(
    conversation: Conversation,
    messages: List<Message>,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: MessageType, dataUri: String?, fileName: String?, fileSize: String?, contactName: String?, contactNumber: String?) -> Unit,
    onScheduleMessage: (text: String, scheduledTime: Long) -> Unit,
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

    var quickReplies by remember { mutableStateOf(voxaPreferences.getQuickReplies()) }
    var showQuickReplies by remember { mutableStateOf(true) }
    var showAddQuickReplyDialog by remember { mutableStateOf(false) }
    var newQuickReplyText by remember { mutableStateOf("") }

    var inputText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
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
        // Top App Bar for Thread
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

            // Contact click to open details
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showContactInfoDialog = true }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(initials = conversation.initials, size = 38.dp)

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
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
            }

            // Direct Call button
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

            // In-Chat Search button
            IconButton(
                onClick = {
                    isSearchInChatOpen = !isSearchInChatOpen
                    if (!isSearchInChatOpen) chatSearchQuery = ""
                },
                modifier = Modifier.testTag("thread_search_btn")
            ) {
                Icon(
                    imageVector = if (isSearchInChatOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search in chat",
                    tint = if (isSearchInChatOpen) colors.accent else colors.textSecondary
                )
            }

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
                shape = RoundedCornerShape(8.dp),
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

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("thread_messages_list")
        ) {
            items(filteredMessages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    onToggleStar = { onToggleStar(msg.id) },
                    onDeleteMessage = { onDeleteMessage(msg.id) }
                )
            }
        }

        // Quick Reply feature with pre-defined response chips (like 'Yes', 'No', 'Talk later')
        if (showQuickReplies) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .testTag("quick_reply_container")
            ) {
                // Header with title, add button, and dismiss toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Quick Reply",
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "QUICK REPLY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.testTag("quick_reply_label")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "+ Add" quick reply button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface2)
                                .clickable { showAddQuickReplyDialog = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("quick_reply_add_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom response",
                                tint = colors.accent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Add",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.accent
                            )
                        }

                        // Close/Minimize icon
                        IconButton(
                            onClick = { showQuickReplies = false },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("quick_reply_toggle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hide Quick Replies",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Pre-defined response chips row at the bottom of the active chat view
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickReplies.forEach { reply ->
                        val lower = reply.trim().lowercase()
                        val chipTag = when (lower) {
                            "yes" -> "quick_reply_chip_yes"
                            "no" -> "quick_reply_chip_no"
                            "talk later" -> "quick_reply_chip_talk_later"
                            else -> "quick_reply_chip_${lower.replace(" ", "_").replace("'", "").take(20)}"
                        }

                        val chipIcon = when (lower) {
                            "yes" -> Icons.Default.Check
                            "no" -> Icons.Default.Close
                            "talk later" -> Icons.Default.Schedule
                            "ok" -> Icons.Default.ThumbUp
                            "on my way!" -> Icons.Default.NearMe
                            "can't talk now", "can't talk right now" -> Icons.Default.CallEnd
                            "call you later" -> Icons.Default.Phone
                            "thanks!", "thanks" -> Icons.Default.Favorite
                            else -> Icons.Default.FlashOn
                        }

                        val iconTint = when (lower) {
                            "yes" -> Color(0xFF34C77B)
                            "no" -> Color(0xFFFF5C5C)
                            "talk later" -> Color(0xFFFF9840)
                            else -> colors.accent
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface2)
                                .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = {
                                        onSendMessage(reply, MessageType.TEXT, null, null, null, null, null)
                                        Toast.makeText(context, "Sent: \"$reply\"", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongClick = {
                                        inputText = reply
                                        Toast.makeText(context, "Inserted \"$reply\" into text field", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag(chipTag),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = chipIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = reply,
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
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

        // Composer bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(
                    onClick = { showAttachMenu = true },
                    modifier = Modifier.testTag("composer_attach_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach",
                        tint = colors.textSecondary
                    )
                }

                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📷 Photo") },
                        onClick = {
                            showAttachMenu = false
                            onSendMessage("Photo attachment", MessageType.IMAGE, "sample_uri", null, null, null, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📄 File Document") },
                        onClick = {
                            showAttachMenu = false
                            onSendMessage("Document attachment", MessageType.FILE, null, "Contract_Notes.pdf", "184 KB", null, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("👤 Contact Card") },
                        onClick = {
                            showAttachMenu = false
                            onSendMessage("Shared Contact", MessageType.CONTACT, null, null, null, "Amina Ali", "+255 744 112 334")
                        }
                    )
                }
            }

            IconButton(
                onClick = { showEmojiPicker = !showEmojiPicker },
                modifier = Modifier.testTag("composer_emoji_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = "Emoji",
                    tint = if (showEmojiPicker) colors.accent else colors.textSecondary
                )
            }

            // Quick reply toggle button in composer
            IconButton(
                onClick = { showQuickReplies = !showQuickReplies },
                modifier = Modifier.testTag("composer_quick_reply_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = if (showQuickReplies) "Hide Quick Replies" else "Show Quick Replies",
                    tint = if (showQuickReplies) colors.accent else colors.textSecondary
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message (SMS)") },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
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
                    .padding(horizontal = 4.dp)
                    .testTag("composer_input")
            )

            IconButton(
                onClick = { showScheduleDialog = true },
                modifier = Modifier.testTag("composer_schedule_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Schedule SMS",
                    tint = colors.textSecondary
                )
            }

            // If input is empty, show Microphone for Voice note; otherwise Send button!
            if (inputText.trim().isEmpty()) {
                IconButton(
                    onClick = {
                        onSendMessage("Voice note", MessageType.VOICE, null, null, "0:08", null, null)
                        Toast.makeText(context, "Voice message sent", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .testTag("composer_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showScheduleDialog) {
            ScheduleDialog(
                onDismiss = { showScheduleDialog = false },
                onConfirm = { scheduledTime ->
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty()) {
                        onScheduleMessage(trimmed, scheduledTime)
                        inputText = ""
                        showScheduleDialog = false
                    }
                }
            )
        }

        // Add Custom Quick Reply Dialog
        if (showAddQuickReplyDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddQuickReplyDialog = false
                    newQuickReplyText = ""
                },
                title = { Text("Add Quick Reply") },
                text = {
                    Column {
                        Text(
                            text = "Add a pre-defined response chip for 1-tap fast replies in active chats.",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newQuickReplyText,
                            onValueChange = { newQuickReplyText = it },
                            placeholder = { Text("e.g. In a meeting, Talk soon...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_quick_reply_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = newQuickReplyText.trim()
                            if (trimmed.isNotEmpty()) {
                                voxaPreferences.addQuickReply(trimmed)
                                quickReplies = voxaPreferences.getQuickReplies()
                                newQuickReplyText = ""
                                showAddQuickReplyDialog = false
                                Toast.makeText(context, "Quick reply added", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("save_quick_reply_btn")
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showAddQuickReplyDialog = false
                            newQuickReplyText = ""
                        }
                    ) {
                        Text("Cancel")
                    }
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
