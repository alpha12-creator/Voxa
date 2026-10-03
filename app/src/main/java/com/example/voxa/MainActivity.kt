package com.example.voxa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.example.voxa.ui.VoxaTab
import com.example.voxa.ui.VoxaViewModel
import com.example.voxa.ui.screens.ArchiveScreen
import com.example.voxa.ui.screens.BinScreen
import com.example.voxa.ui.screens.InboxScreen
import com.example.voxa.ui.screens.QuickRepliesScreen
import com.example.voxa.ui.screens.ScheduledMessagesScreen
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

    // Runtime SMS & Contacts Permissions
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val inboxList by viewModel.inboxConversations.collectAsStateWithLifecycle()
    val archiveList by viewModel.archiveConversations.collectAsStateWithLifecycle()
    val binList by viewModel.binConversations.collectAsStateWithLifecycle()
    val blockedList by viewModel.blockedConversations.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val starredList by viewModel.starredMessages.collectAsStateWithLifecycle()
    val scheduledList by viewModel.scheduledMessages.collectAsStateWithLifecycle()

    val selectedConvIds by viewModel.selectedConversationIds.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
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
            onResendMessage = { msgId ->
                viewModel.resendMessage(msgId)
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
        // Back handling for secondary views
        if (selectedConvIds.isNotEmpty()) {
            BackHandler { viewModel.clearSelection() }
        } else if (currentTab != VoxaTab.MESSAGES) {
            BackHandler { viewModel.setTab(VoxaTab.MESSAGES) }
        }

        val showBottomNav = currentTab in listOf(VoxaTab.MESSAGES, VoxaTab.ARCHIVE, VoxaTab.SETTINGS) && selectedConvIds.isEmpty()

        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(
                        containerColor = colors.surface,
                        contentColor = colors.text,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // 1. Inbox
                        NavigationBarItem(
                            selected = currentTab == VoxaTab.MESSAGES,
                            onClick = { viewModel.setTab(VoxaTab.MESSAGES) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(
                                                containerColor = colors.accent,
                                                contentColor = colors.accentText
                                            ) {
                                                Text(text = if (unreadCount > 99) "99+" else "$unreadCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentTab == VoxaTab.MESSAGES) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Inbox"
                                    )
                                }
                            },
                            label = { Text("Inbox") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary,
                                indicatorColor = colors.surface2
                            ),
                            modifier = Modifier.testTag("nav_inbox")
                        )

                        // 2. Archive
                        NavigationBarItem(
                            selected = currentTab == VoxaTab.ARCHIVE,
                            onClick = { viewModel.setTab(VoxaTab.ARCHIVE) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (archiveCount > 0) {
                                            Badge(
                                                containerColor = colors.surface2,
                                                contentColor = colors.textSecondary
                                            ) {
                                                Text(text = "$archiveCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentTab == VoxaTab.ARCHIVE) Icons.Filled.Archive else Icons.Outlined.Archive,
                                        contentDescription = "Archive"
                                    )
                                }
                            },
                            label = { Text("Archive") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary,
                                indicatorColor = colors.surface2
                            ),
                            modifier = Modifier.testTag("nav_archive")
                        )

                        // 3. Settings
                        NavigationBarItem(
                            selected = currentTab == VoxaTab.SETTINGS,
                            onClick = { viewModel.setTab(VoxaTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == VoxaTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary,
                                indicatorColor = colors.surface2
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    VoxaTab.ARCHIVE -> {
                        ArchiveScreen(
                            conversations = archiveList,
                            onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                            onOpenConversation = { viewModel.openConversation(it) },
                            onRestoreArchive = { viewModel.restoreFromArchive(it) },
                            onToggleKeepArchived = { viewModel.toggleKeepArchived(it) },
                            onMoveToBin = { viewModel.moveToBin(it) },
                            onToggleUnread = { viewModel.toggleUnread(it) }
                        )
                    }
                    VoxaTab.BIN -> {
                        BinScreen(
                            conversations = binList,
                            retentionDays = settings.binRetentionDays,
                            onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                            onRestoreFromBin = { viewModel.restoreFromBin(it) },
                            onDeletePermanently = { viewModel.deletePermanently(it) },
                            onEmptyBin = { viewModel.emptyBin() },
                            onRestoreSelected = { ids -> viewModel.restoreFromBinMultiple(ids) },
                            onDeletePermanentlySelected = { ids -> viewModel.deletePermanentlyMultiple(ids) }
                        )
                    }
                    VoxaTab.STARRED -> {
                        StarredScreen(
                            starredList = starredList,
                            onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                            onOpenConversation = { id -> viewModel.openConversation(id) },
                            onToggleStar = {
                                viewModel.toggleStar(it)
                                viewModel.loadStarredMessages()
                            }
                        )
                    }
                    VoxaTab.SPAM_BLOCKED -> {
                        SpamBlockedScreen(
                            blockedList = blockedList,
                            onBack = { viewModel.setTab(VoxaTab.SETTINGS) },
                            onUnblock = { viewModel.unblockContact(it) },
                            onBlockNumber = { num, name -> viewModel.blockNumber(num, name) }
                        )
                    }
                    VoxaTab.SCHEDULED -> {
                        ScheduledMessagesScreen(
                            scheduledList = scheduledList,
                            onBack = { viewModel.setTab(VoxaTab.MESSAGES) },
                            onOpenConversation = { id -> viewModel.openConversation(id) },
                            onSendNow = { msgId -> viewModel.sendScheduledMessageNow(msgId) },
                            onCancelScheduled = { msgId, convId -> viewModel.cancelScheduledMessage(msgId, convId) }
                        )
                    }
                    VoxaTab.QUICK_REPLIES -> {
                        QuickRepliesScreen(
                            quickReplies = viewModel.getQuickReplies(),
                            onBack = { viewModel.setTab(VoxaTab.SETTINGS) },
                            onAddQuickReply = { viewModel.addQuickReply(it) },
                            onRemoveQuickReply = { viewModel.removeQuickReply(it) },
                            onResetDefaults = { viewModel.resetQuickRepliesToDefault() }
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
                            onSetDeliveryReports = { viewModel.setDeliveryReportsEnabled(it) },
                            onNavigateToQuickReplies = { viewModel.setTab(VoxaTab.QUICK_REPLIES) },
                            onNavigateToScheduled = { viewModel.setTab(VoxaTab.SCHEDULED) },
                            onNavigateToBlocked = { viewModel.setTab(VoxaTab.SPAM_BLOCKED) },
                            onNavigateToBin = { viewModel.setTab(VoxaTab.BIN) },
                            onEmptyBin = { viewModel.emptyBin() }
                        )
                    }
                    VoxaTab.MESSAGES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.bg)
                        ) {
                            // Top Bar on Inbox
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surface)
                                    .windowInsetsPadding(WindowInsets.statusBars)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 6.dp, top = 14.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "VOXA",
                                            color = colors.text,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    // Secondary Features in Clean Three-Dots Menu
                                    Box {
                                        IconButton(
                                            onClick = { showThreeDotsMenu = true },
                                            modifier = Modifier.testTag("top_three_dots_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More Options",
                                                tint = colors.text
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showThreeDotsMenu,
                                            onDismissRequest = { showThreeDotsMenu = false }
                                        ) {
                                            // 1. Starred messages
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

                                            // 2. Scheduled messages
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = colors.accent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                },
                                                text = { Text("Scheduled messages") },
                                                onClick = {
                                                    showThreeDotsMenu = false
                                                    viewModel.setTab(VoxaTab.SCHEDULED)
                                                }
                                            )

                                            // 3. Bin
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
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    showThreeDotsMenu = false
                                                    viewModel.setTab(VoxaTab.BIN)
                                                }
                                            )

                                            // 4. Blocked numbers
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
                                                        Text("Blocked numbers")
                                                        if (blockedCount > 0) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "($blockedCount)",
                                                                color = colors.textSecondary,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    showThreeDotsMenu = false
                                                    viewModel.setTab(VoxaTab.SPAM_BLOCKED)
                                                }
                                            )

                                            HorizontalDivider(
                                                color = colors.border,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )

                                            // 5. Mark all as read
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

                                            // 6. Sync messages
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Sync,
                                                        contentDescription = null,
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                },
                                                text = { Text("Sync SMS") },
                                                onClick = {
                                                    showThreeDotsMenu = false
                                                    viewModel.syncDeviceMessages()
                                                    Toast.makeText(context, "Syncing messages...", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                            }

                            // Inbox Screen with list, search, categories, and multi-selection
                            InboxScreen(
                                conversations = inboxList,
                                selectedCategory = selectedCategory,
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
                                onToggleBlock = { viewModel.toggleBlock(it) },
                                selectedConversationIds = selectedConvIds,
                                onToggleSelectConversation = { viewModel.toggleSelectConversation(it) },
                                onSelectAll = { viewModel.selectAll(it) },
                                onClearSelection = { viewModel.clearSelection() },
                                onArchiveSelected = { viewModel.archiveSelected() },
                                onMoveToBinSelected = { viewModel.moveToBinSelected() },
                                onMarkSelectedAsRead = { viewModel.markSelectedAsRead() },
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSyncDeviceMessages = { viewModel.syncDeviceMessages() }
                            )
                        }
                    }
                }
            }
        }
    }
}
