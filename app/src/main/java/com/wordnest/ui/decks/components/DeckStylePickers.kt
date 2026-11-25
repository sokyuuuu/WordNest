package com.wordnest.ui.decks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wordnest.data.local.entity.DeckStyle
import com.wordnest.ui.theme.wnDeckColorForKey

fun deckIconVector(icon: String): ImageVector = when (icon) {
    DeckStyle.ICON_BOOK -> Icons.Filled.Book
    DeckStyle.ICON_PLANE, "flight" -> Icons.Filled.Flight
    DeckStyle.ICON_GLOBE, "language" -> Icons.Filled.Language
    DeckStyle.ICON_BULB -> Icons.Filled.Lightbulb
    DeckStyle.ICON_BRIEFCASE, "work" -> Icons.Filled.Work
    "school" -> Icons.Filled.School
    "code" -> Icons.Filled.Code
    "music" -> Icons.Filled.MusicNote
    "favorite" -> Icons.Filled.Favorite
    "star" -> Icons.Filled.Star
    else -> Icons.Filled.Book
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeckStyle.AVAILABLE_COLORS.forEach { colorKey ->
            val selected = colorKey == selectedColor
            val bg = wnDeckColorForKey(colorKey)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape,
                    )
                    .clickable { onColorSelected(colorKey) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Đã chọn màu $colorKey",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
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
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DeckStyle.AVAILABLE_ICONS.forEach { iconKey ->
            val selected = iconKey == selectedIcon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onIconSelected(iconKey) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = deckIconVector(iconKey),
                    contentDescription = iconKey,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
