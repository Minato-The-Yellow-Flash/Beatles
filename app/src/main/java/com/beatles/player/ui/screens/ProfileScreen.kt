package com.beatles.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
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
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

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
    val isDark = isDarkTheme(state.themeMode)

    val currentRoleColor = when (state.userRole) {
        UserRole.LISTENER -> ListenerRed
        UserRole.ARTIST -> ArtistPurple
        UserRole.ADMIN -> AdminBlue
    }

    val pageBg = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
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
                val backInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidPress(backInteraction)
                        .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f))
                        .glassRefraction(20.dp, isDark = isDark, isCircle = true)
                        .clickable(interactionSource = backInteraction, indication = null) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF141416),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Image(
                    painter = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                    contentDescription = "Beatles",
                    modifier = Modifier.size(36.dp)
                )

                val saveInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .liquidPress(saveInteraction)
                        .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color.White else Color(0xFF141416))
                        .glassRefraction(50.dp, isDark = isDark)
                        .clickable(interactionSource = saveInteraction, indication = null) {
                            if (nameText.isNotBlank()) {
                                viewModel.setUserName(nameText.trim())
                            }
                            onClose()
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save", color = if (isDark) Color(0xFF141416) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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

                // Hero Profile Card with liquid glass
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1C1E26).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.75f))
                        .glassRefraction(24.dp, isDark = isDark)
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(88.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(4.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF383842).copy(alpha = 0.40f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                .glassRefraction(44.dp, isDark = isDark, isCircle = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF141416),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(currentRoleColor)
                                .border(
                                    width = 2.dp,
                                    color = if (isDark) Color(0xFF1C1E26) else Color.White,
                                    shape = CircleShape
                                ),
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
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(50), spotColor = currentRoleColor.copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(50))
                            .background(currentRoleColor.copy(alpha = 0.22f))
                            .glassRefraction(50.dp, isDark = isDark)
                            .padding(horizontal = 14.dp, vertical = 5.dp)
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
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(ListenerRed),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        decorationBox = { innerTextField ->
                            if (nameText.isEmpty()) {
                                Text("Your name", color = textMuted, textAlign = TextAlign.Center)
                            }
                            innerTextField()
                        }
                    )
                    Text("Tap to edit name", color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Theme Selection (Systemwide / Dark / Light) with liquid glass
                Text(
                    "APP THEME",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(3.dp, RoundedCornerShape(24.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF181A22).copy(alpha = 0.60f) else Color(0xFFFFFFFF).copy(alpha = 0.70f))
                        .glassRefraction(24.dp, isDark = isDark)
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
                        val themeInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .liquidPress(themeInteraction)
                                .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) (if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFFE2E2E2))
                                    else Color.Transparent
                                )
                                .then(if (isSelected) Modifier.glassRefraction(20.dp, isDark = isDark) else Modifier)
                                .clickable(interactionSource = themeInteraction, indication = null) {
                                    viewModel.setThemeMode(mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) (if (isDark) Color.White else Color(0xFF141416)) else (if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF141416).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    label,
                                    color = if (isSelected) (if (isDark) Color.White else Color(0xFF141416)) else (if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF141416).copy(alpha = 0.5f)),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Switch Role / Account Section (ONE-TAP SWITCH, NO PASSWORD) with liquid glass
                Text(
                    "SWITCH ACCOUNT ROLE",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF181A22).copy(alpha = 0.60f) else Color(0xFFFFFFFF).copy(alpha = 0.70f))
                        .glassRefraction(20.dp, isDark = isDark)
                ) {
                    RoleOptionRow(
                        title = "Listener",
                        subtitle = "Stream music & create custom albums/playlists",
                        role = UserRole.LISTENER,
                        icon = Icons.Default.Headphones,
                        color = ListenerRed,
                        isSelected = state.userRole == UserRole.LISTENER,
                        isDark = isDark,
                        onClick = {
                            viewModel.setUserRole(UserRole.LISTENER)
                            onClose()
                        }
                    )

                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))

                    RoleOptionRow(
                        title = "Artist",
                        subtitle = "Upload songs & albums, view analytics, chat with fans",
                        role = UserRole.ARTIST,
                        icon = Icons.Default.Mic,
                        color = ArtistPurple,
                        isSelected = state.userRole == UserRole.ARTIST,
                        isDark = isDark,
                        onClick = {
                            viewModel.setUserRole(UserRole.ARTIST)
                            onClose()
                        }
                    )

                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))

                    RoleOptionRow(
                        title = "Administrator",
                        subtitle = "Manage users, review song content & configure system",
                        role = UserRole.ADMIN,
                        icon = Icons.Default.AdminPanelSettings,
                        color = AdminBlue,
                        isSelected = state.userRole == UserRole.ADMIN,
                        isDark = isDark,
                        onClick = {
                            viewModel.setUserRole(UserRole.ADMIN)
                            onClose()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Updates Section with liquid glass
                val context = LocalContext.current
                val installedVersion = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.4"
                    } catch (_: Exception) {
                        "1.4"
                    }
                }
                Text(
                    "APP UPDATES",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF181A22).copy(alpha = 0.60f) else Color(0xFFFFFFFF).copy(alpha = 0.70f))
                        .glassRefraction(20.dp, isDark = isDark)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Beatles Version", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (state.isCheckingUpdate) "Checking GitHub for updates..."
                                    else if (state.availableUpdate != null) "New version v${state.availableUpdate.versionName} available!"
                                    else "v$installedVersion (Installed)",
                                    color = if (state.availableUpdate != null) ListenerRed else textSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            val checkBtnInteraction = remember { MutableInteractionSource() }
                            if (state.availableUpdate != null) {
                                Box(
                                    modifier = Modifier
                                        .liquidPress(checkBtnInteraction)
                                        .shadow(3.dp, RoundedCornerShape(50), spotColor = ListenerRed.copy(alpha = 0.45f))
                                        .clip(RoundedCornerShape(50))
                                        .background(ListenerRed)
                                        .glassRefraction(50.dp, isDark = isDark)
                                        .clickable(
                                            interactionSource = checkBtnInteraction,
                                            indication = null,
                                            enabled = state.updateDownloadProgress == null
                                        ) { viewModel.downloadAndInstallUpdate(context) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.updateDownloadProgress != null) {
                                        Text("${(state.updateDownloadProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Update", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .liquidPress(checkBtnInteraction)
                                        .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isDark) Color(0xFF383842).copy(alpha = 0.45f) else Color(0xFFE2E2E2).copy(alpha = 0.65f))
                                        .glassRefraction(50.dp, isDark = isDark)
                                        .clickable(
                                            interactionSource = checkBtnInteraction,
                                            indication = null,
                                            enabled = !state.isCheckingUpdate
                                        ) { viewModel.checkForUpdates(silent = false) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isCheckingUpdate) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = textPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text("Check", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

                // Sign out button with liquid glass
                val signOutInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .liquidPress(signOutInteraction)
                        .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF22242B).copy(alpha = 0.85f) else Color(0xFFE8E9EC))
                        .glassRefraction(50.dp, isDark = isDark)
                        .clickable(interactionSource = signOutInteraction, indication = null) {
                            viewModel.logout()
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ListenerRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out / Reset Mode", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Crafted by team Beatles (Aashirvad, Tarun, Yash)",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp)
                )
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
    isDark: Boolean,
    onClick: () -> Unit
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidPress(rowInteraction)
            .clickable(interactionSource = rowInteraction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = if (isDark) 0.18f else 0.12f))
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = if (isSelected) 0.50f else 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(50), spotColor = color.copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.22f))
                            .glassRefraction(50.dp, isDark = isDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(subtitle, color = textSecondary, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.30f),
            modifier = Modifier.size(18.dp)
        )
    }
}
