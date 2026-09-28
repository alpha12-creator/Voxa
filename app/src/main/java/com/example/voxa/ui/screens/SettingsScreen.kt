package com.example.voxa.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voxa.data.preferences.AccentColor
import com.example.voxa.data.preferences.ThemeMode
import com.example.voxa.data.preferences.VoxaSettings
import com.example.voxa.sms.SmsHelper
import com.example.voxa.ui.theme.LocalVoxaColors

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton

@Composable
fun SettingsScreen(
    settings: VoxaSettings,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentColor: (AccentColor) -> Unit,
    onSetKeepArchivedByDefault: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetHideMessagePreview: (Boolean) -> Unit,
    onEmptyBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val colors = LocalVoxaColors.current
    var showEmptyBinConfirm by remember { mutableStateOf(false) }
    var showSmsDefaultInfo by remember { mutableStateOf(false) }

    val isDefaultSms = remember { SmsHelper.isDefaultSmsApp(context) }

    if (showEmptyBinConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyBinConfirm = false },
            title = { Text("Empty Bin?") },
            text = { Text("Permanently delete all conversations currently in Bin?") },
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

    if (showSmsDefaultInfo) {
        AlertDialog(
            onDismissRequest = { showSmsDefaultInfo = false },
            title = { Text("Default SMS App") },
            text = {
                Text(
                    "Setting VOXA as your default SMS app enables full SMS sending and receiving capabilities with low memory and fast startup on your device."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSmsDefaultInfo = false
                        val intent = SmsHelper.buildDefaultSmsIntent(context)
                        intent?.let { context.startActivity(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.accentText)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSmsDefaultInfo = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("settings_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }
            Text(
                text = "Settings",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Section: Appearance
        SectionHeader("Appearance")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeSegmentButton(
                label = "Dark",
                isSelected = settings.themeMode == ThemeMode.DARK,
                onClick = { onSetThemeMode(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
            ThemeSegmentButton(
                label = "Light",
                isSelected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { onSetThemeMode(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeSegmentButton(
                label = "System",
                isSelected = settings.themeMode == ThemeMode.SYSTEM,
                onClick = { onSetThemeMode(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Accent Color",
            color = colors.text,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AccentColor.entries.forEach { accent ->
                val isSelected = settings.accentColor == accent
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) colors.text else colors.border,
                            shape = CircleShape
                        )
                        .clickable { onSetAccentColor(accent) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp, modifier = Modifier.padding(top = 10.dp))

        // Section: Archive
        SectionHeader("Archive")
        SettingSwitchRow(
            title = "Keep archived by default",
            subtitle = "New incoming messages won't automatically un-archive conversations",
            checked = settings.keepArchivedByDefault,
            onCheckedChange = onSetKeepArchivedByDefault
        )

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // Section: Bin
        SectionHeader("Bin")
        SettingTextRow(
            title = "Auto-delete period",
            value = "${settings.binRetentionDays} days"
        )
        SettingClickableRow(
            title = "Empty bin",
            titleColor = colors.danger,
            onClick = { showEmptyBinConfirm = true }
        )

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // Section: Notifications
        SectionHeader("Notifications")
        SettingSwitchRow(
            title = "Enable notifications",
            subtitle = "Show notification banners when new messages arrive",
            checked = settings.notificationsEnabled,
            onCheckedChange = onSetNotificationsEnabled
        )
        SettingSwitchRow(
            title = "Hide message preview",
            subtitle = "Keep message body private on lock screen & notifications",
            checked = settings.hideMessagePreview,
            onCheckedChange = onSetHideMessagePreview
        )

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // Section: Default SMS App
        SectionHeader("Default SMS App")
        SettingClickableRow(
            title = if (isDefaultSms) "VOXA is your default SMS app" else "Set VOXA as default SMS app",
            subtitle = if (isDefaultSms) "Ready to send and receive carrier SMS" else "Tap to set VOXA as default",
            titleColor = if (isDefaultSms) colors.accent else colors.accent,
            onClick = { showSmsDefaultInfo = true }
        )

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        // About VOXA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VOXA",
                color = colors.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version 1.0.0",
                color = colors.textSecondary,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Private. Simple. Yours.",
                color = colors.textSecondary,
                fontSize = 12.5.sp
            )
        }
    }
}
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalVoxaColors.current
    Text(
        text = title.uppercase(),
        color = colors.textSecondary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun ThemeSegmentButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVoxaColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.accent else colors.surface2)
            .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.accentText else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalVoxaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surface2
            )
        )
    }
}

@Composable
private fun SettingTextRow(
    title: String,
    value: String
) {
    val colors = LocalVoxaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.text,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = colors.textSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SettingClickableRow(
    title: String,
    subtitle: String? = null,
    titleColor: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalVoxaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = titleColor ?: colors.text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
