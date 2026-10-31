package com.wordnest.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.HomeViewModel
import com.wordnest.util.calculateLongestStreak
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// Model & State
// ---------------------------------------------------------------------------

data class StreakUiState(
    val currentStreak: Int,
    val longestStreak: Int,
    val month: YearMonth,
    val completedDays: Set<LocalDate>,
    val today: LocalDate,
    val nextMilestone: Int,
    val milestones: List<Milestone>,
)

data class Milestone(val days: Int, val reached: Boolean)

private val DEFAULT_MILESTONES = listOf(3, 7, 14, 30, 100)
private val FlameColor = Color(0xFFFF9600)

fun createStreakUiState(
    currentStreak: Int,
    longestStreak: Int,
    completedDays: Set<LocalDate>,
    today: LocalDate = LocalDate.now(),
    month: YearMonth = YearMonth.from(today)
): StreakUiState {
    val milestones = DEFAULT_MILESTONES.map { days ->
        Milestone(days = days, reached = currentStreak >= days)
    }
    val nextMilestone = DEFAULT_MILESTONES.firstOrNull { it > currentStreak }
        ?: (currentStreak + 10)

    return StreakUiState(
        currentStreak = currentStreak,
        longestStreak = maxOf(longestStreak, currentStreak),
        month = month,
        completedDays = completedDays,
        today = today,
        nextMilestone = nextMilestone,
        milestones = milestones
    )
}

// ---------------------------------------------------------------------------
// Screens
// ---------------------------------------------------------------------------

@Composable
fun DetailStreakScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStreak by viewModel.eggDay.collectAsStateWithLifecycle()
    val completedDays by viewModel.studyDays.collectAsStateWithLifecycle()
    val today = remember { viewModel.today }
    val longestStreak = remember(completedDays, currentStreak) {
        maxOf(calculateLongestStreak(completedDays, today), currentStreak)
    }

    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }

    val state = remember(currentStreak, longestStreak, completedDays, today, displayedMonth) {
        createStreakUiState(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completedDays = completedDays,
            today = today,
            month = displayedMonth
        )
    }

    DetailStreakScreen(
        state = state,
        onBack = onBack,
        onPrevMonth = { displayedMonth = displayedMonth.minusMonths(1) },
        onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailStreakScreen(
    state: StreakUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chuỗi ngày học", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CreamBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StreakHero(state) }
            item { StatsRow(state) }
            item { StreakCalendar(state, onPrevMonth, onNextMonth) }
            item { MilestoneSection(state.milestones) }
        }
    }
}

// ---------------------------------------------------------------------------
// Hero: ngọn lửa + số ngày + tiến độ tới mốc kế tiếp
// ---------------------------------------------------------------------------

