package com.example.voxa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voxa.ui.CategoryFilter
import com.example.voxa.ui.VoxaTab
import com.example.voxa.ui.VoxaViewModel
import com.example.voxa.ui.screens.ArchiveScreen
import com.example.voxa.ui.screens.BinScreen
import com.example.voxa.ui.screens.InboxScreen
import com.example.voxa.ui.screens.SettingsScreen
import com.example.voxa.ui.screens.SpamBlockedScreen
import com.example.voxa.ui.screens.StarredScreen
import com.example.voxa.ui.screens.ThreadScreen
import com.example.voxa.ui.theme.LocalVoxaColors
import com.example.voxa.ui.theme.VoxaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VoxaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            VoxaTheme(themeMode = settings.themeMode, accent = settings.accentColor) {
                VoxaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VoxaApp(viewModel: VoxaViewModel) {
    val context = LocalContext.current
    val colors = LocalVoxaColors.current

    // Request permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.READ_SMS] == true) {
            viewModel.syncDeviceMessages()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
        viewModel.syncDeviceMessages()
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val openConvId by viewModel.openConversationId.collectAsStateWithLifecycle()
    val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val inboxList by viewModel.inboxConversations.collectAsStateWithLifecycle()
    val archiveList by viewModel.archiveConversations.collectAsStateWithLifecycle()
    val binList by viewModel.binConversations.collectAsStateWithLifecycle()
    val blockedList by viewModel.blockedConversations.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val starredList by viewModel.starredMessages.collectAsStateWithLifecycle()

    val archiveCount by viewModel.archiveCount.collectAsStateWithLifecycle()
    val binCount by viewModel.binCount.collectAsStateWithLifecycle()
    val blockedCount by viewModel.blockedCount.collectAsStateWithLifecycle()

    var showThreeDotsMenu by remember { mutableStateOf(false) }

    val activeConv = openConvId?.let { viewModel.getConversation(it) }

    if (activeConv != null) {
        ThreadScreen(
            conversation = activeConv,
            messages = currentMessages,
            onBack = { viewModel.closeConversation() },
            onSendMessage = { text, type, uri, name, size, cName, cNum ->
                viewModel.sendMessage(activeConv.id, text, type, uri, name, size, cName, cNum)
            },
            onScheduleMessage = { text, time ->
                viewModel.scheduleMessage(activeConv.id, text, time)
            },
            onToggleStar = { viewModel.toggleStar(it) },
            onDeleteMessage = { viewModel.deleteMessage(it, activeConv.id) },
            onClearChatHistory = { viewModel.clearConversation(activeConv.id) },
            onTogglePin = { viewModel.togglePin(activeConv.id) },
            onToggleMute = { viewModel.toggleMute(activeConv.id) },
            onArchive = { viewModel.archive(activeConv.id) },
            onMoveToBin = { viewModel.moveToBin(activeConv.id) },
            onToggleBlock = { viewModel.toggleBlock(activeConv.id) }
        )
    } else {
        when (currentTab) {
            VoxaTab.ARCHIVE -> {
                ArchiveScreen(
                    conversations = archiveList,
                    onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                    onOpenConversation = { viewModel.openConversation(it) },
                    onRestoreArchive = { viewModel.restoreFromArchive(it) },
                    onToggleKeepArchived = { viewModel.toggleKeepArchived(it) },
                    onMoveToBin = { viewModel.moveToBin(it) }
                )
            }
            VoxaTab.BIN -> {
                BinScreen(
                    conversations = binList,
                    retentionDays = settings.binRetentionDays,
                    onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                    onRestoreFromBin = { viewModel.restoreFromBin(it) },
                    onDeletePermanently = { viewModel.deletePermanently(it) },
                    onEmptyBin = { viewModel.emptyBin() }
                )
            }
            VoxaTab.STARRED -> {
                StarredScreen(
                    starredList = starredList,
                    onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                    onOpenConversation = { id ->
                        viewModel.openConversation(id)
                    },
                    onToggleStar = {
                        viewModel.toggleStar(it)
                        viewModel.loadStarredMessages()
                    }
                )
            }
            VoxaTab.SPAM_BLOCKED -> {
                SpamBlockedScreen(
                    blockedList = blockedList,
                    onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                    onUnblock = { viewModel.unblockContact(it) },
                    onBlockNumber = { num, name -> viewModel.blockNumber(num, name) }
                )
            }
            VoxaTab.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                    onSetThemeMode = { viewModel.setThemeMode(it) },
                    onSetAccentColor = { viewModel.setAccentColor(it) },
                    onSetKeepArchivedByDefault = { viewModel.setKeepArchivedByDefault(it) },
                    onSetNotificationsEnabled = { viewModel.setNotificationsEnabled(it) },
                    onSetHideMessagePreview = { viewModel.setHideMessagePreview(it) },
                    onEmptyBin = { viewModel.emptyBin() }
                )
            }
            VoxaTab.MESSAGES -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg)
                ) {
                    // Modern Messages Top App Bar with navigation in three dots
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VOXA",
                                    color = colors.text,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = "Private. Simple. Yours.",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            // Search button
                            IconButton(
                                onClick = { viewModel.toggleSearch() },
                                modifier = Modifier.testTag("top_search_btn")
                            ) {
                                Icon(
                                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchVisible) colors.accent else colors.textSecondary
                                )
                            }

                            // Navigation via Three Dots Menu (as requested: "navigation batton should be added to three dots not below")
                            Box {
                                IconButton(
                                    onClick = { showThreeDotsMenu = true },
                                    modifier = Modifier.testTag("top_three_dots_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options & Navigation",
                                        tint = colors.text
                                    )
                                }

                                DropdownMenu(
                                    expanded = showThreeDotsMenu,
                                    onDismissRequest = { showThreeDotsMenu = false }
                                ) {
                                    // 1. Archived
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Archived")
                                                if (archiveCount > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "($archiveCount)",
                                                        color = colors.textSecondary,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.setTab(VoxaTab.ARCHIVE)
                                        }
                                    )

                                    // 2. Spam & Blocked
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Spam & Blocked")
                                                if (blockedCount > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "($blockedCount)",
                                                        color = colors.textSecondary,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.setTab(VoxaTab.SPAM_BLOCKED)
                                        }
                                    )

                                    // 3. Starred Messages
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = { Text("Starred messages") },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.setTab(VoxaTab.STARRED)
                                        }
                                    )

                                    // 4. Bin / Trash
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Bin")
                                                if (binCount > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "($binCount)",
                                                        color = colors.textSecondary,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.setTab(VoxaTab.BIN)
                                        }
                                    )

                                    HorizontalDivider(
                                        color = colors.border,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    // 5. Sync device messages
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = { Text("Sync device messages") },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.syncDeviceMessages()
                                            Toast.makeText(context, "Syncing messages...", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    // 6. Mark all as read
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DoneAll,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = { Text("Mark all as read") },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.markAllAsRead()
                                        }
                                    )

                                    // 7. Settings
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = { Text("Settings") },
                                        onClick = {
                                            showThreeDotsMenu = false
                                            viewModel.setTab(VoxaTab.SETTINGS)
                                        }
                                    )
                                }
                            }
                        }

                        // Search Input TextField if search open
                        if (isSearchVisible) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search name, number or message") },
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
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("global_search_input")
                            )
                        }

                        HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                    }

                    // Main Inbox content with search bar, category chips & conversations
                    InboxScreen(
                        conversations = inboxList,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSyncDeviceMessages = { viewModel.syncDeviceMessages() },
                        onSelectCategory = { viewModel.setCategory(it) },
                        onOpenConversation = { viewModel.openConversation(it) },
                        onStartNewConversation = { name, number ->
                            viewModel.startNewConversation(name, number)
                        },
                        onTogglePin = { viewModel.togglePin(it) },
                        onToggleMute = { viewModel.toggleMute(it) },
                        onToggleUnread = { viewModel.toggleUnread(it) },
                        onArchive = { viewModel.archive(it) },
                        onMoveToBin = { viewModel.moveToBin(it) },
                        onToggleBlock = { viewModel.toggleBlock(it) }
                    )
                }
            }
        }
    }
}
