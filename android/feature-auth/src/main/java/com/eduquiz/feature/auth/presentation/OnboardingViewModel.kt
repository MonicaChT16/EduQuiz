package com.eduquiz.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.data.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < TOTAL_STEPS - 1) {
            _currentStep.update { it + 1 }
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.update { it - 1 }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.markOnboardingAsCompleted()
        }
    }

    companion object {
        const val TOTAL_STEPS = 3
    }
}
