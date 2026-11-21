package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.model.GameRoom
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LobbyViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    defaultDisplayName: String
) : ViewModel() {

    val myRoomsState: StateFlow<UiState<List<GameRoom>>> = repository.observeMyRooms(currentUserId)
        .map<List<GameRoom>, UiState<List<GameRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing rooms", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load your rooms"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val fallbackNickname: String = defaultDisplayName.trim().take(30).ifBlank { "Player" }

    fun createRoom(
        roomName: String,
        nickname: String,
        initialDeckIds: List<Int>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _statusMessage.value = null
            val result = repository.createRoom(
                roomName = roomName,
                hostNickname = nickname.ifBlank { fallbackNickname },
                initialDeckIds = initialDeckIds
            )
            _busy.value = false
            result.onSuccess { code ->
                onCreated(code)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to create room."
            }
        }
    }

    fun joinRoom(
        roomCode: String,
        nickname: String,
        onJoined: (String) -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _statusMessage.value = null
            val result = repository.joinRoom(
                roomCodeInput = roomCode,
                nickname = nickname.ifBlank { fallbackNickname }
            )
            _busy.value = false
            result.onSuccess { code ->
                onJoined(code)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not join room."
            }
        }
    }

    fun deleteRoom(roomCode: String) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.deleteRoom(roomCode)
            _busy.value = false
            result.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not delete room."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val TAG = "LobbyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
