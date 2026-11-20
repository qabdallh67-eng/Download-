package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PlatformType
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldRich
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SkyCanvasBottom
import com.example.ui.theme.SkyCanvasTop
import com.example.ui.theme.SkyHeaderEnd
import com.example.ui.theme.SkyHeaderStart
import com.example.ui.theme.SkySurface
import com.example.ui.theme.VioletBright

@Composable
fun CrownPlayLogo(
    size: Dp = 44.dp,
    showCrown: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showCrown) {
            Canvas(
                modifier = Modifier
                    .size(size * 0.56f)
                    .align(Alignment.TopCenter)
                    .offset(y = (-size * 0.14f))
            ) {
                val w = this.size.width
                val h = this.size.height
                val crownPath = Path().apply {
                    moveTo(w * 0.12f, h * 0.85f)
                    lineTo(w * 0.05f, h * 0.30f)
                    lineTo(w * 0.32f, h * 0.56f)
                    lineTo(w * 0.50f, h * 0.10f)
                    lineTo(w * 0.68f, h * 0.56f)
                    lineTo(w * 0.95f, h * 0.30f)
                    lineTo(w * 0.88f, h * 0.85f)
                    close()
                }
                drawPath(
                    path = crownPath,
                    brush = Brush.linearGradient(listOf(GoldBright, GoldRich, GoldDeep))
                )
                drawPath(
                    path = crownPath,
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(size * 0.78f)
                .align(Alignment.BottomCenter)
                .shadow(8.dp, CircleShape, ambientColor = ElectricBlue, spotColor = CyanGlow)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(CyanGlow, ElectricBlue, SkyHeaderEnd)
                    )
                )
                .border(1.5.dp, GoldBright.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Abdullah Abu Qaoud Logo",
                tint = Color.White,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
fun PortraitAvatar(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(14.dp, CircleShape, ambientColor = ElectricBlue, spotColor = GoldBright)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(GoldBright, CyanGlow, Color.White, ElectricBlue, GoldRich, GoldBright)
                )
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(SkyCanvasTop),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.abdullah_portrait),
            contentDescription = "Abdullah Abu Qaoud Portrait",
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = ElectricBlue.copy(alpha = 0.35f),
                spotColor = CyanGlow.copy(alpha = 0.35f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(listOf(SkyHeaderStart, ElectricBlue, SkyHeaderEnd))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                ElectricBlue.copy(alpha = 0.35f),
                                SkyHeaderEnd.copy(alpha = 0.35f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (enabled) 0.45f else 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    glowBorder: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == NavyVoid
    val bgColor = if (isDark) NavyCard.copy(alpha = 0.88f) else SkySurface.copy(alpha = 0.96f)
    val borderBrush = if (glowBorder) {
        Brush.linearGradient(
            listOf(
                CyanGlow.copy(alpha = if (isDark) 0.50f else 0.65f),
                ElectricBlue.copy(alpha = if (isDark) 0.30f else 0.45f),
                VioletBright.copy(alpha = if (isDark) 0.45f else 0.45f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                if (isDark) ElectricBlue.copy(alpha = 0.28f) else SkyBorder.copy(alpha = 0.75f),
                if (isDark) RoyalPurple.copy(alpha = 0.22f) else SkyBorder.copy(alpha = 0.45f)
            )
        )
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 6.dp else 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = ElectricBlue.copy(alpha = 0.22f),
                spotColor = CyanGlow.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        border = BorderStroke(1.3.dp, borderBrush),
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun PlatformIconBadge(
    platform: PlatformType,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val drawableRes: Int? = when (platform) {
        PlatformType.FACEBOOK -> R.drawable.img_platform_facebook
        PlatformType.YOUTUBE -> R.drawable.img_platform_youtube
        PlatformType.TIKTOK -> R.drawable.img_platform_tiktok
        PlatformType.INSTAGRAM -> R.drawable.img_platform_instagram
        PlatformType.TELEGRAM -> R.drawable.img_platform_telegram
        PlatformType.WHATSAPP -> R.drawable.img_platform_whatsapp
        PlatformType.X -> R.drawable.img_platform_x
        PlatformType.VIMEO -> R.drawable.img_platform_vimeo
        PlatformType.DIRECT_URL -> R.drawable.img_platform_direct
        PlatformType.OTHER -> null
    }

    val shape = RoundedCornerShape(14.dp)
    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = platform.displayNameEn,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .shadow(4.dp, shape, ambientColor = platform.brandColor)
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(ElectricBlue, VioletBright)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.45f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Public,
                contentDescription = platform.displayNameEn,
                tint = Color.White,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun BrandSplashScreen(
    isArabic: Boolean,
    darkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val bgBrush = if (darkMode) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF132D6B), NavyDeep, NavyVoid),
            center = Offset(500f, 500f),
            radius = 1200f
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF3FAFF), SkyCanvasTop, SkyCanvasBottom)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_home_hero_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (darkMode) 0.18f else 0.42f,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(ElectricBlue.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
                PortraitAvatar(size = 132.dp)
                CrownPlayLogo(
                    size = 46.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "عبدالله أبو قاعود",
                style = MaterialTheme.typography.displaySmall,
                color = if (darkMode) GoldBright else GoldRich,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Abdullah Abu Qaoud",
                style = MaterialTheme.typography.titleLarge,
                color = if (darkMode) Color.White else LightTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = ElectricBlue.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f))
            ) {
                Text(
                    text = if (isArabic) "محمّل الفيديو والصوت الاحترافي" else "Professional Video & Audio Downloader",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (darkMode) CyanGlow else ElectricBlue,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}
