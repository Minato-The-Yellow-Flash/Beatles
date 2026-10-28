package com.beatles.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.beatles.player.data.UpdateInfo
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import kotlinx.coroutines.launch

private val AccentRed = Color(0xFFF82C38)

@Composable
fun AppUpdateDialog(
    update: UpdateInfo,
    progress: Float?,
    status: String? = null,
    isDark: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)

    // Ensure edge-to-edge on dialog window by traversing view parents to DialogWindowProvider
    DisposableEffect(view) {
        var currentParent = view.parent
        while (currentParent != null) {
            if (currentParent is DialogWindowProvider) {
                val dialogWindow = currentParent.window
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                dialogWindow.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                dialogWindow.setBackgroundDrawableResource(android.R.color.transparent)
                dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
                dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
                break
            }
            currentParent = currentParent.parent
        }
        onDispose {}
    }

    // Fluid iOS-style center spring entrance animation
    val enterScale = remember { Animatable(0.88f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { enterScale.animateTo(1f, spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)) }
        launch { enterAlpha.animateTo(1f, tween(200)) }
    }

    // Moving animations to engage user
    val infiniteTransition = rememberInfiniteTransition(label = "updateBadgeMotion")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val iconFloatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconFloatY"
    )

    // Dynamic shimmer sweep across the CTA button
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val laterInteraction = remember { MutableInteractionSource() }
    val updateInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = {
            Haptics.tick(view)
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    Haptics.tick(view)
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            val dialogShape = RoundedCornerShape(32.dp)
            val translucentBarBg = if (isDark) Color(0xFF14151B).copy(alpha = 0.88f) else Color(0xFFFFFFFF).copy(alpha = 0.92f)
            val translucentBarBorder = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.12f)

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .graphicsLayer {
                        scaleX = enterScale.value
                        scaleY = enterScale.value
                        alpha = enterAlpha.value
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = dialogShape,
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.20f)
                    )
                    .clip(dialogShape)
                    .background(translucentBarBg)
                    .border(
                        width = 1.dp,
                        color = translucentBarBorder,
                        shape = dialogShape
                    )
                    .glassRefraction(32.dp, isDark = isDark)
                    .padding(horizontal = 22.dp, vertical = 22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop tap dismiss */ },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Moving glowing pulse update badge
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer expanding glowing aura ring
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .scale(pulseScale)
                            .graphicsLayer { alpha = pulseAlpha }
                            .background(AccentRed.copy(alpha = 0.40f), CircleShape)
                    )

                    // Frosted inner icon container with floating bob
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .graphicsLayer { translationY = iconFloatY }
                            .shadow(6.dp, CircleShape, spotColor = AccentRed.copy(alpha = 0.45f))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AccentRed.copy(alpha = 0.25f), AccentRed.copy(alpha = 0.10f))
                                )
                            )
                            .border(1.5.dp, AccentRed.copy(alpha = 0.50f), CircleShape)
                            .glassRefraction(29.dp, isDark = isDark, isCircle = true),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "New Beatles Update",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                // Version badge pill
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentRed.copy(alpha = if (isDark) 0.18f else 0.12f))
                        .border(1.dp, AccentRed.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        "Version ${update.versionName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed
                    )
                }

                if (update.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.06f)
                                else Color.Black.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = update.releaseNotes,
                            color = textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (progress != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = AccentRed,
                            trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            if (!status.isNullOrBlank()) "Downloading... ${(progress * 100).toInt()}% • $status"
                            else "Downloading update... ${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Later" Pill Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.10f)
                                    else Color.Black.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = translucentBarBorder,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .glassRefraction(24.dp, isDark = isDark)
                                .liquidPress(laterInteraction)
                                .clickable(
                                    interactionSource = laterInteraction,
                                    indication = null
                                ) {
                                    Haptics.tick(view)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Later",
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // "Update Now" Shiny Shimmer CTA Button
                        Box(
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = AccentRed.copy(alpha = 0.50f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .background(AccentRed)
                                .drawWithContent {
                                    drawContent()
                                    // Animated liquid sweep sheen across the button
                                    val sweepWidth = size.width * 0.55f
                                    val startX = (size.width + sweepWidth * 2f) * shimmerTranslate - sweepWidth
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.0f to Color.Transparent,
                                            0.5f to Color.White.copy(alpha = 0.30f),
                                            1.0f to Color.Transparent,
                                            startX = startX,
                                            endX = startX + sweepWidth
                                        )
                                    )
                                }
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .glassRefraction(24.dp, isDark = true)
                                .liquidPress(updateInteraction)
                                .clickable(
                                    interactionSource = updateInteraction,
                                    indication = null
                                ) {
                                    Haptics.confirm(view)
                                    onInstall()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Update Now",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
