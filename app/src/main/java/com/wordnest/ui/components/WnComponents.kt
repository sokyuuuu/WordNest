/*
 * Các thành phần dùng chung, dựng theo design Figma + style guide.
 * Màn hình nào cần dùng token/điều khiển giống nhau thì gom về đây để
 * mỗi màn chỉ giữ riêng logic của nó.
 */
package com.wordnest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Thẻ trắng viền kem bo góc 16 — bề mặt chính của mọi màn (không đổ bóng
 * theo phong cách phẳng của Figma).
 */
@Composable
fun WnCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
        content = content,
    )
}

/**
 * Tiêu đề màn hình: nút quay lại ô vuông 48dp vàng bo 16 + tiêu đề 24 Bold,
 * có thể kèm hành động bên phải (trailing).
 */
@Composable
fun WnHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Tiêu đề nhóm kiểu "• Nhắc học" trong Cài đặt. */
@Composable
fun WnSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Thanh tiến độ: track kem - viền nhạt, fill xanh bo tròn hoàn toàn.
 * [progress] trong khoảng 0..1.
 */
@Composable
fun WnProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

/**
 * Ô thống kê nhỏ: số to (ExtraBold) + nhãn phụ; [icon] hiển thị cạnh số
 * (✓ / ↺ / 🥚 ...) tô xanh theo Figma.
 */
@Composable
fun WnStatCard(
    value: String,
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Dải kết quả có dấu ✓ trong vòng tròn xanh — dùng cho màn kết quả
 * buổi học và kết quả bài kiểm tra.
 */
@Composable
fun WnResultBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Nút chính VÀNG (#FFC857) chữ nâu đậm — dùng cho mọi hành động khuyến khích. */
@Composable
fun WnPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Nút phụ: nền trắng viền kem, chữ nâu — hành động nhẹ hơn nút chính. */
@Composable
fun WnGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Nút "Đã thuộc" xanh lá chữ trắng. */
@Composable
fun WnSuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Liên kết chữ như "Về trang chủ" / "Xem tất cả". */
@Composable
fun WnTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.tertiary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

/** Trạng thái hiển thị của một đáp án trắc nghiệm. */
enum class WnOptionState { Normal, Correct, Wrong }

/**
 * Ô đáp án trắc nghiệm: mặc định trắng viền kem; khi chọn đúng → nền xanh
 * chữ trắng kèm ✓; khi chọn sai → nền đỏ chữ trắng kèm ⊗.
 */
@Composable
fun WnOptionButton(
    text: String,
    state: WnOptionState,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val container = when (state) {
        WnOptionState.Normal -> MaterialTheme.colorScheme.surface
        WnOptionState.Correct -> MaterialTheme.colorScheme.secondary
        WnOptionState.Wrong -> MaterialTheme.colorScheme.error
    }
    val contentColor = when (state) {
        WnOptionState.Normal -> MaterialTheme.colorScheme.onBackground
        WnOptionState.Correct -> MaterialTheme.colorScheme.onSecondary
        WnOptionState.Wrong -> MaterialTheme.colorScheme.onError
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(
                if (state == WnOptionState.Normal) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled && state == WnOptionState.Normal, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        when (state) {
            WnOptionState.Correct -> Text(
                text = "✓",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
            WnOptionState.Wrong -> Text(
                text = "⊗",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
            WnOptionState.Normal -> Unit
        }
    }
}
