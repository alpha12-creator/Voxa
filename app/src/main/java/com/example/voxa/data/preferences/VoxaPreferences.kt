package com.example.voxa.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

enum class AccentColor(val key: String, val hex: Long, val displayName: String) {
    BLUE("blue", 0xFF3D8BFF, "Electric Blue"),
    GREEN("green", 0xFF34C77B, "Emerald Green"),
    PURPLE("purple", 0xFF9B6DFF, "Neon Purple"),
    ORANGE("orange", 0xFFFF9840, "Vibrant Orange"),
    PINK("pink", 0xFFFF5C9E, "Hot Pink");

    val color: Color
        get() = Color(hex)

    companion object {
        fun fromKey(key: String): AccentColor =
            entries.firstOrNull { it.key == key } ?: BLUE
    }
}

data class VoxaSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.BLUE,
    val keepArchivedByDefault: Boolean = false,
    val binRetentionDays: Int = 30,
    val notificationsEnabled: Boolean = true,
    val hideMessagePreview: Boolean = false,
    val deliveryReportsEnabled: Boolean = true
)

class VoxaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voxa_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<VoxaSettings> = _settings.asStateFlow()

    private fun loadSettings(): VoxaSettings {
        val themeStr = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        val accentStr = prefs.getString(KEY_ACCENT, AccentColor.BLUE.key) ?: AccentColor.BLUE.key
        val accentColor = AccentColor.fromKey(accentStr)
        val keepArchived = prefs.getBoolean(KEY_KEEP_ARCHIVED, false)
        val binDays = prefs.getInt(KEY_BIN_DAYS, 30)
        val notif = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        val hidePreview = prefs.getBoolean(KEY_HIDE_PREVIEW, false)
        val deliveryReports = prefs.getBoolean(KEY_DELIVERY_REPORTS, true)

        return VoxaSettings(
            themeMode = themeMode,
            accentColor = accentColor,
            keepArchivedByDefault = keepArchived,
            binRetentionDays = binDays,
            notificationsEnabled = notif,
            hideMessagePreview = hidePreview,
            deliveryReportsEnabled = deliveryReports
        )
    }

    fun setDeliveryReportsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DELIVERY_REPORTS, enabled).apply()
        _settings.value = _settings.value.copy(deliveryReportsEnabled = enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccentColor(accent: AccentColor) {
        prefs.edit().putString(KEY_ACCENT, accent.key).apply()
        _settings.value = _settings.value.copy(accentColor = accent)
    }

    fun setKeepArchivedByDefault(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_ARCHIVED, enabled).apply()
        _settings.value = _settings.value.copy(keepArchivedByDefault = enabled)
    }

    fun setBinRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_BIN_DAYS, days).apply()
        _settings.value = _settings.value.copy(binRetentionDays = days)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun setHideMessagePreview(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_PREVIEW, enabled).apply()
        _settings.value = _settings.value.copy(hideMessagePreview = enabled)
    }

    fun getQuickReplies(): List<String> {
        val stored = prefs.getString(KEY_QUICK_REPLIES, null)
        return if (stored.isNullOrBlank()) {
            DEFAULT_QUICK_REPLIES
        } else {
            stored.split("|||").filter { it.isNotBlank() }
        }
    }

    fun saveQuickReplies(replies: List<String>) {
        val encoded = replies.joinToString("|||")
        prefs.edit().putString(KEY_QUICK_REPLIES, encoded).apply()
    }

    fun addQuickReply(reply: String) {
        val current = getQuickReplies().toMutableList()
        if (reply.isNotBlank() && !current.contains(reply)) {
            current.add(0, reply.trim())
            saveQuickReplies(current)
        }
    }

    fun removeQuickReply(reply: String) {
        val current = getQuickReplies().toMutableList()
        current.remove(reply)
        saveQuickReplies(current)
    }

    fun resetQuickRepliesToDefault() {
        saveQuickReplies(DEFAULT_QUICK_REPLIES)
    }

    companion object {
        val DEFAULT_QUICK_REPLIES = listOf(
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

        private const val KEY_THEME = "theme_mode"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_KEEP_ARCHIVED = "keep_archived_default"
        private const val KEY_BIN_DAYS = "bin_retention_days"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_HIDE_PREVIEW = "hide_message_preview"
        private const val KEY_DELIVERY_REPORTS = "delivery_reports_enabled"
        private const val KEY_QUICK_REPLIES = "custom_quick_replies"
    }
}
