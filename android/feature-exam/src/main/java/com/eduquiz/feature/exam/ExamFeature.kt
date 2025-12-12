package com.eduquiz.feature.exam

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.eduquiz.feature.exam.ExamResult
import com.eduquiz.feature.exam.QuestionReview

// Objeto compartido para pasar la materia desde HomeScreen
object ExamNavigationHelper {
    @Volatile
    var pendingSubject: String? = null
        private set
    
    fun setPendingSubject(subject: String?) {
        pendingSubject = subject
    }
    
    fun getAndClearPendingSubject(): String? {
        val subject = pendingSubject
        pendingSubject = null
        return subject
    }
}

@Composable
fun ExamFeature(
    uid: String,
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    viewModel: ExamViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // Inicializar solo una vez cuando cambia el uid
    LaunchedEffect(uid) { 
        android.util.Log.d("ExamFeature", "LaunchedEffect triggered with uid: $uid")
        viewModel.initialize(uid) 
    }
    
    // Verificar si hay una materia pendiente para iniciar el examen automáticamente
    LaunchedEffect(state.stage, state.pack) {
        if (state.stage == ExamStage.Start && state.pack != null) {
            val pendingSubject = ExamNavigationHelper.getAndClearPendingSubject()
            if (pendingSubject != null) {
                android.util.Log.d("ExamFeature", "Auto-starting exam with subject: $pendingSubject")
                viewModel.startExam(pendingSubject)
            }
        }
    }

    when (state.stage) {
        ExamStage.Loading -> LoadingScreen(modifier)
        ExamStage.Start -> ExamStartScreen(
            state = state,
            modifier = modifier,
            viewModel = viewModel
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
            attemptId = state.attemptId,
            onExit = onExit,
            modifier = modifier,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ExamStartScreen(
    state: ExamUiState,
    onStart: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ExamViewModel = hiltViewModel()
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Simulacro PISA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "10 preguntas, 20 minutos. Las capturas se bloquearán y salir de la app dos veces anula el intento.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Card del Pack Activo
            if (state.pack != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.pack.weekLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "ID: ${state.pack.packId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Preguntas disponibles: ${state.totalQuestions}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Card del Pack Disponible (si no hay pack activo)
            if (state.pack == null && state.availablePack != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Pack disponible: ${state.availablePack.weekLabel}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "ID: ${state.availablePack.packId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Indicador de carga
            if (state.isLoadingPack || state.isDownloading || state.isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Mensaje de error
            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Botones de acción
            if (state.pack == null) {
                // Si no hay pack activo, mostrar botones de descarga
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.downloadPack() },
                        enabled = state.availablePack != null && !state.isDownloading && !state.isLoadingPack,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(text = "Descargar Pack")
                    }
                    TextButton(
                        onClick = { viewModel.refreshAvailablePack() },
                        enabled = !state.isDownloading && !state.isLoadingPack
                    ) {
                        Text(text = "Refrescar")
                    }
                }
            } else {
                // Si hay pack activo, mostrar botones de materias
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecciona una materia:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    SubjectButton(
                        subject = com.eduquiz.domain.pack.Subject.MATEMATICA,
                        onClick = { 
                            android.util.Log.d("ExamFeature", "Matemática button clicked")
                            viewModel.startExam(com.eduquiz.domain.pack.Subject.MATEMATICA) 
                        },
                        enabled = !state.isBusy && state.totalQuestions > 0,
                        isLoading = state.isBusy
                    )
                    
                    SubjectButton(
                        subject = com.eduquiz.domain.pack.Subject.COMPRENSION_LECTORA,
                        onClick = { 
                            android.util.Log.d("ExamFeature", "Comprensión lectora button clicked")
                            viewModel.startExam(com.eduquiz.domain.pack.Subject.COMPRENSION_LECTORA) 
                        },
                        enabled = !state.isBusy && state.totalQuestions > 0,
                        isLoading = state.isBusy
                    )
                    
                    SubjectButton(
                        subject = com.eduquiz.domain.pack.Subject.CIENCIAS,
                        onClick = { 
                            android.util.Log.d("ExamFeature", "Ciencias button clicked")
                            viewModel.startExam(com.eduquiz.domain.pack.Subject.CIENCIAS) 
                        },
                        enabled = !state.isBusy && state.totalQuestions > 0,
                        isLoading = state.isBusy
                    )
                }
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
    attemptId: String?,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val resultState by viewModel.resultState.collectAsStateWithLifecycle()
    val reviewData by viewModel.reviewData.collectAsStateWithLifecycle()
    var showReview by remember { mutableStateOf(false) }
    
    LaunchedEffect(attemptId) {
        if (attemptId != null) {
            viewModel.loadResult(attemptId)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        if (showReview && reviewData.isNotEmpty()) {
            ExamReviewScreen(
                reviewData = reviewData,
                onBack = { showReview = false },
                onExit = onExit
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (resultState == null) {
                    CircularProgressIndicator()
                } else {
                    val result = resultState!!
                    Text(text = "Resultados", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Estado: ${result.status}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Puntaje: ${result.scoreRaw}${if (result.totalQuestions > 0) " / ${result.totalQuestions}" else ""}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preguntas respondidas: ${result.answeredCount}${if (result.totalQuestions > 0) " / ${result.totalQuestions}" else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = { showReview = true }) {
                            Text(text = "Ver Revisión")
                        }
                        OutlinedButton(onClick = onExit) {
                            Text(text = "Volver")
                        }
                    }
                }
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
private fun SubjectButton(
    subject: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = com.eduquiz.domain.pack.Subject.getDisplayName(subject),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
    }
}

@Composable
private fun WarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Entendido")
            }
        },
        title = { Text(text = "Advertencia") },
        text = {
            Text(text = "Si sales una vez más, el examen se enviará automáticamente.")
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

@Composable
private fun ExamReviewScreen(
    reviewData: List<QuestionReview>,
    onBack: () -> Unit,
    onExit: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Revisión del Examen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBack) {
                        Text("Atrás")
                    }
                    Button(onClick = onExit) {
                        Text("Salir")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lista de preguntas
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(reviewData) { index, review ->
                    QuestionReviewCard(
                        questionNumber = index + 1,
                        review = review
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(
    questionNumber: Int,
    review: QuestionReview
) {
    val isCorrect = review.userAnswer?.isCorrect == true
    val userSelectedOption = review.options.find { it.optionId == review.userAnswer?.selectedOptionId }
    val correctOption = review.options.find { it.optionId == review.correctOptionId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Número y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pregunta $questionNumber",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isCorrect) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCorrect) "✓ Correcta" else "✗ Incorrecta",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCorrect) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onError
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Texto de lectura
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = review.text.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = review.text.body,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
            
            // Pregunta
            Text(
                text = review.question.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Opciones
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                review.options.forEach { option ->
                    val isUserAnswer = option.optionId == review.userAnswer?.selectedOptionId
                    val isCorrectAnswer = option.optionId == review.correctOptionId
                    
                    val backgroundColor = when {
                        isCorrectAnswer && isUserAnswer -> MaterialTheme.colorScheme.primaryContainer
                        isCorrectAnswer -> MaterialTheme.colorScheme.primaryContainer
                        isUserAnswer -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    
                    val borderColor = when {
                        isCorrectAnswer -> MaterialTheme.colorScheme.primary
                        isUserAnswer -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (isUserAnswer) {
                                Text(
                                    text = "Tu respuesta",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isCorrectAnswer && !isUserAnswer) {
                                Text(
                                    text = "Correcta",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // Explicación
            val explanationText = review.question.explanationText
            if (!explanationText.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Explicación:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = explanationText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
