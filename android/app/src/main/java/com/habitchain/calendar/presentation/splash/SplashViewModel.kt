package com.habitchain.calendar.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitchain.calendar.core.config.GameConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplashUiState(val isReady: Boolean = false)

class SplashViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()
    private val _finished = MutableStateFlow(false)
    val finished = _finished.asStateFlow()
    private var delivered = false
    init {
        viewModelScope.launch {
            delay(GameConfig.LOADER_DURATION_MS)
            _uiState.update { it.copy(isReady = true) }
            _finished.value = true
        }
    }
    fun takeCompletion(): Boolean {
        if (!_finished.value || delivered) return false
        delivered = true
        return true
    }
}
