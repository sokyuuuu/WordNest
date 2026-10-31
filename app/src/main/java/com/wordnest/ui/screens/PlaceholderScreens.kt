package com.wordnest.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.R
import com.wordnest.data.local.dao.DeckWithStats
import com.wordnest.reminder.ReminderWorker
import com.wordnest.ui.components.PrimaryButton
import com.wordnest.ui.theme.BorderWarm
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.TextPrimary
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.HomeViewModel
import com.wordnest.ui.viewmodel.SettingsViewModel
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    decks: List<DeckWithStats> = emptyList(),
    onOpenDecksTab: (() -> Unit)? = null,
    onOpenStreak: (() -> Unit)? = null
) {
    val eggDay by viewModel.eggDay.collectAsStateWithLifecycle()
    val totalWords = remember(decks) { decks.sumOf { it.wordCount } }
    val learnedWords = remember(decks) { decks.sumOf { it.learnedCount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chào bạn!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFF3E0))
                    .then(
                        if (onOpenStreak != null) Modifier.clickable(onClick = onOpenStreak)
                        else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🥚",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$eggDay ngày ấp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(
                    if (onOpenStreak != null) Modifier.clickable(onClick = onOpenStreak)
                    else Modifier
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ngày ấp trứng",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Text(
                    text = eggDay.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ForestGreen
                )
                Text(
                    text = "ngày học liên tiếp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                if (decks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "${decks.size} Tổ từ • $totalWords từ vựng • Đã thuộc $learnedWords từ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

            if (onOpenDecksTab != null) {
                PrimaryButton(
                    text = "Vào Tổ từ vựng",
                    onClick = onOpenDecksTab
                )
            }
       }
    }
/**
 * Giao diện SettingsScreen từ WordNest, đã sửa lỗi xin quyền thông báo Android 13+
 * và tối ưu hóa trải nghiệm cuộn màn hình.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val autoSuggest by viewModel.autoSuggestEnabled.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.deeplApiKey.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf(savedApiKey) }
    var apiKeySaved by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationDenied by remember { mutableStateOf(false) }

    LaunchedEffect(savedApiKey) {
        apiKeyInput = savedApiKey
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationDenied = !granted
        viewModel.setReminderEnabled(granted)
    }
    fun onReminderToggled(enabled: Boolean) {
        if (!enabled) {
            notificationDenied = false
            viewModel.setReminderEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderWorker.canPostNotifications(context)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationDenied = false
            viewModel.setReminderEnabled(true)
        }
    }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = ForestGreen
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cài đặt",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Nhắc học hằng ngày",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Switch(
                checked = settings.reminderEnabled,
                onCheckedChange = ::onReminderToggled,
                colors = switchColors
            )
        }

        if (notificationDenied) {
            Text(
                text = "Chưa được cấp quyền thông báo. Bạn sẽ không nhận được nhắc học. Hãy bật quyền trong cài đặt của điện thoại.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = settings.reminderEnabled) { showTimePicker = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Giờ nhắc",
                style = MaterialTheme.typography.bodyLarge,
                color = if (settings.reminderEnabled) TextPrimary else TextSecondary
            )
            Text(
                text = "%02d:%02d".format(settings.reminderTime.hour, settings.reminderTime.minute),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (settings.reminderEnabled) ForestGreen else TextSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Chế độ tối",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Switch(
                checked = settings.darkMode,
                onCheckedChange = viewModel::setDarkMode,
                colors = switchColors
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tự động gợi ý khi thêm từ",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Switch(
                checked = autoSuggest,
                onCheckedChange = viewModel::setAutoSuggest,
                colors = switchColors
            )
        }

        HorizontalDivider(color = BorderWarm)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "DeepL API Key (dịch tự động tùy chọn)",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    apiKeySaved = false
                },
                placeholder = { Text("Nhập khóa DeepL (ví dụ: ...:fx)", color = TextSecondary) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = ForestGreen,
                    unfocusedBorderColor = BorderWarm,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (apiKeySaved) "✓ Đã lưu khóa DeepL" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForestGreen
                )
                TextButton(
                    onClick = {
                        viewModel.saveDeeplApiKey(apiKeyInput)
                        apiKeySaved = true
                    }
                ) {
                    Text("Lưu khóa", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initial = settings.reminderTime,
            onConfirm = { time ->
                viewModel.setReminderTime(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("Lưu", color = ForestGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        },
        text = { TimePicker(state = state) }
    )
}

@Composable
fun StatsPlaceholderScreen(decks: List<DeckWithStats>) {
    val totalWords = remember(decks) { decks.sumOf { it.wordCount } }
    val totalBirds = remember(decks) { decks.sumOf { it.learnedCount } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📊", fontSize = 52.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Thống kê học tập",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tổng cộng $totalWords từ trong ${decks.size} Tổ từ • Đã thuộc $totalBirds từ 🦅",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyComingSoonScreen(
    title: String = stringResource(R.string.coming_soon_title),
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🐣", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.coming_soon_title),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.coming_soon_desc),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            PrimaryButton(text = "Quay lại", onClick = onBack)
        }
    }
}
