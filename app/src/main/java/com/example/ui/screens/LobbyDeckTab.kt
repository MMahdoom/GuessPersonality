package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameRoom
import com.example.model.RoomPlayer
import com.example.model.RoomStatus
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpotlightGold
import com.example.ui.theme.StageDeepIndigo
import com.example.util.QrCodeUtils

@Composable
fun LobbyDeckTab(
    room: GameRoom,
    currentUserId: String,
    isHost: Boolean,
    busy: Boolean,
    onOpenDeckBuilder: () -> Unit,
    onQuickStartMatch: () -> Unit,
    onGoToForeheadTab: () -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val joinLink = remember(room.roomCode) { QrCodeUtils.buildJoinDeepLink(room.roomCode) }
    val qrBitmap = remember(joinLink) { QrCodeUtils.generateQrBitmap(joinLink, 480) }

    val sortedPlayers = remember(room.players, room.hostId) {
        room.players.entries.sortedWith(
            compareByDescending<Map.Entry<String, RoomPlayer>> { it.key == room.hostId }
                .thenByDescending { it.value.totalScore }
                .thenBy { it.value.nickname.lowercase() }
        )
    }

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
            // Iron Thumbs-style QR & Room Code Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
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
                        Text(
                            text = "SCAN TO JOIN ROOM",
                            style = MaterialTheme.typography.labelMedium,
                            color = SpotlightGold,
                            letterSpacing = 1.6.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = room.roomCode,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            letterSpacing = 6.sp,
                            color = Color.White,
                            modifier = Modifier.testTag("room_code_display")
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (qrBitmap != null) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp),
                                shadowElevation = 6.dp
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_qr_code),
                                    modifier = Modifier
                                        .size(196.dp)
                                        .padding(12.dp)
                                        .testTag("room_qr_image")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = joinLink,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFC9BBE0),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomCode))
                                    onCopyFeedback("Copied room code ${room.roomCode}")
                                },
                                border = BorderStroke(1.dp, SpotlightGold.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("copy_room_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy),
                                    tint = SpotlightGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Code", color = SpotlightGold)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            QrCodeUtils.buildShareInviteMessage(room.roomName, room.roomCode)
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Room Invite"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_invite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.cd_share),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.share_invite))
                            }
                        }
                    }
                }
            }

            // Host Subset Selection & Match Control Card
            item {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Personality Subset (Max # = ${room.maxSelectableNumber})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (room.deckFinalized) {
                                        "Locked & shuffled! Players pick a secret number from 1 to ${room.maxSelectableNumber}."
                                    } else {
                                        "Currently ${room.maxSelectableNumber} personalities loaded. Host can customize from all 100 icons."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (room.deckFinalized) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (room.deckFinalized) "LOCKED" else "DRAFT",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isHost && room.roomStatus != RoomStatus.FINISHED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenDeckBuilder,
                                    enabled = !busy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("open_deck_builder_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select Subset")
                                }

                                Button(
                                    onClick = onQuickStartMatch,
                                    enabled = !busy && room.deckIds.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(50.dp)
                                        .testTag("finalize_start_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (room.roomStatus == RoomStatus.PLAYING) "Reshuffle Deck"
                                        else "Finalize & Play"
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = onGoToForeheadTab,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("go_to_forehead_play_button")
                            ) {
                                Text("Go to Forehead Play (Pick 1–${room.maxSelectableNumber})")
                            }
                        }
                    }
                }
            }

            // Players Roster Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Players in Room (${sortedPlayers.size}/20)",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Round ${room.currentRound} of 3",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(sortedPlayers, key = { it.key }) { (uid, player) ->
                PlayerRosterRow(
                    player = player,
                    isHost = uid == room.hostId,
                    isMe = uid == currentUserId
                )
            }
        }
    }
}

@Composable
private fun PlayerRosterRow(
    player: RoomPlayer,
    isHost: Boolean,
    isMe: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isMe) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.nickname.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isMe) "${player.nickname} (You)" else player.nickname,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isHost) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Host",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val statusLabel = when {
                    player.selectedCardNumber <= 0L -> "Choosing a card number..."
                    player.guessCompleted && player.guessedCorrectly -> "Card #${player.selectedCardNumber} • Guessed in ${player.totalQuestions} Qs"
                    player.guessCompleted -> "Card #${player.selectedCardNumber} • Unguessed (0 pts)"
                    player.cardVisible -> "Card #${player.selectedCardNumber} • DISPLAYING ON FOREHEAD"
                    else -> "Card #${player.selectedCardNumber} • Hidden (${player.totalQuestions}/15 Qs)"
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        player.guessCompleted -> Icons.Default.CheckCircle
                        player.cardVisible -> Icons.Default.Visibility
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = when {
                        player.guessCompleted -> MaterialTheme.colorScheme.secondary
                        player.cardVisible -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${player.totalScore} pts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
