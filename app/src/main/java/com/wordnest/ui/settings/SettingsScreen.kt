package com.wordnest.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordnest.viewmodel.SettingsViewModel
import java.time.LocalTime

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showTimePicker by remember { mutableStateOf(false) }

    fun isNotificationGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // Quyền có thể được cấp/từ chối từ ngoài app (Cài đặt hệ thống), nên tính lại
    // trạng thái mỗi lần quay lại foreground thay vì chỉ dựa vào kết quả dialog (F17).
    var notificationGranted by remember { mutableStateOf(isNotificationGranted()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationGranted = granted }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationGranted = isNotificationGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onReminderToggled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.setReminderEnabled(enabled)
    }

    // Chỉ cảnh báo khi nhắc đang bật mà thiếu quyền thông báo.
    val showPermissionWarning = settings.reminderEnabled && !notificationGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .animateContentSize(), // chuyển tiếp mượt khi cảnh báo quyền xuất hiện/biến mất
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Cài đặt", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Nhắc học hằng ngày", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = settings.reminderEnabled, onCheckedChange = ::onReminderToggled)
        }

        AnimatedVisibility(
            visible = showPermissionWarning,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Text(
                text = "Chưa được cấp quyền thông báo. Bạn sẽ không nhận được nhắc học. Hãy bật quyền trong cài đặt của điện thoại.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = settings.reminderEnabled) { showTimePicker = true }
                // Gợi ý thị giác: hàng giờ nhắc mờ đi khi nhắc học đang tắt (TC17-05)
                .alpha(if (settings.reminderEnabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Giờ nhắc", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "%02d:%02d".format(settings.reminderTime.hour, settings.reminderTime.minute),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Chế độ tối", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = settings.darkMode, onCheckedChange = viewModel::setDarkMode)
        }
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initial = settings.reminderTime,
            onConfirm = { time ->
                viewModel.setReminderTime(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
        text = { TimePicker(state = state) },
    )
}
