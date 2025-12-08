package com.eduquiz.feature.exam

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.domain.exam.ExamStatus

@Composable
fun ExamFeature(
    uid: String,
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    viewModel: ExamViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(uid) { viewModel.initialize(uid) }

    when (state.stage) {
        ExamStage.Loading -> LoadingScreen(modifier)
        ExamStage.Start -> ExamStartScreen(
            state = state,
            onStart = viewModel::startExam,
            modifier = modifier
        )
        ExamStage.InProgress -> {
            SecureFlagEffect()
            LifecycleLeaveWatcher(onLeave = viewModel::onLeaveApp)
            ExamInProgressScreen(
                state = state,
                onSelectOption = viewModel::selectOption,
                onPrev = viewModel::goToPreviousQuestion,
                onNext = viewModel::goToNextQuestion,
                onSubmit = viewModel::submitManually,
                onDismissWarning = viewModel::dismissWarning,
                modifier = modifier
            )
        }
        ExamStage.Finished -> ExamResultScreen(
            state = state,
            onExit = onExit,
            modifier = modifier
        )
    }
}

@Composable
private fun ExamStartScreen(
    state: ExamUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Simulacro PISA", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "10 preguntas, 20 minutos. Las capturas se bloquearán y salir de la app dos veces anula el intento.",
                style = MaterialTheme.typography.bodyMedium
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.pack?.weekLabel ?: "Sin pack activo",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "ID: ${state.pack?.packId ?: "--"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Preguntas: ${state.totalQuestions.takeIf { it > 0 } ?: "No disponibles"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Button(
                onClick = onStart,
                enabled = state.pack != null && state.totalQuestions > 0 && !state.isBusy
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(text = "Iniciar intento")
            }
        }
    }
}

@Composable
private fun ExamInProgressScreen(
    state: ExamUiState,
    onSelectOption: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onDismissWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = state.questions.getOrNull(state.currentIndex)
    val scrollState = rememberScrollState()
    BackHandler(enabled = true) {
        onSubmit()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExamHeader(
                remainingMs = state.remainingMs,
                durationMs = state.durationMs,
                currentIndex = state.currentIndex,
                totalQuestions = state.totalQuestions
            )
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = current?.text?.title ?: "Texto no encontrado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = current?.text?.body ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            if (current != null) {
                QuestionCard(
                    question = current,
                    selectedOptionId = state.answers[current.question.questionId],
                    locked = state.areOptionsLocked,
                    lockRemainingMs = state.lockRemainingMs,
                    onSelectOption = onSelectOption,
                    onPrev = onPrev,
                    onNext = onNext,
                    onSubmit = onSubmit,
                    isLast = state.currentIndex == state.totalQuestions - 1,
                    isFirst = state.currentIndex == 0
                )
            }
        }
    }

    if (state.showWarningDialog) {
        WarningDialog(onDismiss = onDismissWarning)
    }
}

@Composable
private fun ExamResultScreen(
    state: ExamUiState,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Resultados", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Estado: ${state.finishedStatus ?: ExamStatus.COMPLETED}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Puntaje: ${state.correctCount} / ${state.totalQuestions}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preguntas respondidas: ${state.answeredCount}",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onExit) {
                Text(text = "Volver")
            }
        }
    }
}

@Composable
private fun ExamHeader(
    remainingMs: Long,
    durationMs: Long,
    currentIndex: Int,
    totalQuestions: Int
) {
    val fraction = if (durationMs > 0) {
        1f - (remainingMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tiempo: ${formatMillis(remainingMs)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Pregunta ${currentIndex + 1} / $totalQuestions",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth()
        )

    }
}

@Composable
private fun QuestionCard(
    question: ExamContent,
    selectedOptionId: String?,
    locked: Boolean,
    lockRemainingMs: Long,
    onSelectOption: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    isLast: Boolean,
    isFirst: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = question.question.prompt,
                style = MaterialTheme.typography.titleMedium
            )
            if (locked) {
                Text(
                    text = "Opciones disponibles en ${lockRemainingMs / 1000}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEach { option ->
                    val selected = option.optionId == selectedOptionId
                    val container = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    TextButton(
                        onClick = { onSelectOption(option.optionId) },
                        enabled = !locked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(container, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = option.text,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onPrev, enabled = !isFirst) {
                    Text(text = "Anterior")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSubmit) {
                        Text(text = "Enviar ahora")
                    }
                    Button(onClick = if (isLast) onSubmit else onNext) {
                        Text(text = if (isLast) "Finalizar" else "Siguiente")
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Continuar")
            }
        },
        title = { Text(text = "No salgas de la app") },
        text = {
            Text(text = "Si vuelves a salir se anulará el intento por trampa.")
        }
    )
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SecureFlagEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
private fun LifecycleLeaveWatcher(onLeave: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                onLeave()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
