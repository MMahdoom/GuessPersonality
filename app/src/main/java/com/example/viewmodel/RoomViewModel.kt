package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.model.GameRoom
import com.example.model.ScoringFormula
import com.example.model.UiState
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

class RoomViewModel(
    private val repository: GameRepository,
    val roomCode: String,
    val currentUserId: String
) : ViewModel() {

    val roomState: StateFlow<UiState<GameRoom?>> = repository.observeRoom(roomCode)
        .map<GameRoom?, UiState<GameRoom?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing room $roomCode", rootError)
            emit(UiState.Error(rootError.message ?: "Lost connection to room $roomCode"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun hostFinalizeDeck(room: GameRoom, selectedIds: List<Int>, startPlaying: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.hostFinalizeDeck(room, selectedIds, startPlaying)
            _busy.value = false
            result.onSuccess {
                _actionMessage.value = if (startPlaying) {
                    "Deck locked with ${selectedIds.size} personalities — game started!"
                } else {
                    "Deck finalized with ${selectedIds.size} personalities (Pick 1–${selectedIds.size})."
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to finalize deck."
            }
        }
    }

    fun hostAdvanceRound(room: GameRoom, nextRound: Int, resetCards: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.hostAdvanceRound(room, nextRound, resetCards)
            _busy.value = false
            result.onSuccess {
                _actionMessage.value = "Moved room to Round $nextRound!"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to advance round."
            }
        }
    }

    fun hostFinishMatch(room: GameRoom) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.hostFinishMatch(room)
            _busy.value = false
            result.onSuccess {
                _actionMessage.value = "Match finished! Final standings locked."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to finish match."
            }
        }
    }

    fun hostRecordScore(room: GameRoom, targetUid: String, round: Int, score: Int) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.hostRecordRoundScore(room, targetUid, round, score)
            _busy.value = false
            result.onSuccess {
                val playerName = room.players[targetUid]?.nickname ?: "Player"
                _actionMessage.value = "Recorded Round $round score ($score pts) for $playerName."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to record score."
            }
        }
    }

    fun pickCard(room: GameRoom, cardNumber: Int) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.playerPickCard(room, cardNumber)
            _busy.value = false
            result.onSuccess {
                _actionMessage.value = "Card #$cardNumber selected & hidden. Hold phone to forehead, then tap DISPLAY!"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not pick card #$cardNumber."
            }
        }
    }

    fun setCardVisible(room: GameRoom, visible: Boolean) {
        viewModelScope.launch {
            val result = repository.playerSetCardVisibility(room, visible)
            result.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not toggle display mode."
            }
        }
    }

    fun incrementQuestion(room: GameRoom) {
        val me = room.players[currentUserId] ?: return
        val currentRoundQ = me.roundQuestions.toInt()
        val currentTotalQ = me.totalQuestions.toInt()
        if (currentTotalQ >= ScoringFormula.MAX_TOTAL_QUESTIONS) {
            _actionMessage.value = "Maximum 15 questions reached! Mark guess result."
            return
        }
        if (currentRoundQ >= ScoringFormula.MAX_QUESTIONS_PER_ROUND) {
            _actionMessage.value = "5 questions reached for this round! Start next question block or mark guess."
            return
        }
        viewModelScope.launch {
            repository.playerUpdateQuestions(
                room = room,
                newRoundQuestions = currentRoundQ + 1,
                newTotalQuestions = currentTotalQ + 1
            ).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not update question count."
            }
        }
    }

    fun decrementQuestion(room: GameRoom) {
        val me = room.players[currentUserId] ?: return
        val currentRoundQ = me.roundQuestions.toInt()
        val currentTotalQ = me.totalQuestions.toInt()
        if (currentTotalQ <= 0) return
        viewModelScope.launch {
            repository.playerUpdateQuestions(
                room = room,
                newRoundQuestions = (currentRoundQ - 1).coerceAtLeast(0),
                newTotalQuestions = (currentTotalQ - 1).coerceAtLeast(0)
            ).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not undo question."
            }
        }
    }

    fun resetRoundQuestionCounter(room: GameRoom) {
        val me = room.players[currentUserId] ?: return
        viewModelScope.launch {
            repository.playerUpdateQuestions(
                room = room,
                newRoundQuestions = 0,
                newTotalQuestions = me.totalQuestions.toInt()
            ).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not reset round counter."
            }
        }
    }

    fun completeGuess(room: GameRoom, guessedCorrectly: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val result = repository.playerCompleteGuess(room, guessedCorrectly)
            _busy.value = false
            result.onSuccess { score ->
                _actionMessage.value = if (guessedCorrectly) {
                    "Awesome guess! Scored $score pts (15 - questions asked)."
                } else {
                    "Marked unguessed (0 pts recorded for Round ${room.currentRound})."
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not record guess."
            }
        }
    }

    fun leaveRoom(onLeft: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            repository.leaveRoom(roomCode)
            _busy.value = false
            onLeft()
        }
    }

    private companion object {
        const val TAG = "RoomVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
