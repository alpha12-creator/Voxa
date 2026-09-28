package com.example.voxa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun NewConversationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String) -> Unit
) {
    val colors = LocalVoxaColors.current
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("new_conv_dialog")
        ) {
            Text(
                text = "New Message",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = number,
                onValueChange = {
                    number = it
                    errorText = null
                },
                label = { Text("Phone Number") },
                placeholder = { Text("+1 234 567 8900") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = errorText != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_conv_number_input")
            )

            if (errorText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorText ?: "",
                    color = colors.danger,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact Name (Optional)") },
                placeholder = { Text("Zawadi") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_conv_name_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                Button(
                    onClick = {
                        if (number.trim().isEmpty()) {
                            errorText = "Please enter a phone number"
                        } else {
                            onConfirm(name.trim(), number.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.accentText
                    ),
                    modifier = Modifier.testTag("start_chat_btn")
                ) {
                    Text("Start Chat")
                }
            }
        }
    }
}
