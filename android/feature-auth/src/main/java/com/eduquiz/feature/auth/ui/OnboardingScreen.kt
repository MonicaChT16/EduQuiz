package com.eduquiz.feature.auth.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.feature.auth.R
import com.eduquiz.feature.auth.presentation.OnboardingViewModel

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()

    OnboardingScreen(
        currentStep = currentStep,
        totalSteps = OnboardingViewModel.TOTAL_STEPS,
        onNextClick = { viewModel.nextStep() },
        onPreviousClick = { viewModel.previousStep() },
        onFinishClick = {
            viewModel.completeOnboarding()
            onNavigateToLogin()
        },
        modifier = modifier
    )
}

@Composable
fun OnboardingScreen(
    currentStep: Int,
    totalSteps: Int,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deepBlue = Color(0xFF0D47A1)
    val lightBlue = Color(0xFF1565C0)
    val white = Color.White

    val onboardingSteps = listOf(
        OnboardingStep(
            title = "EduQuiz",
            description = "Domina las pruebas PISA con simulacros interactivos. Mejora tu comprensión lectora, matemática y científica desde tu celular",
            buttonLabel = "SIGUIENTE"
        ),
        OnboardingStep(
            title = "EduQuiz",
            description = "Recibe feedback inteligente al instante. Nuestra IA te explica cada respuesta para que aprendas de tus errores y mejores día a día",
            buttonLabel = "SIGUIENTE"
        ),
        OnboardingStep(
            title = "EduQuiz",
            description = "Gana EduCoins y destaca en tu aula. Supera retos semanales, personaliza tu perfil y demuestra que estás listo para el futuro",
            buttonLabel = "EMPEZAR"
        )
    )

    val currentOnboardingStep = onboardingSteps[currentStep]

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(deepBlue, lightBlue)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacing
                Spacer(modifier = Modifier.height(40.dp))

                // Title
                Text(
                    text = currentOnboardingStep.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = white,
                    textAlign = TextAlign.Center
                )

                Image(
                    painter = painterResource(id = R.drawable.robot_start),
                    contentDescription = null,
                    modifier = Modifier.size(220.dp)
                )

                // Description
                Text(
                    text = currentOnboardingStep.description,
                    fontSize = 16.sp,
                    color = white,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Progress indicators (dots)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        val isActive = index == currentStep
                        val indicatorColor by animateColorAsState(
                            targetValue = if (isActive) white else white.copy(alpha = 0.3f),
                            label = "indicatorColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 12.dp else 8.dp)
                                .background(indicatorColor, shape = RoundedCornerShape(50))
                        )
                        if (index < totalSteps - 1) {
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            color = white.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((currentStep + 1) / totalSteps.toFloat())
                            .height(4.dp)
                            .background(
                                color = white,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Main button (Next or Start)
                    Button(
                        onClick = {
                            if (currentStep == totalSteps - 1) {
                                onFinishClick()
                            } else {
                                onNextClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = white,
                            contentColor = deepBlue
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = currentOnboardingStep.buttonLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Previous button (only show if not on first step)
                    if (currentStep > 0) {
                        Button(
                            onClick = onPreviousClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = white.copy(alpha = 0.2f),
                                contentColor = white
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "ANTERIOR",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom spacing
                Spacer(modifier = Modifier.height(16.dp))

                // Copyright
                Text(
                    text = "Copyright © 2025",
                    fontSize = 12.sp,
                    color = white.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
data class OnboardingStep(
    val title: String,
    val description: String,
    val buttonLabel: String
)
