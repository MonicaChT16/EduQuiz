package com.eduquiz.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.store.Cosmetic
import com.eduquiz.domain.store.CosmeticCategory
import com.eduquiz.domain.store.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoreUiState(
    val catalog: List<Cosmetic> = emptyList(),
    val filteredCatalog: List<Cosmetic> = emptyList(),
    val selectedCategory: CosmeticCategory = CosmeticCategory.ALL,
    val purchasedCosmetics: Set<String> = emptySet(),
    val selectedCosmeticId: String? = null,
    val currentCoins: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    private var currentUid: String? = null

    fun initialize(uid: String) {
        if (currentUid == uid) return
        currentUid = uid
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val catalog = storeRepository.getCatalog()
                val inventory = profileRepository.getInventory(uid)
                val purchasedIds = inventory.map { it.cosmeticId }.toSet()
                val profile = profileRepository.observeProfile(uid).firstOrNull()
                
                _state.update {
                    it.copy(
                        catalog = catalog,
                        filteredCatalog = catalog,
                        purchasedCosmetics = purchasedIds,
                        selectedCosmeticId = profile?.selectedCosmeticId,
                        currentCoins = profile?.coins ?: 0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar la tienda"
                    )
                }
            }
        }
    }

    fun selectCategory(category: CosmeticCategory) {
        _state.update { currentState ->
            val filtered = if (category == CosmeticCategory.ALL) {
                currentState.catalog
            } else {
                currentState.catalog.filter { it.category == category }
            }
            currentState.copy(
                selectedCategory = category,
                filteredCatalog = filtered
            )
        }
    }

    fun purchaseCosmetic(cosmeticId: String) {
        val uid = currentUid ?: return
        if (_state.value.purchasedCosmetics.contains(cosmeticId)) {
            _state.update { it.copy(errorMessage = "Ya tienes este cosmético") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val success = storeRepository.purchaseCosmetic(uid, cosmeticId)
                if (success) {
                    // Recargar estado
                    val inventory = profileRepository.getInventory(uid)
                    val purchasedIds = inventory.map { it.cosmeticId }.toSet()
                    val profile = profileRepository.observeProfile(uid).firstOrNull()
                    
                    _state.update {
                        it.copy(
                            purchasedCosmetics = purchasedIds,
                            currentCoins = profile?.coins ?: 0,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No tienes suficientes EduCoins o ya está comprado"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al comprar"
                    )
                }
            }
        }
    }

    fun equipCosmetic(cosmeticId: String) {
        val uid = currentUid ?: return
        if (!_state.value.purchasedCosmetics.contains(cosmeticId)) {
            _state.update { it.copy(errorMessage = "Debes comprar este cosmético primero") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val success = storeRepository.equipCosmetic(uid, cosmeticId)
                if (success) {
                    _state.update {
                        it.copy(
                            selectedCosmeticId = cosmeticId,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error al equipar el cosmético"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al equipar"
                    )
                }
            }
        }
    }
}

