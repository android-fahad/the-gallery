package com.polylab.thegallery.ui.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class MviViewModel<Intent : UiIntent, State : UiState, Effect : UiEffect>(
    initialState: State
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    protected val currentState: State get() = _state.value

    protected fun setState(reducer: State.() -> State) {
        val newState = currentState.reducer()
        _state.value = newState
    }

    protected suspend fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        _effect.emit(effectValue)
    }

    abstract suspend fun handleIntent(intent: Intent)
}

interface UiIntent
interface UiState
interface UiEffect