package com.eduquiz.data.repository

import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.OnboardingPreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingRepository @Inject constructor(
    private val database: AppDatabase
) {
    val hasCompletedOnboarding: Flow<Boolean> = database.onboardingDao()
        .observeOnboardingPreferences()
        .map { it?.hasCompletedOnboarding ?: false }

    suspend fun markOnboardingAsCompleted() {
        database.onboardingDao().upsertOnboardingPreferences(
            OnboardingPreferencesEntity(
                id = 1,
                hasCompletedOnboarding = true
            )
        )
    }

    suspend fun getOnboardingStatus(): Boolean {
        return database.onboardingDao().getOnboardingPreferences()?.hasCompletedOnboarding ?: false
    }
}
