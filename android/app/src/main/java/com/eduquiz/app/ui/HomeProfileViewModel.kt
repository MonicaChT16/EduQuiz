package com.eduquiz.app.ui

import androidx.lifecycle.ViewModel
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.UserProfile
import com.eduquiz.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@HiltViewModel
class HomeProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profile: Flow<UserProfile?> = authRepository.authState.flatMapLatest { user ->
        if (user != null) {
            profileRepository.observeProfile(user.uid)
        } else {
            flowOf(null)
        }
    }
}
