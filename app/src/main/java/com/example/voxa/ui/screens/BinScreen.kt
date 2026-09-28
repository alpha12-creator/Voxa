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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun BinScreen(
    conversations: List<Conversation>,
    retentionDays: Int,
    onBack: () -> Unit,
    onRestoreFromBin: (Long) -> Unit,
    onDeletePermanently: (Long) -> Unit,
    onEmptyBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val colors = LocalVoxaColors.current
    var selectedConvForMenu by remember { mutableStateOf<Conversation?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }

    if (showEmptyBinConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyBinConfirm = false },
            title = { Text("Empty Bin?") },
            text = { Text("Permanently delete all conversations in Bin? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyBinConfirm = false
                        onEmptyBin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                ) {
                    Text("Empty Bin")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEmptyBinConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
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
            IconButton(onClick = onBack, modifier = Modifier.testTag("bin_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }
            Text(
                text = "Bin",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            if (conversations.isNotEmpty()) {
                TextButton(
                    onClick = { showEmptyBinConfirm = true },
                    modifier = Modifier.testTag("empty_bin_top_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Empty Bin",
                        tint = colors.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Empty",
                        color = colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        if (conversations.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface2)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items in Bin are auto-deleted after $retentionDays days",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            HorizontalDivider(color = colors.border, thickness = 0.5.dp)
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
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your bin is empty.",
                        color = colors.textSecondary,
                        fontSize = 14.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bin_list")
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { selectedConvForMenu = conv },
                            onLongClick = { selectedConvForMenu = conv }
                        )
                    }
                }
            }

            selectedConvForMenu?.let { conv ->
                ConversationActionDialog(
                    conversation = conv,
                    kind = ConversationListKind.BIN,
                    onDismiss = { selectedConvForMenu = null },
                    onTogglePin = {},
                    onToggleMute = {},
                    onToggleUnread = {},
                    onArchive = {},
                    onRestoreArchive = {},
                    onToggleKeepArchived = {},
                    onMoveToBin = {},
                    onRestoreFromBin = { onRestoreFromBin(conv.id) },
                    onDeletePermanently = { onDeletePermanently(conv.id) },
                    onToggleBlock = {}
                )
            }
        }
    }
}
