package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.GameRoom
import com.example.model.Personality
import com.example.model.UiState
import com.example.viewmodel.RoomViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    viewModel: RoomViewModel,
    allPersonalities: List<Personality>,
    personalityLookup: (Int) -> Personality?,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDeckBuilder by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val currentRoom: GameRoom? = (roomState as? UiState.Success)?.data
    val isHost = currentRoom?.hostId == viewModel.currentUserId

    val tabItems = listOf(
        Triple(stringResource(R.string.tab_lobby_deck), Icons.Filled.Groups, Icons.Outlined.Groups),
        Triple(stringResource(R.string.tab_play_forehead), Icons.Filled.Visibility, Icons.Outlined.Visibility),
        Triple(stringResource(R.string.tab_points), Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("room_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = currentRoom?.roomName ?: "Room ${viewModel.roomCode}",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1
                            )
                            Text(
                                text = "Code: ${viewModel.roomCode} • Round ${currentRoom?.currentRound ?: 1}/3",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        if (currentRoom != null && !isHost) {
                            IconButton(
                                onClick = { viewModel.leaveRoom(onNavigateBack) },
                                modifier = Modifier.testTag("leave_room_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(R.string.leave_room)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        tabItems.forEachIndexed { idx, (label, filledIcon, outlinedIcon) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == idx) filledIcon else outlinedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("room_tab_$idx")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        tabItems.forEachIndexed { idx, (label, filledIcon, outlinedIcon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == idx) filledIcon else outlinedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("room_rail_tab_$idx")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val state = roomState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onNavigateBack) {
                                    Text("Back to Home")
                                }
                            }
                        }

                        is UiState.Success -> {
                            val room = state.data
                            if (room == null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Text(
                                        text = "Room ${viewModel.roomCode} was closed by the host.",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onNavigateBack) {
                                        Text("Back to Home")
                                    }
                                }
                            } else {
                                when (selectedTab) {
                                    0 -> LobbyDeckTab(
                                        room = room,
                                        currentUserId = viewModel.currentUserId,
                                        isHost = isHost,
                                        busy = busy,
                                        onOpenDeckBuilder = { showDeckBuilder = true },
                                        onQuickStartMatch = {
                                            viewModel.hostFinalizeDeck(
                                                room = room,
                                                selectedIds = room.deckIds.map { it.toInt() },
                                                startPlaying = true
                                            )
                                            selectedTab = 1
                                        },
                                        onGoToForeheadTab = { selectedTab = 1 },
                                        onCopyFeedback = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )

                                    1 -> PlayForeheadTab(
                                        room = room,
                                        currentUserId = viewModel.currentUserId,
                                        personalityLookup = personalityLookup,
                                        busy = busy,
                                        onPickCard = { cardNum -> viewModel.pickCard(room, cardNum) },
                                        onSetCardVisible = { vis -> viewModel.setCardVisible(room, vis) },
                                        onIncrementQuestion = { viewModel.incrementQuestion(room) },
                                        onDecrementQuestion = { viewModel.decrementQuestion(room) },
                                        onResetRoundQuestionBlock = { viewModel.resetRoundQuestionCounter(room) },
                                        onCompleteGuess = { correct -> viewModel.completeGuess(room, correct) }
                                    )

                                    else -> PointsTab(
                                        room = room,
                                        currentUserId = viewModel.currentUserId,
                                        isHost = isHost,
                                        busy = busy,
                                        onHostRecordScore = { uid, round, score ->
                                            viewModel.hostRecordScore(room, uid, round, score)
                                        },
                                        onHostAdvanceRound = { nextRound, resetCards ->
                                            viewModel.hostAdvanceRound(room, nextRound, resetCards)
                                        },
                                        onHostFinishMatch = { viewModel.hostFinishMatch(room) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeckBuilder && currentRoom != null) {
        PersonalityDeckDialog(
            allPersonalities = allPersonalities,
            selectionMode = true,
            initialSelectedIds = currentRoom.deckIds.map { it.toInt() }.toSet(),
            onConfirmSelection = { chosenIds, startNow ->
                viewModel.hostFinalizeDeck(currentRoom, chosenIds, startNow)
                showDeckBuilder = false
                if (startNow) selectedTab = 1
            },
            onDismiss = { showDeckBuilder = false }
        )
    }
}
