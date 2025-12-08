package com.eduquiz.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val examRepository: ExamRepository
) : ViewModel() {

    private val _currentUid = MutableStateFlow<String?>(null)
    
    val attempts: StateFlow<List<ExamAttempt>> = 
        _currentUid.asStateFlow()
            .flatMapLatest { uid ->
                if (uid != null) {
                    examRepository.observeAttempts(uid)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    fun initialize(uid: String) {
        if (_currentUid.value != uid) {
            _currentUid.value = uid
        }
    }
}
