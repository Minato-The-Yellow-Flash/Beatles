package com.beatles.player.ui.screens
import com.beatles.player.ui.theme.AppTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatles.player.R
import com.beatles.player.data.ThemeMode
import com.beatles.player.data.UserRole
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

private val DarkBg = Color(0xFF151515)
private val ListenerRed = Color(0xFFF82C38)
private val ArtistPurple = Color(0xFF7B2FBE)
private val AdminBlue = Color(0xFF2471A3)

@Composable
fun ProfileScreen(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    var nameText by remember { mutableStateOf(state.userName.ifBlank { "User" }) }
    var showRoleModal by remember { mutableStateOf<UserRole?>(null) }
    var passcodeText by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf(false) }

    val currentRoleColor = when (state.userRole) {
        UserRole.LISTENER -> ListenerRed
        UserRole.ARTIST -> ArtistPurple
        UserRole.ADMIN -> AdminBlue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AppTheme.card)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppTheme.fg(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Image(
                    painter = painterResource(id = if (com.beatles.player.ui.theme.AppTheme.isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                    contentDescription = "Beatles",
                    modifier = Modifier.size(36.dp)
                )

                Button(
                    onClick = {
                        if (nameText.isNotBlank()) {
                            viewModel.setUserName(nameText.trim())
                        }
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) Color.White else Color(0xFF141416),
                        contentColor = if (AppTheme.isDark) Color(0xFF141416) else Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text("Save", color = if (AppTheme.isDark) Color(0xFF141416) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            val profileScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(profileScrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Hero Profile Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.fg(0.05f), RoundedCornerShape(24.dp))
                        .border(1.dp, AppTheme.fg(0.08f), RoundedCornerShape(24.dp))
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(88.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppTheme.fg(0.12f), CircleShape)
                                .border(2.dp, AppTheme.fg(0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = AppTheme.fg(0.8f),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(currentRoleColor, CircleShape)
                                .border(2.dp, AppTheme.bg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (state.userRole) {
                                    UserRole.LISTENER -> Icons.Default.MusicNote
                                    UserRole.ARTIST -> Icons.Default.Star
                                    UserRole.ADMIN -> Icons.Default.Security
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .background(currentRoleColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            state.userRole.name,
                            color = currentRoleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        textStyle = TextStyle(
                            color = AppTheme.fg(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(ListenerRed),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        decorationBox = { innerTextField ->
                            if (nameText.isEmpty()) {
                                Text("Your name", color = AppTheme.fg(0.3f), textAlign = TextAlign.Center)
                            }
                            innerTextField()
                        }
                    )
                    Text("Tap to edit name", color = AppTheme.fg(0.35f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Theme Selection (Systemwide / Dark / Light)
                Text(
                    "APP THEME",
                    color = AppTheme.fg(0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(AppTheme.fg(0.05f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val themes = listOf(
                        Triple(ThemeMode.SYSTEM, "System", Icons.Default.BrightnessAuto),
                        Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode),
                        Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode)
                    )
                    themes.forEach { (mode, label, icon) ->
                        val isSelected = state.themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AppTheme.fg(0.18f) else Color.Transparent)
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else AppTheme.fg(0.5f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else AppTheme.fg(0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Switch Role / Account Section
                Text(
                    "SWITCH ACCOUNT ROLE",
                    color = AppTheme.fg(0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.fg(0.05f), RoundedCornerShape(20.dp))
                        .border(1.dp, AppTheme.fg(0.08f), RoundedCornerShape(20.dp))
                ) {
                    RoleOptionRow(
                        title = "Listener",
                        subtitle = "Stream music & create custom albums/playlists",
                        role = UserRole.LISTENER,
                        icon = Icons.Default.Headphones,
                        color = ListenerRed,
                        isSelected = state.userRole == UserRole.LISTENER,
                        onClick = {
                            viewModel.setUserRole(UserRole.LISTENER)
                            onClose()
                        }
                    )

                    HorizontalDivider(color = AppTheme.fg(0.06f))

                    RoleOptionRow(
                        title = "Artist",
                        subtitle = "Upload songs & albums, view analytics, chat with fans",
                        role = UserRole.ARTIST,
                        icon = Icons.Default.Mic,
                        color = ArtistPurple,
                        isSelected = state.userRole == UserRole.ARTIST,
                        onClick = {
                            if (state.userRole == UserRole.ARTIST) {
                                onClose()
                            } else {
                                showRoleModal = UserRole.ARTIST
                            }
                        }
                    )

                    HorizontalDivider(color = AppTheme.fg(0.06f))

                    RoleOptionRow(
                        title = "Administrator",
                        subtitle = "Manage users, review song content & configure system",
                        role = UserRole.ADMIN,
                        icon = Icons.Default.AdminPanelSettings,
                        color = AdminBlue,
                        isSelected = state.userRole == UserRole.ADMIN,
                        onClick = {
                            if (state.userRole == UserRole.ADMIN) {
                                onClose()
                            } else {
                                showRoleModal = UserRole.ADMIN
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Updates Section
                val context = LocalContext.current
                val installedVersion = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.2"
                    } catch (_: Exception) {
                        "1.2"
                    }
                }
                Text(
                    "APP UPDATES",
                    color = AppTheme.fg(0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.fg(0.05f), RoundedCornerShape(20.dp))
                        .border(1.dp, AppTheme.fg(0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Beatles Version", color = AppTheme.fg(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (state.isCheckingUpdate) "Checking GitHub for updates..."
                                    else if (state.availableUpdate != null) "New version v${state.availableUpdate.versionName} available!"
                                    else "v$installedVersion (Installed)",
                                    color = if (state.availableUpdate != null) ListenerRed else AppTheme.fg(0.55f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            if (state.availableUpdate != null) {
                                Button(
                                    onClick = { viewModel.downloadAndInstallUpdate(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ListenerRed),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    enabled = state.updateDownloadProgress == null
                                ) {
                                    if (state.updateDownloadProgress != null) {
                                        Text("${(state.updateDownloadProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Update", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.checkForUpdates(silent = false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.fg(0.12f)),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    enabled = !state.isCheckingUpdate
                                ) {
                                    if (state.isCheckingUpdate) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AppTheme.fg(), strokeWidth = 2.dp)
                                    } else {
                                        Text("Check", color = AppTheme.fg(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        if (state.updateNotice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.updateNotice!!, color = Color(0xFF2ECC71), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (state.updateErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.updateErrorMessage!!, color = Color(0xFFE74C3C), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign out button
                Button(
                    onClick = {
                        viewModel.logout()
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.fg(0.08f)
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ListenerRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out / Reset Mode", color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Crafted by team Beatles (Aashirvad, Tarun, Yash)",
                    color = AppTheme.fg(0.40f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp)
                )
            }
        }

        // Role Passcode Modal
        if (showRoleModal != null) {
            val targetRole = showRoleModal!!
            val modalColor = if (targetRole == UserRole.ARTIST) ArtistPurple else AdminBlue
            val requiredPasscode = if (targetRole == UserRole.ARTIST) "artist123" else "admin123"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showRoleModal = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppTheme.sheet)
                        .border(1.dp, AppTheme.fg(0.1f), RoundedCornerShape(24.dp))
                        .clickable(enabled = false) {}
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(modalColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (targetRole == UserRole.ARTIST) Icons.Default.Mic else Icons.Default.Security,
                            contentDescription = null,
                            tint = modalColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "${targetRole.name} Access",
                        color = AppTheme.fg(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enter passcode to switch into ${targetRole.name.lowercase()} mode (hint: ${requiredPasscode})",
                        color = AppTheme.fg(0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    BasicTextField(
                        value = passcodeText,
                        onValueChange = {
                            passcodeText = it
                            passcodeError = false
                        },
                        textStyle = TextStyle(color = AppTheme.fg(), fontSize = 15.sp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.08f), RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (passcodeError) Color.Red else AppTheme.fg(0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        decorationBox = { inner ->
                            if (passcodeText.isEmpty()) {
                                Text("Passcode (${requiredPasscode})", color = AppTheme.fg(0.3f), fontSize = 14.sp)
                            }
                            inner()
                        }
                    )

                    if (passcodeError) {
                        Text("Incorrect passcode. Try '$requiredPasscode'", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                showRoleModal = null
                                passcodeText = ""
                                passcodeError = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.fg(0.1f)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Cancel", color = AppTheme.fg(), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (passcodeText.trim().lowercase() == requiredPasscode || passcodeText.isBlank()) {
                                    viewModel.setUserRole(targetRole)
                                    showRoleModal = null
                                    passcodeText = ""
                                    onClose()
                                } else {
                                    passcodeError = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = modalColor),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Login", color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleOptionRow(
    title: String,
    subtitle: String,
    role: UserRole,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(subtitle, color = AppTheme.fg(0.45f), fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTheme.fg(0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}
