package com.wordnest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.data.local.entity.WordStatus
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.DangerRed
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.StatusBirdBg
import com.wordnest.ui.theme.StatusBirdText
import com.wordnest.ui.theme.StatusChickBg
import com.wordnest.ui.theme.StatusChickText
import com.wordnest.ui.theme.StatusEggBg
import com.wordnest.ui.theme.StatusEggText
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.theme.deckColorForKey

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = ForestGreen,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = BorderWarm,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        WordStatus.CHICK -> StatusChickBg to StatusChickText
        WordStatus.BIRD -> StatusBirdBg to StatusBirdText
        else -> StatusEggBg to StatusEggText
    }
    val emoji = WordStatus.emojiFor(status)
    val label = WordStatus.labelVi(status)

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = "$emoji $label",
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Xóa",
    dismissText: String = "Huỷ",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) DangerRed else ForestGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = TextSecondary)
            }
        }
    )
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emoji: String = "🪺",
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryButton(
                text = actionLabel,
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        }
    }
}

fun deckIconVector(icon: String): ImageVector = when (icon) {
    DeckStyle.ICON_BOOK -> Icons.Default.Book
    DeckStyle.ICON_PLANE, "flight" -> Icons.Default.Flight
    DeckStyle.ICON_GLOBE, "language" -> Icons.Default.Language
    DeckStyle.ICON_BULB -> Icons.Default.Lightbulb
    DeckStyle.ICON_BRIEFCASE, "work" -> Icons.Default.Work
    "school" -> Icons.Default.School
    "code" -> Icons.Default.Code
    "music" -> Icons.Default.MusicNote
    "favorite" -> Icons.Default.Favorite
    "star" -> Icons.Default.Star
    else -> Icons.Default.Book
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DeckStyle.AVAILABLE_COLORS.forEach { colorKey ->
            val selected = colorKey == selectedColor
            val bg = deckColorForKey(colorKey)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) ForestGreen else BorderWarm,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorKey) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DeckStyle.AVAILABLE_ICONS.forEach { iconKey ->
            val selected = iconKey == selectedIcon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) ForestGreen.copy(alpha = 0.14f) else CardWarm)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) ForestGreen else BorderWarm,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onIconSelected(iconKey) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deckIconVector(iconKey),
                    contentDescription = iconKey,
                    tint = if (selected) ForestGreen else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
