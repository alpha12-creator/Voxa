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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.data.model.Conversation
import com.example.voxa.ui.components.AvatarView
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun SpamBlockedScreen(
    blockedList: List<Conversation>,
    onBack: () -> Unit,
    onUnblock: (Long) -> Unit,
    onBlockNumber: (number: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val colors = LocalVoxaColors.current
    var showAddBlockDialog by remember { mutableStateOf(false) }

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
            IconButton(onClick = onBack, modifier = Modifier.testTag("spam_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }
            Text(
                text = "Spam & Blocked",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            IconButton(
                onClick = { showAddBlockDialog = true },
                modifier = Modifier.testTag("add_blocked_number_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Block number",
                    tint = colors.accent
                )
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // Informational header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface2)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Blocked contacts won't be able to reach you in inbox.",
                color = colors.textSecondary,
                fontSize = 12.5.sp
            )
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        if (blockedList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No spam or blocked numbers.",
                    color = colors.textSecondary,
                    fontSize = 14.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("blocked_list")
            ) {
                items(blockedList, key = { it.id }) { conv ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(initials = conv.initials, size = 40.dp)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.displayName,
                                    color = colors.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = conv.number,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { onUnblock(conv.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.surface2,
                                    contentColor = colors.accent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("unblock_btn_${conv.id}")
                            ) {
                                Text("Unblock", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showAddBlockDialog) {
        var num by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showAddBlockDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Block a number",
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = num,
                    onValueChange = { num = it; err = null },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 234 567 8900") },
                    singleLine = true,
                    isError = err != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (err != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = err ?: "", color = colors.danger, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name / Label (Optional)") },
                    placeholder = { Text("Spam Telemarketer") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { showAddBlockDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (num.trim().isEmpty()) {
                                err = "Please enter a number"
                            } else {
                                onBlockNumber(num.trim(), name.trim())
                                showAddBlockDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.danger,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text("Block")
                    }
                }
            }
        }
    }
}
