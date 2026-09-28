package com.example.voxa.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.data.model.Message
import com.example.voxa.data.model.MessageDirection
import com.example.voxa.data.model.MessageStatus
import com.example.voxa.data.model.MessageType
import com.example.voxa.sms.SmsHelper
import com.example.voxa.ui.theme.LocalVoxaColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onToggleStar: () -> Unit,
    onDeleteMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalVoxaColors.current
    val isOut = message.dir == MessageDirection.OUT
    val isScheduled = message.status == MessageStatus.SCHEDULED

    var showActionDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val bubbleShape = if (isOut) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = when {
        isScheduled -> Color.Transparent
        isOut -> colors.accent
        else -> colors.surface2
    }

    val textColor = when {
        isScheduled -> colors.text
        isOut -> colors.accentText
        else -> colors.text
    }

    val metaColor = when {
        isScheduled -> colors.textSecondary
        isOut -> colors.accentText.copy(alpha = 0.75f)
        else -> colors.textSecondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = if (isOut) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 72.dp, max = 295.dp)
                .clip(bubbleShape)
                .then(
                    if (isScheduled) {
                        Modifier.border(1.dp, colors.textSecondary, bubbleShape)
                    } else Modifier
                )
                .background(bubbleBg)
                .combinedClickable(
                    onClick = { showActionDialog = true },
                    onLongClick = { showActionDialog = true }
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag("bubble_${message.id}")
        ) {
            Column {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp
                        )
                    }
                    MessageType.VOICE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isOut) colors.surface.copy(alpha = 0.25f) else colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play voice message",
                                    tint = if (isOut) textColor else colors.accentText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.fileSize ?: "0:06",
                                color = textColor,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    MessageType.CONTACT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Contact",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.contactName ?: "Contact",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = message.contactNumber ?: "",
                                    color = metaColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    MessageType.FILE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "File",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.fileName ?: "Document",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = message.fileSize ?: "File",
                                    color = metaColor,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                    MessageType.IMAGE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface.copy(alpha = 0.5f))
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📷 Photo Attachment",
                                color = textColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isScheduled) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Scheduled",
                            tint = metaColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Scheduled · ${SmsHelper.formatTime(message.scheduledTime ?: message.time)}",
                            color = metaColor,
                            fontSize = 10.5.sp
                        )
                    } else {
                        Text(
                            text = SmsHelper.formatTime(message.time),
                            color = metaColor,
                            fontSize = 10.5.sp
                        )

                        if (message.isStarred) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = if (isOut) textColor else Color(0xFFFFB300),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        // Visual status indicator (single checkmark for sent, double checkmark for delivered / read)
                        if (isOut) {
                            Spacer(modifier = Modifier.width(3.dp))
                            when (message.status) {
                                MessageStatus.PENDING -> {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Pending",
                                        tint = metaColor,
                                        modifier = Modifier
                                            .size(11.dp)
                                            .testTag("status_indicator_pending_${message.id}")
                                    )
                                }
                                MessageStatus.SENT -> {
                                    // Single checkmark: message sent to network
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sent",
                                        tint = metaColor,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .testTag("status_indicator_sent_${message.id}")
                                    )
                                }
                                MessageStatus.DELIVERED -> {
                                    // Double checkmark: message delivered to recipient device
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Delivered",
                                        tint = metaColor,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .testTag("status_indicator_delivered_${message.id}")
                                    )
                                }
                                MessageStatus.READ -> {
                                    // Double blue checkmark: message read by recipient
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        tint = Color(0xFF38B6FF),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .testTag("status_indicator_read_${message.id}")
                                    )
                                }
                                MessageStatus.FAILED -> {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Failed",
                                        tint = colors.danger,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .testTag("status_indicator_failed_${message.id}")
                                    )
                                }
                                MessageStatus.SCHEDULED -> {
                                    // handled in isScheduled branch
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActionDialog) {
        Dialog(onDismissRequest = { showActionDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Message Options",
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                HorizontalDivider(color = colors.border, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))

                // Copy
                if (message.text.isNotBlank()) {
                    MessageActionRow(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy text",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            showActionDialog = false
                        }
                    )
                }

                // Star / Unstar
                MessageActionRow(
                    icon = Icons.Default.Star,
                    label = if (message.isStarred) "Unstar message" else "Star message",
                    tint = if (message.isStarred) Color(0xFFFFB300) else colors.text,
                    onClick = {
                        onToggleStar()
                        showActionDialog = false
                    }
                )

                // Share / Forward
                MessageActionRow(
                    icon = Icons.Default.Share,
                    label = "Share / Forward",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share message"))
                        showActionDialog = false
                    }
                )

                // Details
                MessageActionRow(
                    icon = Icons.Default.Info,
                    label = "Message details",
                    onClick = {
                        showActionDialog = false
                        showDetailsDialog = true
                    }
                )

                // Delete
                MessageActionRow(
                    icon = Icons.Default.Delete,
                    label = "Delete message",
                    tint = colors.danger,
                    onClick = {
                        onDeleteMessage()
                        showActionDialog = false
                    }
                )
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Message Details") },
            text = {
                Column {
                    Text("Type: ${message.type.name}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Direction: ${if (isOut) "Outgoing" else "Incoming"}")
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusDesc = when (message.status) {
                        MessageStatus.PENDING -> "Pending"
                        MessageStatus.SENT -> "Sent (✓ Single checkmark)"
                        MessageStatus.DELIVERED -> "Delivered (✓✓ Double checkmark)"
                        MessageStatus.READ -> "Read (✓✓ Double blue checkmark)"
                        MessageStatus.FAILED -> "Failed to send"
                        MessageStatus.SCHEDULED -> "Scheduled"
                    }
                    Text("Status: $statusDesc")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Date & Time: ${SmsHelper.formatDetailTime(message.time)}")
                    if (message.scheduledTime != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Scheduled For: ${SmsHelper.formatDetailTime(message.scheduledTime)}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDetailsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun MessageActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalVoxaColors.current
    val itemColor = tint ?: colors.text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
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
