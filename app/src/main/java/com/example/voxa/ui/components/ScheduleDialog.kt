package com.example.voxa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.voxa.sms.SmsHelper
import com.example.voxa.ui.theme.LocalVoxaColors

@Composable
fun ScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (scheduledTime: Long) -> Unit
) {
    val colors = LocalVoxaColors.current
    val now = remember { System.currentTimeMillis() }

    val options = listOf(
        "In 15 minutes" to (15 * 60 * 1000L),
        "In 1 hour" to (60 * 60 * 1000L),
        "In 3 hours" to (3 * 3600 * 1000L),
        "Tomorrow morning (8:00 AM)" to calculateTomorrowMorning(now)
    )

    var selectedDelta by remember { mutableStateOf(options[0].second) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("schedule_dialog")
        ) {
            Text(
                text = "Schedule Message",
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            options.forEach { (label, delta) ->
                val targetTime = if (label.startsWith("Tomorrow")) delta else now + delta
                val isSelected = selectedDelta == delta

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.surface2 else colors.surface)
                        .border(
                            1.dp,
                            if (isSelected) colors.accent else colors.border,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedDelta = delta }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = colors.text,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = SmsHelper.formatDetailTime(targetTime),
                        color = colors.textSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                        val finalTime = if (selectedDelta > 24 * 3600 * 1000L) selectedDelta else now + selectedDelta
                        onConfirm(finalTime)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.accentText
                    ),
                    modifier = Modifier.testTag("confirm_schedule_btn")
                ) {
                    Text("Schedule")
                }
            }
        }
    }
}

private fun calculateTomorrowMorning(now: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        add(java.util.Calendar.DAY_OF_YEAR, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 8)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
