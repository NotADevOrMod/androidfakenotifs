package com.example.fakenotifs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

object Pal {
    val Primary = Color(0xFF4F46E5)
    val PrimaryDark = Color(0xFF3730A3)
    val PrimaryLight = Color(0xFFE0E7FF)
    val Secondary = Color(0xFF0EA5E9)
    val SecondaryDark = Color(0xFF0369A1)
    val Red = Color(0xFFEF4444)
    val RedDark = Color(0xFFB91C1C)
    val Amber = Color(0xFFF59E0B)
    val AmberDark = Color(0xFFB45309)
    val Text = Color(0xFF1F2937)
    val Muted = Color(0xFF9CA3AF)
    val Border = Color(0xFFE5E7EB)
    val Snow = Color(0xFFF9FAFB)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pal.Primary,
            onPrimary = Color.White,
            primaryContainer = Pal.PrimaryLight,
            onPrimaryContainer = Pal.PrimaryDark,
            secondary = Pal.Secondary,
            secondaryContainer = Pal.PrimaryLight,
            background = Color.White,
            surface = Color.White,
            onSurface = Pal.Text,
            surfaceVariant = Pal.Snow,
            outline = Pal.Border
        ),
        content = content
    )
}

fun modelOf(src: Any?): Any? = if (src is String) File(src) else src

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: Color = Pal.Primary,
    textColor: Color = Color.White,
    compact: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier
            .height(if (compact) 40.dp else 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) face else Pal.Border)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) textColor else Pal.Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 13.sp else 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Pal.Border, RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun Pill(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Pal.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = Pal.SecondaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun Avatar(appName: String, title: String, iconSrc: Any?, accent: Int, size: Dp) {
    if (iconSrc != null) {
        AsyncImage(
            model = modelOf(iconSrc),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape)
        )
    } else {
        val letter = appName.ifBlank { title }.trim().take(1).uppercase().ifBlank { "?" }
        Box(Modifier.size(size).clip(CircleShape).background(Color(accent)), contentAlignment = Alignment.Center) {
            Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp)
        }
    }
}

@Composable
fun NotificationPreview(
    appName: String,
    title: String,
    message: String,
    iconSrc: Any?,
    imageSrc: Any?,
    accent: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Pal.Snow)
            .border(1.dp, Pal.Border, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(Color(accent)))
            Spacer(Modifier.width(8.dp))
            Text(
                appName.ifBlank { "App name" },
                color = Pal.Muted, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text("now", color = Pal.Muted, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Avatar(appName, title, iconSrc, accent, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title.ifBlank { "Title" },
                    color = Pal.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    message.ifBlank { "Message" },
                    color = Color(0xFF6B7280), fontSize = 14.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (imageSrc != null) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = modelOf(imageSrc),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
            )
        }
    }
}
