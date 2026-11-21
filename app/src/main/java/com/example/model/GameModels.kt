package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class RoomStatus {
    LOBBY,
    PLAYING,
    FINISHED;

    companion object {
        fun fromRaw(raw: String?): RoomStatus =
            entries.firstOrNull { it.name == raw } ?: LOBBY
    }
}

object ScoringFormula {
    const val MAX_ROUNDS = 3
    const val MAX_QUESTIONS_PER_ROUND = 5
    const val MAX_TOTAL_QUESTIONS = 15

    /**
     * Exact scoring formula:
     * Score = 15 - (number of questions asked), clamped at 0 minimum if
     * questions reach/exceed 15 or the personality was not guessed.
     */
    fun calculateScore(questionsAsked: Int, guessedCorrectly: Boolean): Int {
        if (!guessedCorrectly) return 0
        return (MAX_TOTAL_QUESTIONS - questionsAsked).coerceIn(0, MAX_TOTAL_QUESTIONS)
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val gamesPlayed: Long = 0L,
    val totalPoints: Long = 0L,
    val bestRoundScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomPlayer(
    val nickname: String = "",
    val selectedCardNumber: Long = 0L,
    val cardVisible: Boolean = false,
    val roundQuestions: Long = 0L,
    val totalQuestions: Long = 0L,
    val guessCompleted: Boolean = false,
    val guessedCorrectly: Boolean = false,
    val round1Score: Long = 0L,
    val round2Score: Long = 0L,
    val round3Score: Long = 0L,
    val totalScore: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "nickname" to nickname,
        "selectedCardNumber" to selectedCardNumber,
        "cardVisible" to cardVisible,
        "roundQuestions" to roundQuestions,
        "totalQuestions" to totalQuestions,
        "guessCompleted" to guessCompleted,
        "guessedCorrectly" to guessedCorrectly,
        "round1Score" to round1Score,
        "round2Score" to round2Score,
        "round3Score" to round3Score,
        "totalScore" to (round1Score + round2Score + round3Score)
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): RoomPlayer {
            if (map == null) return RoomPlayer()
            val r1 = (map["round1Score"] as? Number)?.toLong() ?: 0L
            val r2 = (map["round2Score"] as? Number)?.toLong() ?: 0L
            val r3 = (map["round3Score"] as? Number)?.toLong() ?: 0L
            return RoomPlayer(
                nickname = map["nickname"] as? String ?: "Player",
                selectedCardNumber = (map["selectedCardNumber"] as? Number)?.toLong() ?: 0L,
                cardVisible = map["cardVisible"] as? Boolean ?: false,
                roundQuestions = (map["roundQuestions"] as? Number)?.toLong() ?: 0L,
                totalQuestions = (map["totalQuestions"] as? Number)?.toLong() ?: 0L,
                guessCompleted = map["guessCompleted"] as? Boolean ?: false,
                guessedCorrectly = map["guessedCorrectly"] as? Boolean ?: false,
                round1Score = r1,
                round2Score = r2,
                round3Score = r3,
                totalScore = (map["totalScore"] as? Number)?.toLong() ?: (r1 + r2 + r3)
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val roomName: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val status: String = RoomStatus.LOBBY.name,
    val currentRound: Long = 1L,
    val deckIds: List<Long> = emptyList(),
    val deckFinalized: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val players: Map<String, RoomPlayer> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val roomStatus: RoomStatus
        get() = RoomStatus.fromRaw(status)

    val maxSelectableNumber: Int
        get() = deckIds.size

    fun personalityIdForCard(cardNumber: Int): Int? {
        if (cardNumber < 1 || cardNumber > deckIds.size) return null
        return deckIds[cardNumber - 1].toInt()
    }
}
