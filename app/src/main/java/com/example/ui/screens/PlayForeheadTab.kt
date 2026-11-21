package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.model.GameRoom
import com.example.model.Personality
import com.example.model.RoomPlayer
import com.example.model.RoomStatus
import com.example.model.ScoringFormula
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpotlightGold
import com.example.ui.theme.StageCrimson
import com.example.ui.theme.StageDeepIndigo
import com.example.ui.theme.StageTeal
import com.example.ui.theme.StageVelvetSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayForeheadTab(
    room: GameRoom,
    currentUserId: String,
    personalityLookup: (Int) -> Personality?,
    busy: Boolean,
    onPickCard: (Int) -> Unit,
    onSetCardVisible: (Boolean) -> Unit,
    onIncrementQuestion: () -> Unit,
    onDecrementQuestion: () -> Unit,
    onResetRoundQuestionBlock: () -> Unit,
    onCompleteGuess: (Boolean) -> Unit
) {
    val myPlayer = room.players[currentUserId] ?: RoomPlayer()
    val maxCards = room.maxSelectableNumber
    val pickedNumber = myPlayer.selectedCardNumber.toInt()
    val assignedPersonalityId = room.personalityIdForCard(pickedNumber)
    val assignedPersonality = assignedPersonalityId?.let { personalityLookup(it) }
    val isFinished = room.roomStatus == RoomStatus.FINISHED

    // Keep screen awake while displaying personality on forehead
    val context = LocalContext.current
    DisposableEffect(myPlayer.cardVisible) {
        val window = (context as? Activity)?.window
        if (myPlayer.cardVisible) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val takenNumbersMap = remember(room.players, currentUserId) {
        room.players.entries
            .filter { it.value.selectedCardNumber > 0L && it.key != currentUserId }
            .associate { it.value.selectedCardNumber.toInt() to it.value.nickname }
    }

    val livePotentialScore = ScoringFormula.calculateScore(
        questionsAsked = myPlayer.totalQuestions.toInt(),
        guessedCorrectly = true
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. FOREHEAD STAGE CARD (HIDDEN BY DEFAULT -> DISPLAY -> HIDE / LOCK)
            item {
                if (pickedNumber <= 0 || assignedPersonality == null) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = StageDeepIndigo,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SpotlightGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pick a Secret Number (1–$maxCards)",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your assigned personality stays HIDDEN after picking so you can safely raise the phone to your forehead before tapping DISPLAY.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD6CBE6),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (!myPlayer.cardVisible) {
                    // HIDDEN / LOCKED FOREHEAD SHIELD
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StageDeepIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forehead_hidden_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = SpotlightGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SpotlightGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = SpotlightGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CARD #$pickedNumber • HIDDEN",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SpotlightGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(StageVelvetSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$pickedNumber",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    color = SpotlightGold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Place Phone on Forehead",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hold screen facing outward toward friends, then tap DISPLAY below to reveal your personality!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD6CBE6),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = { onSetCardVisible(true) },
                                enabled = !busy && !isFinished,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpotlightGold,
                                    contentColor = StageDeepIndigo
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .testTag("display_personality_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.display_button),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                } else {
                    // REVEALED FOREHEAD DISPLAY MODE (HIGH CONTRAST FOR ACROSS-THE-ROOM READABILITY)
                    ForeheadRevealedCard(
                        cardNumber = pickedNumber,
                        personality = assignedPersonality,
                        busy = busy,
                        isFinished = isFinished,
                        onHideLock = { onSetCardVisible(false) }
                    )
                }
            }

            // 2. 3 ROUNDS / 15 QUESTIONS MAX CONTROL PANEL
            if (pickedNumber > 0) {
                item {
                    QuestionTrackerCard(
                        currentRound = room.currentRound.toInt(),
                        player = myPlayer,
                        potentialScore = livePotentialScore,
                        busy = busy,
                        isFinished = isFinished,
                        onIncrement = onIncrementQuestion,
                        onDecrement = onDecrementQuestion,
                        onResetRoundBlock = onResetRoundQuestionBlock,
                        onMarkGuessed = { onCompleteGuess(true) },
                        onMarkUnguessed = { onCompleteGuess(false) }
                    )
                }
            }

            // 3. SECRET NUMBER PICKER (STRICTLY 1 TO N = FINALIZED DECK SIZE)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pick Card Number (1–$maxCards)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (room.deckFinalized) {
                                        "Max number matches finalized deck ($maxCards personalities)."
                                    } else {
                                        "Deck preview ($maxCards cards). Host can finalize in Lobby."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val available = (1..maxCards).filter { it !in takenNumbersMap }
                                    val pool = if (available.isNotEmpty()) available else (1..maxCards).toList()
                                    pool.randomOrNull()?.let { onPickCard(it) }
                                },
                                enabled = !busy && !isFinished && maxCards > 0,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("random_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Random")
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (num in 1..maxCards) {
                                val isMine = num == pickedNumber
                                val takenBy = takenNumbersMap[num]
                                NumberChipTile(
                                    number = num,
                                    isSelectedByMe = isMine,
                                    takenByNickname = takenBy,
                                    enabled = !busy && !isFinished,
                                    onClick = { onPickCard(num) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForeheadRevealedCard(
    cardNumber: Int,
    personality: Personality,
    busy: Boolean,
    isFinished: Boolean,
    onHideLock: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = StageDeepIndigo,
            contentColor = Color.White
        ),
        border = BorderStroke(2.dp, SpotlightGold),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("forehead_revealed_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Portrait photo with seamless illustrated Fallback Clue Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(StageVelvetSurface),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = personality.imageUrl,
                    contentDescription = stringResource(R.string.cd_personality_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        FallbackClueCardArt(personality = personality)
                    },
                    error = {
                        FallbackClueCardArt(personality = personality)
                    }
                )

                // Subtle bottom readability gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    StageDeepIndigo.copy(alpha = 0.94f)
                                )
                            )
                        )
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = SpotlightGold,
                        contentColor = StageDeepIndigo,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "CARD #$cardNumber",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        color = StageDeepIndigo.copy(alpha = 0.85f),
                        contentColor = StageTeal,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, StageTeal.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = personality.region.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = personality.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("revealed_personality_name")
                )
                if (personality.tamilName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = personality.tamilName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SpotlightGold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClueChip(text = personality.category)
                    ClueChip(text = personality.era)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onHideLock,
                    enabled = !busy && !isFinished,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("hide_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.hide_lock_button),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackClueCardArt(personality: Personality) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_clue_card_bg),
            contentDescription = stringResource(R.string.cd_clue_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = StageDeepIndigo.copy(alpha = 0.88f),
                border = BorderStroke(3.dp, SpotlightGold),
                modifier = Modifier.size(104.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = personality.initials,
                        style = MaterialTheme.typography.displayMedium,
                        color = SpotlightGold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CLUE CARD • ${personality.category.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFDE68A)
            )
        }
    }
}

@Composable
private fun ClueChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFEBE2F7),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun QuestionTrackerCard(
    currentRound: Int,
    player: RoomPlayer,
    potentialScore: Int,
    busy: Boolean,
    isFinished: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onResetRoundBlock: () -> Unit,
    onMarkGuessed: () -> Unit,
    onMarkUnguessed: () -> Unit
) {
    val rQ = player.roundQuestions.toInt()
    val tQ = player.totalQuestions.toInt()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Round $currentRound of 3 • Yes/No Tracker",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Formula: Score = max(0, 15 - Questions Asked)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$potentialScore pts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Round Questions (0..5)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("This Round Questions", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "$rQ / 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("round_question_counter")
                    )
                }
                LinearProgressIndicator(
                    progress = { (rQ / 5f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            // Total Questions (0..15)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Match Questions", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "$tQ / 15",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("total_question_counter")
                    )
                }
                LinearProgressIndicator(
                    progress = { (tQ / 15f).coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            if (player.guessCompleted) {
                Surface(
                    color = if (player.guessedCorrectly) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (player.guessedCorrectly) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (player.guessedCorrectly) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val recorded = if (player.guessedCorrectly) potentialScore else 0
                        Text(
                            text = if (player.guessedCorrectly) {
                                "Guess complete in $tQ questions! Recorded $recorded pts for Round $currentRound."
                            } else {
                                "Marked unguessed — 0 pts recorded for Round $currentRound."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Question +/- Stepper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecrement,
                        enabled = !busy && !isFinished && tQ > 0,
                        modifier = Modifier
                            .weight(0.75f)
                            .height(52.dp)
                            .testTag("decrement_question_button")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Undo question")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo")
                    }

                    Button(
                        onClick = onIncrement,
                        enabled = !busy && !isFinished && rQ < 5 && tQ < 15,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .testTag("increment_question_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add question")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+1 Yes/No Question")
                    }
                }

                if (rQ >= 5 && tQ < 15) {
                    OutlinedButton(
                        onClick = onResetRoundBlock,
                        enabled = !busy && !isFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("next_question_block_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Next 5-Question Round Block ($tQ/15 total)")
                    }
                }

                // Mark Guess Complete Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkUnguessed,
                        enabled = !busy && !isFinished,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("mark_unguessed_button")
                    ) {
                        Text("Unguessed (0)")
                    }
                    Button(
                        onClick = onMarkGuessed,
                        enabled = !busy && !isFinished,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("mark_guessed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guessed! (+$potentialScore)")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberChipTile(
    number: Int,
    isSelectedByMe: Boolean,
    takenByNickname: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelectedByMe -> MaterialTheme.colorScheme.primary
        takenByNickname != null -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val contentColor = when {
        isSelectedByMe -> MaterialTheme.colorScheme.onPrimary
        takenByNickname != null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        isSelectedByMe -> MaterialTheme.colorScheme.primary
        takenByNickname != null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(if (isSelectedByMe) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("card_number_$number")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$number",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (takenByNickname != null && !isSelectedByMe) {
                Text(
                    text = takenByNickname.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp
                )
            }
        }
    }
}
