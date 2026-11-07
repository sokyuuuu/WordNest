package com.wordnest.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.viewmodel.StudyMode
import com.wordnest.viewmodel.StudyViewModel

/**
 * Màn hình trung tâm của luồng học, điều hướng nội bộ qua [StudyMode]
 * trong khi Navigation giữ route "study".
 */
@Composable
fun StudyFlowScreen(
    viewModel: StudyViewModel,
    onExit: () -> Unit,
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()

    when (mode) {
        StudyMode.FLASHCARD -> FlashcardScreen(viewModel, onExit)
        StudyMode.FLASHCARD_RESULT -> FlashcardResultScreen(viewModel, onHome = onExit)
        StudyMode.QUIZ -> QuizScreen(viewModel)
        StudyMode.QUIZ_RESULT -> QuizResultScreen(viewModel, onHome = onExit)
        StudyMode.TYPE_IN -> TypeInScreen(viewModel)
        StudyMode.STATISTICS -> StatisticsScreen(viewModel)
    }
}