@Composable
private fun StreakHero(data: StreakUiState) {
    val active = data.currentStreak > 0
    val count by animateIntAsState(data.currentStreak, tween(600), label = "streakCount")
    val progress by animateFloatAsState(
        targetValue = (data.currentStreak.toFloat() / data.nextMilestone.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "streakProgress",
    )
    val remaining = (data.nextMilestone - data.currentStreak).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) FlameColor.copy(alpha = 0.16f)
            else CardWhite,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (active) "Chuỗi hiện tại ${data.currentStreak} ngày liên tiếp"
                    else "Chưa có chuỗi nào"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (active) FlameColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(72.dp),
            )
            if (active) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "ngày liên tiếp",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = FlameColor,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Còn $remaining ngày tới mốc ${data.nextMilestone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                Text(
                    text = "Bắt đầu chuỗi mới hôm nay",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stats: hiện tại / dài nhất
// ---------------------------------------------------------------------------

@Composable
private fun StatsRow(data: StreakUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            icon = Icons.Filled.LocalFireDepartment,
            value = data.currentStreak,
            label = "Hiện tại",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Filled.EmojiEvents,
            value = data.longestStreak,
            label = "Dài nhất",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value ngày" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = FlameColor, modifier = Modifier.size(28.dp))
            Column {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Lịch tháng
// ---------------------------------------------------------------------------

private val WeekdayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

@Composable
private fun StreakCalendar(
    data: StreakUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val leading = data.month.atDay(1).dayOfWeek.value - 1 // T2 = 0
    val cells: List<LocalDate?> =
        List(leading) { null } + (1..data.month.lengthOfMonth()).map { data.month.atDay(it) }
    val weeks = cells.chunked(7)
    val canGoNext = data.month < YearMonth.from(data.today)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Tháng trước", tint = TextPrimary)
                }
                Text(
                    text = "Tháng ${data.month.monthValue}, ${data.month.year}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                IconButton(onClick = onNextMonth, enabled = canGoNext) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Tháng sau",
                        tint = if (canGoNext) TextPrimary else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                WeekdayLabels.forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val date = week.getOrNull(col)
                        val completed = date != null && date in data.completedDays
                        val joinLeft = completed && col > 0 && date.minusDays(1) in data.completedDays
                        val next = date?.plusDays(1)
                        val joinRight = completed && col < 6 && next != null &&
                                YearMonth.from(next) == data.month && next in data.completedDays
                        DayCell(
                            date = date,
                            completed = completed,
                            joinLeft = joinLeft,
                            joinRight = joinRight,
                            isToday = date == data.today,
                            isFuture = date != null && date.isAfter(data.today),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    completed: Boolean,
    joinLeft: Boolean,
    joinRight: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(44.dp), contentAlignment = Alignment.Center) {
        if (date == null) return@Box

        // Dải nối các ngày liên tiếp trong cùng một hàng
        if (completed) {
            val r = 18.dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (joinLeft) 0.dp else r,
                            bottomStart = if (joinLeft) 0.dp else r,
                            topEnd = if (joinRight) 0.dp else r,
                            bottomEnd = if (joinRight) 0.dp else r,
                        ),
                    )
                    .background(FlameColor.copy(alpha = 0.22f)),
            )
        }

        val description = buildString {
            append("Ngày ${date.dayOfMonth}")
            if (completed) append(", đã hoàn thành")
            if (isToday) append(", hôm nay")
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    when {
                        completed -> Modifier.background(FlameColor)
                        isToday -> Modifier.border(2.dp, FlameColor, CircleShape)
                        else -> Modifier
                    },
                )
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            if (completed) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFuture) TextSecondary.copy(alpha = 0.38f)
                    else TextPrimary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Mốc thành tích
// ---------------------------------------------------------------------------

@Composable
private fun MilestoneSection(milestones: List<Milestone>) {
    Column {
        Text("Mốc thành tích", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            milestones.forEach { MilestoneChip(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun MilestoneChip(milestone: Milestone, modifier: Modifier = Modifier) {
    val reached = milestone.reached
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (reached) FlameColor.copy(alpha = 0.16f)
                else CardWhite,
            )
            .border(
                width = 1.dp,
                color = if (reached) FlameColor.copy(alpha = 0.4f) else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Mốc ${milestone.days} ngày, " + if (reached) "đã đạt" else "chưa đạt"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = if (reached) FlameColor else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "${milestone.days}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (reached) TextPrimary else TextSecondary,
        )
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DetailStreakPreview() {
    val today = LocalDate.of(2026, 10, 30)
    val completed = (20..30).map { LocalDate.of(2026, 10, it) }.toSet() +
            setOf(3, 4, 5, 9, 10, 14).map { LocalDate.of(2026, 10, it) }
    MaterialTheme {
        DetailStreakScreen(
            state = StreakUiState(
                currentStreak = 11,
                longestStreak = 25,
                month = YearMonth.of(2026, 10),
                completedDays = completed,
                today = today,
                nextMilestone = 14,
                milestones = listOf(
                    Milestone(3, true), Milestone(7, true), Milestone(14, false),
                    Milestone(30, false), Milestone(100, false),
                ),
            ),
            onBack = {},
        )
    }
}
