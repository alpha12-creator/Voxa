package com.example.voxa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.data.model.Conversation
import com.example.voxa.ui.theme.LocalVoxaColors

enum class ConversationListKind {
    INBOX, ARCHIVE, BIN
}

@Composable
fun ConversationActionDialog(
    conversation: Conversation,
    kind: ConversationListKind,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleUnread: () -> Unit,
    onArchive: () -> Unit,
    onRestoreArchive: () -> Unit,
    onToggleKeepArchived: () -> Unit,
    onMoveToBin: () -> Unit,
    onRestoreFromBin: () -> Unit,
    onDeletePermanently: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val colors = LocalVoxaColors.current
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showDeletePermanentlyConfirm by remember { mutableStateOf(false) }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text(if (conversation.isBlocked) "Unblock Contact?" else "Block Contact?") },
            text = {
                Text(
                    if (conversation.isBlocked)
                        "Unblock ${conversation.displayName}? You will receive messages from this number again."
                    else
                        "Block ${conversation.displayName}? VOXA will filter future messages from this number where your device supports it."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirm = false
                        onToggleBlock()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (conversation.isBlocked) colors.accent else colors.danger,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (conversation.isBlocked) "Unblock" else "Block")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBlockConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    if (showDeletePermanentlyConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePermanentlyConfirm = false },
            title = { Text("Delete Permanently?") },
            text = { Text("Permanently delete this conversation with ${conversation.displayName}? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePermanentlyConfirm = false
                        onDeletePermanently()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeletePermanentlyConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp)
                .testTag("conv_action_dialog")
        ) {
            Text(
                text = conversation.displayName,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            HorizontalDivider(
                color = colors.border,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            when (kind) {
                ConversationListKind.INBOX -> {
                    ActionRow(
                        icon = Icons.Default.PushPin,
                        label = if (conversation.isPinned) "Unpin conversation" else "Pin conversation",
                        onClick = { onTogglePin(); onDismiss() }
                    )
                    ActionRow(
                        icon = if (conversation.unread) Icons.Default.MarkChatRead else Icons.Default.MarkChatUnread,
                        label = if (conversation.unread) "Mark as read" else "Mark as unread",
                        onClick = { onToggleUnread(); onDismiss() }
                    )
                    ActionRow(
                        icon = if (conversation.isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        label = if (conversation.isMuted) "Unmute notifications" else "Mute notifications",
                        onClick = { onToggleMute(); onDismiss() }
                    )
                    ActionRow(
                        icon = Icons.Default.Archive,
                        label = "Archive",
                        onClick = { onArchive(); onDismiss() }
                    )
                    ActionRow(
                        icon = Icons.Default.Delete,
                        label = "Move to Bin",
                        color = colors.danger,
                        onClick = { onMoveToBin(); onDismiss() }
                    )
                    ActionRow(
                        icon = Icons.Default.Block,
                        label = if (conversation.isBlocked) "Unblock contact" else "Block contact",
                        color = colors.danger,
                        onClick = { showBlockConfirm = true }
                    )
                }
                ConversationListKind.ARCHIVE -> {
                    ActionRow(
                        icon = Icons.Default.Unarchive,
                        label = "Restore to Inbox",
                        onClick = { onRestoreArchive(); onDismiss() }
                    )
                    ActionRow(
                        icon = Icons.Default.Archive,
                        label = if (conversation.keepArchived) "Allow new messages to un-archive" else "Keep archived on new messages",
                        onClick = { onToggleKeepArchived(); onDismiss() }
                    )
                    ActionRow(
                        icon = Icons.Default.Delete,
                        label = "Move to Bin",
                        color = colors.danger,
                        onClick = { onMoveToBin(); onDismiss() }
                    )
                }
                ConversationListKind.BIN -> {
                    ActionRow(
                        icon = Icons.Default.Restore,
                        label = "Restore to Inbox",
                        onClick = { onRestoreFromBin(); onDismiss() }
                    )
                    ActionRow(
                        icon = Icons.Default.DeleteForever,
                        label = "Delete permanently",
                        color = colors.danger,
                        onClick = { showDeletePermanentlyConfirm = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color? = null
) {
    val colors = LocalVoxaColors.current
    val itemColor = color ?: colors.text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = itemColor,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
