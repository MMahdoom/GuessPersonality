package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.GameRoom
import com.example.model.Personality
import com.example.model.RoomStatus
import com.example.model.UiState
import com.example.ui.theme.SpotlightGold
import com.example.ui.theme.StageDeepIndigo
import com.example.viewmodel.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LobbyViewModel,
    currentUserId: String,
    allPersonalities: List<Personality>,
    pendingDeepLinkCode: String?,
    onDeepLinkConsumed: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val roomsState by viewModel.myRoomsState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val preferredNickname = profile?.nickname?.takeIf { it.isNotBlank() } ?: viewModel.fallbackNickname

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showCatalogDialog by remember { mutableStateOf(false) }
    var prefilledJoinCode by remember { mutableStateOf("") }

    LaunchedEffect(pendingDeepLinkCode) {
        if (!pendingDeepLinkCode.isNullOrBlank()) {
            prefilledJoinCode = pendingDeepLinkCode
            showJoinDialog = true
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Guess the Personality",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Hi, $preferredNickname",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.sign_out)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Spotlight Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_marquee),
                            contentDescription = stringResource(R.string.cd_hero_marquee),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            StageDeepIndigo.copy(alpha = 0.92f),
                                            StageDeepIndigo.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "PARTY STAGE READY",
                                style = MaterialTheme.typography.labelMedium,
                                color = SpotlightGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Forehead Reveal &\n3-Round Guessing",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "70 Tamil • 30 Pan-Indian • 15 Questions Max",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE9DFF5)
                            )
                        }
                    }
                }

                // Lifetime Stats Row (if user has played)
                if (profile != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatChipCard(
                                label = "Matches",
                                value = "${profile!!.gamesPlayed}",
                                modifier = Modifier.weight(1f)
                            )
                            StatChipCard(
                                label = "Total Pts",
                                value = "${profile!!.totalPoints}",
                                modifier = Modifier.weight(1f)
                            )
                            StatChipCard(
                                label = "Best Round",
                                value = "${profile!!.bestRoundScore}/15",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Primary Actions: Host vs Join
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            onClick = { showCreateDialog = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(132.dp)
                                .testTag("create_room_button")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = "Create Room",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Host QR & pick deck",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        Card(
                            onClick = {
                                prefilledJoinCode = ""
                                showJoinDialog = true
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(132.dp)
                                .testTag("join_room_button")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = "Join Room",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Scan QR or code",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 100 Personalities Dataset Explorer Card
                item {
                    Surface(
                        onClick = { showCatalogDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("browse_catalog_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Explore Built-In 100 Personalities",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "70 Tamil + 30 Pan-Indian across Cinema, Music, Sports & History",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Active & Recent Rooms Section
                item {
                    Text(
                        text = "Your Party Rooms",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                when (val state = roomsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No active rooms yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap Create Room to host with a scannable QR code, or Join Room to hop into a friend's party.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.data, key = { it.roomCode }) { room ->
                                RoomSummaryCard(
                                    room = room,
                                    isHost = room.hostId == currentUserId,
                                    onOpen = { onOpenRoom(room.roomCode) },
                                    onDelete = { viewModel.deleteRoom(room.roomCode) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoomDialog(
            defaultNickname = preferredNickname,
            allPersonalities = allPersonalities,
            busy = busy,
            onCreate = { roomName, nickname, initialDeck ->
                viewModel.createRoom(roomName, nickname, initialDeck) { code ->
                    showCreateDialog = false
                    onOpenRoom(code)
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showJoinDialog) {
        JoinRoomDialog(
            initialCode = prefilledJoinCode,
            defaultNickname = preferredNickname,
            busy = busy,
            onOpenScanner = { showQrScanner = true },
            onJoin = { code, nickname ->
                viewModel.joinRoom(code, nickname) { joinedCode ->
                    showJoinDialog = false
                    onOpenRoom(joinedCode)
                }
            },
            onDismiss = { showJoinDialog = false }
        )
    }

    if (showQrScanner) {
        QrScannerDialog(
            onCodeScanned = { code ->
                prefilledJoinCode = code
                showQrScanner = false
                showJoinDialog = true
            },
            onDismiss = { showQrScanner = false }
        )
    }

    if (showCatalogDialog) {
        PersonalityDeckDialog(
            allPersonalities = allPersonalities,
            selectionMode = false,
            onDismiss = { showCatalogDialog = false }
        )
    }
}

@Composable
private fun StatChipCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoomSummaryCard(
    room: GameRoom,
    isHost: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("room_card_${room.roomCode}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = when (room.roomStatus) {
                            RoomStatus.PLAYING -> MaterialTheme.colorScheme.secondaryContainer
                            RoomStatus.FINISHED -> MaterialTheme.colorScheme.tertiaryContainer
                            RoomStatus.LOBBY -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (room.roomStatus) {
                                RoomStatus.LOBBY -> "LOBBY"
                                RoomStatus.PLAYING -> "ROUND ${room.currentRound}/3"
                                RoomStatus.FINISHED -> "FINISHED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isHost) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Host",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = room.roomName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${room.players.size} players • ${room.deckIds.size} personalities • Host: ${room.hostName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isHost) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_room_${room.roomCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete room",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = "Enter room",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CreateRoomDialog(
    defaultNickname: String,
    allPersonalities: List<Personality>,
    busy: Boolean,
    onCreate: (String, String, List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf("Chennai Party Night") }
    var nickname by remember { mutableStateOf(defaultNickname) }
    var selectedPreset by remember { mutableIntStateOf(0) }

    val presets = listOf(
        "Quick Mix (15)" to 15,
        "Tamil Classics (25)" to 25,
        "Mega Party (40)" to 40,
        "All 100 Icons" to 100
    )

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Create Party Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it.take(60) },
                    label = { Text("Room Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_name_input")
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(30) },
                    label = { Text("Your Host Nickname") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_host_nickname_input")
                )
                Text(
                    text = "Starting Deck Subset (customizable in Lobby):",
                    style = MaterialTheme.typography.labelLarge
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.chunked(2).forEachIndexed { rowIdx, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEachIndexed { colIdx, (label, _) ->
                                val idx = rowIdx * 2 + colIdx
                                FilterChip(
                                    selected = selectedPreset == idx,
                                    onClick = { selectedPreset = idx },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deckIds = when (selectedPreset) {
                        0 -> allPersonalities.shuffled().take(15).map { it.id }
                        1 -> allPersonalities.filter { it.region == "Tamil" }.shuffled().take(25).map { it.id }
                        2 -> allPersonalities.shuffled().take(40).map { it.id }
                        else -> allPersonalities.map { it.id }
                    }
                    onCreate(roomName, nickname, deckIds)
                },
                enabled = !busy && roomName.isNotBlank() && nickname.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_room_button")
            ) {
                Text(if (busy) "Creating..." else "Create & Get QR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun JoinRoomDialog(
    initialCode: String,
    defaultNickname: String,
    busy: Boolean,
    onOpenScanner: () -> Unit,
    onJoin: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember(initialCode) { mutableStateOf(initialCode) }
    var nickname by remember { mutableStateOf(defaultNickname) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Join Party Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_qr_scanner_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Host QR Code")
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(10) },
                    label = { Text("6-Character Room Code") },
                    placeholder = { Text("e.g. KVR7M2") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("room_code_input")
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(30) },
                    label = { Text("Your Nickname") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nickname_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code, nickname) },
                enabled = !busy && code.length >= 4 && nickname.isNotBlank(),
                modifier = Modifier.testTag("confirm_join_room_button")
            ) {
                Text(if (busy) "Joining..." else "Join Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel")
            }
        }
    )
}
