package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.GameRoom
import com.example.model.RoomPlayer
import com.example.model.RoomStatus
import com.example.model.ScoringFormula
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpotlightGold
import com.example.ui.theme.StageDeepIndigo

@Composable
fun PointsTab(
    room: GameRoom,
    currentUserId: String,
    isHost: Boolean,
    busy: Boolean,
    onHostRecordScore: (targetUid: String, round: Int, score: Int) -> Unit,
    onHostAdvanceRound: (nextRound: Int, resetCards: Boolean) -> Unit,
    onHostFinishMatch: () -> Unit
) {
    val standings = remember(room.players) {
        room.players.entries.sortedWith(
            compareByDescending<Map.Entry<String, RoomPlayer>> { it.value.totalScore }
                .thenBy { it.value.nickname.lowercase() }
        )
    }

    var editingTarget by remember { mutableStateOf<Pair<String, RoomPlayer>?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Standings Header Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StageDeepIndigo,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = SpotlightGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (room.roomStatus == RoomStatus.FINISHED) {
                                    "Final Match Standings"
                                } else {
                                    "Points & Cumulative Standings"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Round ${room.currentRound} of 3 • Max 15 pts/round (Score = 15 - Questions Asked)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotlightGold,
                            textAlign = TextAlign.Center
                        )

                        if (standings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                standings.take(3).forEachIndexed { idx, (_, p) ->
                                    PodiumMiniCard(
                                        rank = idx + 1,
                                        nickname = p.nickname,
                                        totalScore = p.totalScore.toInt(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Host Round Progression Controls
            if (isHost && room.roomStatus != RoomStatus.FINISHED) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Host Round Controls",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap Edit on any player row below to record or adjust Round 1, 2, or 3 points. Ready for the next round?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val currentRound = room.currentRound.toInt()
                                if (currentRound < ScoringFormula.MAX_ROUNDS) {
                                    Button(
                                        onClick = { onHostAdvanceRound(currentRound + 1, true) },
                                        enabled = !busy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("advance_round_button")
                                    ) {
                                        Text("Start Round ${currentRound + 1}")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.NavigateNext, contentDescription = null)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onHostFinishMatch,
                                    enabled = !busy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("finish_match_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish Match")
                                }
                            }
                        }
                    }
                }
            }

            // Scoreboard Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = "R1",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = "R2",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = "R3",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                    if (isHost && room.roomStatus != RoomStatus.FINISHED) {
                        Spacer(modifier = Modifier.width(44.dp))
                    }
                }
            }

            // Scoreboard Player Rows
            itemsIndexed(standings, key = { _, entry -> entry.key }) { index, (uid, player) ->
                ScoreboardPlayerRow(
                    rank = index + 1,
                    uid = uid,
                    player = player,
                    isMe = uid == currentUserId,
                    canHostEdit = isHost && room.roomStatus != RoomStatus.FINISHED,
                    onEditClick = { editingTarget = uid to player }
                )
            }
        }
    }

    editingTarget?.let { (targetUid, targetPlayer) ->
        HostEditScoreDialog(
            player = targetPlayer,
            defaultRound = room.currentRound.toInt().coerceIn(1, 3),
            onSave = { round, score ->
                onHostRecordScore(targetUid, round, score)
                editingTarget = null
            },
            onDismiss = { editingTarget = null }
        )
    }
}

@Composable
private fun PodiumMiniCard(
    rank: Int,
    nickname: String,
    totalScore: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (rank) {
        1 -> SpotlightGold
        2 -> Color(0xFFD4D4D8)
        else -> Color(0xFFD97706)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = badgeColor,
                contentColor = StageDeepIndigo,
                shape = CircleShape,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = nickname,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$totalScore pts",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun ScoreboardPlayerRow(
    rank: Int,
    uid: String,
    player: RoomPlayer,
    isMe: Boolean,
    canHostEdit: Boolean,
    onEditClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isMe) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isMe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("score_row_$uid")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (rank == 1) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMe) "${player.nickname} (You)" else player.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${player.round1Score}",
                fontFamily = JetBrainsMonoFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = "${player.round2Score}",
                fontFamily = JetBrainsMonoFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = "${player.round3Score}",
                fontFamily = JetBrainsMonoFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = "${player.totalScore}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.9f)
            )

            if (canHostEdit) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("edit_score_$uid")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit round score for ${player.nickname}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HostEditScoreDialog(
    player: RoomPlayer,
    defaultRound: Int,
    onSave: (round: Int, score: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRound by remember { mutableIntStateOf(defaultRound) }
    val initialScore = when (selectedRound) {
        1 -> player.round1Score.toInt()
        2 -> player.round2Score.toInt()
        else -> player.round3Score.toInt()
    }
    var questionsAsked by remember(selectedRound) {
        mutableIntStateOf((15 - initialScore).coerceIn(0, 15))
    }
    var unguessed by remember(selectedRound) { mutableStateOf(initialScore == 0 && questionsAsked == 15) }

    val computedScore = ScoringFormula.calculateScore(questionsAsked, !unguessed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Score • ${player.nickname}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Round:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { r ->
                        FilterChip(
                            selected = selectedRound == r,
                            onClick = { selectedRound = r },
                            label = { Text("Round $r") }
                        )
                    }
                }

                Text("Questions Asked (0–15):", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            unguessed = false
                            questionsAsked = (questionsAsked - 1).coerceAtLeast(0)
                        },
                        modifier = Modifier.testTag("host_score_minus_q")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Fewer questions")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (unguessed) "Unguessed" else "$questionsAsked Qs",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFamily
                        )
                        Text(
                            text = "Score = $computedScore pts",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            unguessed = false
                            questionsAsked = (questionsAsked + 1).coerceAtMost(15)
                        },
                        modifier = Modifier.testTag("host_score_plus_q")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "More questions")
                    }
                }

                OutlinedButton(
                    onClick = {
                        unguessed = !unguessed
                        if (unguessed) questionsAsked = 15
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (unguessed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("host_score_unguessed_toggle")
                ) {
                    Text(if (unguessed) "Marked Unguessed (0 pts)" else "Mark as Unguessed (0 pts)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedRound, computedScore) },
                modifier = Modifier.testTag("host_save_score_button")
            ) {
                Text("Save $computedScore pts")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
