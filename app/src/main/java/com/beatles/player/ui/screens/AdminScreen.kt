package com.beatles.player.ui.screens
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatles.player.data.AdminTab
import com.beatles.player.data.UserRole
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

private val AdminBlue = Color(0xFF2471A3)
private val DarkCardBg = Color.White.copy(alpha = 0.06f)

@Composable
fun AdminScreen(
    state: PlayerUiState,
    viewModel: PlayerViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.adminTab) {
            AdminTab.DASHBOARD -> AdminDashboardTab(state = state)
            AdminTab.USERS -> AdminUsersTab(state = state, viewModel = viewModel)
            AdminTab.CONTENT -> AdminContentTab(state = state, viewModel = viewModel)
            AdminTab.SETTINGS -> AdminSettingsTab()
        }
    }
}

@Composable
fun AdminDashboardTab(state: PlayerUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        // Hero Admin Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(AppTheme.fg(0.05f), RoundedCornerShape(20.dp))
                    .border(1.dp, AppTheme.fg(0.08f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AdminBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AdminBlue, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Admin Control Console", color = AdminBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Platform Overview", color = AppTheme.fg(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${state.trackSubmissions.count { it.status == "pending" }} submissions pending · ${state.mockUsers.size} active users", color = AppTheme.fg(0.5f), fontSize = 12.sp)
                    }
                }
            }
        }

        // Metrics Grid
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("System Health & Metrics", color = AppTheme.fg(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiBox("5,210", "Total Users", "+42 today", AdminBlue, Modifier.weight(1f))
                    AdminKpiBox("12,840", "Total Tracks", "+18 pending", Color(0xFF7B2FBE), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiBox("84.2K", "Daily Streams", "+12% vs yday", Color(0xFF16A085), Modifier.weight(1f))
                    AdminKpiBox("389", "Active Artists", "+7 this week", Color(0xFFE74C3C), Modifier.weight(1f))
                }
            }
        }

        // User Feedback Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("User Feedback & Tickets", color = AppTheme.fg(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        items(state.feedbackTickets) { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tagColor = when (f.type) {
                    "bug" -> Color(0xFFE53935)
                    "praise" -> Color(0xFF4CAF50)
                    else -> Color(0xFFFFB300)
                }
                Box(
                    modifier = Modifier
                        .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(f.type.uppercase(), color = tagColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(f.msg, color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("${f.user} · ${f.timeAgo}", color = AppTheme.fg(0.4f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(
    state: PlayerUiState,
    viewModel: PlayerViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = state.mockUsers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("User Management", color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Manage roles, accounts, and system access", color = AppTheme.fg(0.5f), fontSize = 13.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // Search Box
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = AppTheme.fg(), fontSize = 14.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(AdminBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.fg(0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = AppTheme.fg(0.4f), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) Text("Search by name or email...", color = AppTheme.fg(0.35f), fontSize = 13.sp)
                                inner()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        items(filteredUsers) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(AppTheme.fg(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${user.email} · Joined ${user.joinedDate}", color = AppTheme.fg(0.45f), fontSize = 11.sp)
                }

                // Role badge + switch
                Box(
                    modifier = Modifier
                        .clickable { viewModel.toggleUserRole(user.id) }
                        .background(
                            when (user.role) {
                                UserRole.ARTIST -> Color(0xFF7B2FBE).copy(alpha = 0.2f)
                                UserRole.ADMIN -> AdminBlue.copy(alpha = 0.2f)
                                UserRole.LISTENER -> AppTheme.fg(0.1f)
                            },
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        user.role.name,
                        color = when (user.role) {
                            UserRole.ARTIST -> Color(0xFF7B2FBE)
                            UserRole.ADMIN -> AdminBlue
                            UserRole.LISTENER -> AppTheme.fg(0.7f)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.deleteUser(user.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AdminContentTab(
    state: PlayerUiState,
    viewModel: PlayerViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Content Moderation", color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Approve or reject artist music and album releases", color = AppTheme.fg(0.5f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        items(state.trackSubmissions) { sub ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(DarkCardBg, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sub.title, color = AppTheme.fg(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${sub.artist} · ${sub.album} · ${sub.genre}", color = AppTheme.fg(0.6f), fontSize = 12.sp)
                        Text("Submitted on ${sub.submittedDate}", color = AppTheme.fg(0.35f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }

                    val statusColor = when (sub.status) {
                        "approved" -> Color(0xFF4CAF50)
                        "rejected" -> Color(0xFFE53935)
                        else -> Color(0xFFFFB300)
                    }
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(sub.status.uppercase(), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (sub.status == "pending") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.approveTrack(sub.id) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Approve Track", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.rejectTrack(sub.id) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsTab() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var explicitFilter by remember { mutableStateOf(false) }
    var artistAutoApproval by remember { mutableStateOf(false) }
    var maintenanceMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("System Configuration", color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Global settings and platform preferences", color = AppTheme.fg(0.5f), fontSize = 13.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(20.dp))
                        .padding(vertical = 4.dp)
                ) {
                    SettingToggleRow("Push Notifications", "System-wide announcements & alerts", notificationsEnabled) { notificationsEnabled = it }
                    HorizontalDivider(color = AppTheme.fg(0.05f))
                    SettingToggleRow("Explicit Content Filter", "Block explicit language songs across catalog", explicitFilter) { explicitFilter = it }
                    HorizontalDivider(color = AppTheme.fg(0.05f))
                    SettingToggleRow("Artist Auto-Approval", "Instantly approve verified artist uploads", artistAutoApproval) { artistAutoApproval = it }
                    HorizontalDivider(color = AppTheme.fg(0.05f))
                    SettingToggleRow("Maintenance Mode", "Temporarily pause stream ingestion", maintenanceMode) { maintenanceMode = it }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Storage and stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("Server & Storage Status", color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Database: Cloud Firestore (Healthy · 99.98% uptime)", color = AppTheme.fg(0.5f), fontSize = 12.sp)
                    Text("Cached Audio Streams: 2.4 GB in local disk", color = AppTheme.fg(0.5f), fontSize = 12.sp)
                    Text("CDN Gateway: Active (Edge Bombay 1)", color = AppTheme.fg(0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminKpiBox(value: String, label: String, delta: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(DarkCardBg, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(label, color = AppTheme.fg(0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(delta, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppTheme.fg(0.45f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AdminBlue,
                uncheckedThumbColor = AppTheme.fg(0.5f),
                uncheckedTrackColor = AppTheme.fg(0.1f)
            )
        )
    }
}
