package com.example.data

import android.content.Context
import com.example.R
import com.example.model.GameRoom
import com.example.model.RoomPlayer
import com.example.model.RoomStatus
import com.example.model.ScoringFormula
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) {
            "User must be signed in with Google before accessing Firestore."
        }
        return uid
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(nickname: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = nickname.trim().take(30).ifBlank { "Player" }
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "nickname" to cleanName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "nickname" to cleanName,
                        "gamesPlayed" to 0L,
                        "totalPoints" to 0L,
                        "bestRoundScore" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchStats(roundScore: Int, matchFinished: Boolean = false): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val snap = docRef.get().await()
            val safeScore = roundScore.coerceIn(0, 15).toLong()
            if (snap.exists()) {
                val prevGames = snap.getLong("gamesPlayed") ?: 0L
                val prevTotal = snap.getLong("totalPoints") ?: 0L
                val prevBest = snap.getLong("bestRoundScore") ?: 0L
                docRef.update(
                    mapOf(
                        "gamesPlayed" to (prevGames + if (matchFinished) 1L else 0L),
                        "totalPoints" to (prevTotal + safeScore),
                        "bestRoundScore" to maxOf(prevBest, safeScore),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ROOM OBSERVATION ---

    fun observeMyRooms(userId: String): Flow<List<GameRoom>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "rooms"
        emitAll(
            db.collection("rooms")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { parseGameRoom(it) }
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRoom(roomCode: String): Flow<GameRoom?> = flow {
        val cleanCode = normalizeRoomCode(roomCode)
        val uid = requireUserId()
        if (cleanCode.length < 4) {
            emit(null)
            return@flow
        }
        val path = "rooms/$cleanCode"
        emitAll(
            db.collection("rooms").document(cleanCode)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null else parseGameRoom(snapshot)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    // --- ROOM LIFECYCLE ---

    suspend fun createRoom(
        roomName: String,
        hostNickname: String,
        initialDeckIds: List<Int>
    ): Result<String> {
        var path = "rooms"
        return try {
            val uid = requireUserId()
            val cleanRoomName = roomName.trim().take(60).ifBlank { "Party Night" }
            val cleanHostName = hostNickname.trim().take(30).ifBlank { "Host" }
            val sanitizedDeck = initialDeckIds
                .filter { it in 1..100 }
                .distinct()
                .ifEmpty { (1..15).toList() }
                .shuffled()
                .map { it.toLong() }

            var roomCode = generateRoomCode()
            var attempts = 0
            while (attempts < 5) {
                val existing = db.collection("rooms").document(roomCode).get().await()
                if (!existing.exists()) break
                roomCode = generateRoomCode()
                attempts++
            }

            val docRef = db.collection("rooms").document(roomCode)
            path = docRef.path

            val hostPlayer = RoomPlayer(nickname = cleanHostName)
            val payload = mapOf(
                "roomCode" to roomCode,
                "roomName" to cleanRoomName,
                "hostId" to uid,
                "hostName" to cleanHostName,
                "status" to RoomStatus.LOBBY.name,
                "currentRound" to 1L,
                "deckIds" to sanitizedDeck,
                "deckFinalized" to false,
                "memberUids" to listOf(uid),
                "players" to mapOf(uid to hostPlayer.toMap()),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            upsertUserProfile(cleanHostName)
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomCodeInput: String, nickname: String): Result<String> {
        val code = normalizeRoomCode(roomCodeInput)
        val path = "rooms/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 4..10) { "Enter a valid room code (4-10 letters/numbers)." }
            val cleanNick = nickname.trim().take(30).ifBlank { "Player" }

            val docRef = db.collection("rooms").document(code)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Room $code was not found."))
            }
            val room = parseGameRoom(snapshot)
                ?: return Result.failure(IllegalStateException("Could not read room $code."))

            if (uid in room.memberUids) {
                val existingPlayer = room.players[uid] ?: RoomPlayer(nickname = cleanNick)
                if (room.roomStatus != RoomStatus.FINISHED && existingPlayer.nickname != cleanNick) {
                    docRef.update(
                        FieldPath.of("players", uid),
                        existingPlayer.copy(nickname = cleanNick).toMap(),
                        FieldPath.of("updatedAt"),
                        FieldValue.serverTimestamp()
                    ).await()
                }
                upsertUserProfile(cleanNick)
                return Result.success(code)
            }

            if (room.roomStatus == RoomStatus.FINISHED) {
                return Result.failure(IllegalStateException("Room $code has already finished."))
            }
            if (room.memberUids.size >= 20) {
                return Result.failure(IllegalStateException("Room $code is full (max 20 players)."))
            }

            val updatedMembers = room.memberUids + uid
            val newPlayer = RoomPlayer(nickname = cleanNick)
            docRef.update(
                FieldPath.of("memberUids"),
                updatedMembers,
                FieldPath.of("players", uid),
                newPlayer.toMap(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            upsertUserProfile(cleanNick)
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomCode: String): Result<Unit> {
        val code = normalizeRoomCode(roomCode)
        val path = "rooms/$code"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("rooms").document(code)
            val snap = docRef.get().await()
            val room = parseGameRoom(snap) ?: return Result.success(Unit)
            if (room.hostId == uid || uid !in room.memberUids || room.roomStatus == RoomStatus.FINISHED) {
                return Result.success(Unit)
            }
            val remainingMembers = room.memberUids.filterNot { it == uid }
            docRef.update(
                FieldPath.of("memberUids"),
                remainingMembers,
                FieldPath.of("players", uid),
                FieldValue.delete(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomCode: String): Result<Unit> {
        val code = normalizeRoomCode(roomCode)
        val path = "rooms/$code"
        return try {
            requireUserId()
            db.collection("rooms").document(code).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- HOST DECK & ROUND CONTROLS ---

    suspend fun hostFinalizeDeck(
        room: GameRoom,
        selectedIds: List<Int>,
        startPlaying: Boolean
    ): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostId == uid) { "Only the host can finalize the personality deck." }
            require(selectedIds.isNotEmpty()) { "Select at least 1 personality for the match." }
            val shuffledDeck = selectedIds
                .filter { it in 1..100 }
                .distinct()
                .shuffled()
                .map { it.toLong() }

            // Reset player picks that exceed the new N limit and ensure all cards start hidden
            val resetPlayers = room.players.mapValues { (_, p) ->
                val adjustedCard = if (p.selectedCardNumber > shuffledDeck.size) 0L else p.selectedCardNumber
                p.copy(
                    selectedCardNumber = if (startPlaying) 0L else adjustedCard,
                    cardVisible = false,
                    roundQuestions = if (startPlaying) 0L else p.roundQuestions,
                    totalQuestions = if (startPlaying) 0L else p.totalQuestions,
                    guessCompleted = if (startPlaying) false else p.guessCompleted,
                    guessedCorrectly = if (startPlaying) false else p.guessedCorrectly
                ).toMap()
            }

            val updates = mapOf(
                "deckIds" to shuffledDeck,
                "deckFinalized" to true,
                "status" to if (startPlaying) RoomStatus.PLAYING.name else room.status,
                "players" to resetPlayers,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(room.roomCode).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun hostAdvanceRound(
        room: GameRoom,
        nextRound: Int,
        resetPlayerCardsForNewRound: Boolean
    ): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostId == uid) { "Only the host can change rounds." }
            val safeRound = nextRound.coerceIn(1, ScoringFormula.MAX_ROUNDS).toLong()

            val updatedPlayers = room.players.mapValues { (_, player) ->
                if (resetPlayerCardsForNewRound) {
                    player.copy(
                        selectedCardNumber = 0L,
                        cardVisible = false,
                        roundQuestions = 0L,
                        totalQuestions = 0L,
                        guessCompleted = false,
                        guessedCorrectly = false
                    ).toMap()
                } else {
                    player.copy(
                        roundQuestions = 0L
                    ).toMap()
                }
            }

            val updates = mapOf(
                "status" to RoomStatus.PLAYING.name,
                "currentRound" to safeRound,
                "players" to updatedPlayers,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(room.roomCode).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun hostFinishMatch(room: GameRoom): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostId == uid) { "Only the host can finish the match." }
            db.collection("rooms").document(room.roomCode).update(
                mapOf(
                    "status" to RoomStatus.FINISHED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val hostScore = room.players[uid]?.totalScore?.toInt() ?: 0
            recordMatchStats(roundScore = hostScore.coerceAtMost(15), matchFinished = true)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun hostRecordRoundScore(
        room: GameRoom,
        targetUid: String,
        round: Int,
        score: Int
    ): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostId == uid) { "Only the host can edit Points Tab scores." }
            val current = room.players[targetUid]
                ?: return Result.failure(IllegalArgumentException("Player not found in room."))
            val clamped = score.coerceIn(0, ScoringFormula.MAX_TOTAL_QUESTIONS).toLong()
            val updated = when (round) {
                1 -> current.copy(round1Score = clamped)
                2 -> current.copy(round2Score = clamped)
                else -> current.copy(round3Score = clamped)
            }
            val finalPlayer = updated.copy(
                totalScore = updated.round1Score + updated.round2Score + updated.round3Score
            )
            db.collection("rooms").document(room.roomCode).update(
                FieldPath.of("players", targetUid),
                finalPlayer.toMap(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PLAYER FOREHEAD & QUESTION ACTIONS ---

    suspend fun playerPickCard(room: GameRoom, cardNumber: Int): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            val maxCard = room.deckIds.size
            require(cardNumber in 1..maxCard) {
                "Card number must be between 1 and $maxCard."
            }
            val current = room.players[uid]
                ?: return Result.failure(IllegalStateException("You are not joined to this room."))
            // Keep personality HIDDEN by default when selecting a number for forehead mode
            val updated = current.copy(
                selectedCardNumber = cardNumber.toLong(),
                cardVisible = false,
                roundQuestions = 0L,
                totalQuestions = 0L,
                guessCompleted = false,
                guessedCorrectly = false
            )
            db.collection("rooms").document(room.roomCode).update(
                FieldPath.of("players", uid),
                updated.toMap(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun playerSetCardVisibility(room: GameRoom, visible: Boolean): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            val current = room.players[uid]
                ?: return Result.failure(IllegalStateException("You are not joined to this room."))
            val updated = current.copy(cardVisible = visible)
            db.collection("rooms").document(room.roomCode).update(
                FieldPath.of("players", uid),
                updated.toMap(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun playerUpdateQuestions(
        room: GameRoom,
        newRoundQuestions: Int,
        newTotalQuestions: Int
    ): Result<Unit> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            val current = room.players[uid]
                ?: return Result.failure(IllegalStateException("You are not joined to this room."))
            val safeRoundQ = newRoundQuestions.coerceIn(0, ScoringFormula.MAX_QUESTIONS_PER_ROUND).toLong()
            val safeTotalQ = newTotalQuestions.coerceIn(0, ScoringFormula.MAX_TOTAL_QUESTIONS).toLong()
            val updated = current.copy(
                roundQuestions = safeRoundQ,
                totalQuestions = safeTotalQ
            )
            db.collection("rooms").document(room.roomCode).update(
                FieldPath.of("players", uid),
                updated.toMap(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun playerCompleteGuess(
        room: GameRoom,
        guessedCorrectly: Boolean
    ): Result<Int> {
        val path = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            val current = room.players[uid]
                ?: return Result.failure(IllegalStateException("You are not joined to this room."))

            val score = ScoringFormula.calculateScore(
                questionsAsked = current.totalQuestions.toInt(),
                guessedCorrectly = guessedCorrectly
            )
            val round = room.currentRound.toInt().coerceIn(1, 3)
            val withRoundScore = when (round) {
                1 -> current.copy(round1Score = score.toLong())
                2 -> current.copy(round2Score = score.toLong())
                else -> current.copy(round3Score = score.toLong())
            }
            val updated = withRoundScore.copy(
                guessCompleted = true,
                guessedCorrectly = guessedCorrectly,
                cardVisible = false,
                totalScore = withRoundScore.round1Score + withRoundScore.round2Score + withRoundScore.round3Score
            )
            db.collection("rooms").document(room.roomCode).update(
                FieldPath.of("players", uid),
                updated.toMap(),
                FieldPath.of("updatedAt"),
                FieldValue.serverTimestamp()
            ).await()
            recordMatchStats(roundScore = score, matchFinished = false)
            Result.success(score)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- HELPERS ---

    @Suppress("UNCHECKED_CAST")
    private fun parseGameRoom(doc: DocumentSnapshot): GameRoom? {
        val data = doc.data ?: return null
        val deckIdsRaw = data["deckIds"] as? List<*> ?: emptyList<Any>()
        val deckIds = deckIdsRaw.mapNotNull { (it as? Number)?.toLong() }

        val memberUidsRaw = data["memberUids"] as? List<*> ?: emptyList<Any>()
        val memberUids = memberUidsRaw.mapNotNull { it as? String }

        val playersRaw = data["players"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val players = buildMap {
            for ((k, v) in playersRaw) {
                val key = k as? String ?: continue
                val map = v as? Map<String, Any?>
                put(key, RoomPlayer.fromMap(map))
            }
        }

        return GameRoom(
            id = doc.id,
            roomCode = data["roomCode"] as? String ?: doc.id,
            roomName = data["roomName"] as? String ?: "Party Room",
            hostId = data["hostId"] as? String ?: "",
            hostName = data["hostName"] as? String ?: "Host",
            status = data["status"] as? String ?: RoomStatus.LOBBY.name,
            currentRound = (data["currentRound"] as? Number)?.toLong() ?: 1L,
            deckIds = deckIds,
            deckFinalized = data["deckFinalized"] as? Boolean ?: false,
            memberUids = memberUids,
            players = players,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    companion object {
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun normalizeRoomCode(raw: String): String =
            raw.trim().uppercase().filter { it.isLetterOrDigit() }.take(10)

        fun generateRoomCode(): String =
            (1..6).map { CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
    }
}
