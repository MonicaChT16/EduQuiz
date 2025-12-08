package com.eduquiz.feature.auth.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.feature.auth.R
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.feature.auth.presentation.AuthViewModel

@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onGoogleSignInResult(result.data)
        } else {
            viewModel.onGoogleSignInCanceled()
        }
    }

    LoginScreen(
        state = state,
        onGoogleSignInClick = {
            viewModel.launchGoogleSignIn { intent ->
                launcher.launch(intent)
            }
        },
        modifier = modifier
    )
}

@Composable
fun LoginScreen(
    state: AuthState,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deepBlue = Color(0xFF0D47A1)
    val skyBlue = Color(0xFF64B5F6)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(deepBlue, Color(0xFF1565C0))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset((-60).dp, (-70).dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(220.dp, 520.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 14.dp,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(skyBlue, deepBlue)
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Bienvenido a EduQuiz",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = deepBlue
                                )
                                Text(
                                    text = "Autentícate para guardar tu progreso en la nube",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF607D8B)
                                )
                            }
                        }

                        AuthFeaturesSection(deepBlue = deepBlue)

                        if (state is AuthState.Error) {
                            Surface(
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    color = Color(0xFFD32F2F),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state !is AuthState.Loading,
                            onClick = onGoogleSignInClick,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFB0BEC5)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = deepBlue,
                                disabledContainerColor = Color.White.copy(alpha = 0.8f),
                                disabledContentColor = deepBlue.copy(alpha = 0.6f)
                            )
                        ) {
                            if (state is AuthState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = deepBlue
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continuar con Google",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        Text(
                            text = "¿Haz olvidado la contraseña?",
                            color = deepBlue,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeaturesSection(deepBlue: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE3F2FD),
        border = BorderStroke(1.dp, Color(0xFFBBDEFB))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuthFeatureItem(
                icon = Icons.Rounded.Verified,
                text = "Rutas guiadas",
                tint = deepBlue
            )
            AuthFeatureItem(
                icon = Icons.Rounded.Bolt,
                text = "Retos rápidos",
                tint = deepBlue
            )
            AuthFeatureItem(
                icon = Icons.Rounded.School,
                text = "Aprendizaje visual y ordenado",
                tint = deepBlue
            )
        }
    }
}

@Composable
private fun AuthFeatureItem(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    LoginScreen(state = AuthState.Unauthenticated, onGoogleSignInClick = {})
}
