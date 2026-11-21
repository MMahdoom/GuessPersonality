package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.GameRepository
import com.example.model.PersonalityRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RoomScreen
import com.example.ui.theme.GuessPersonalityTheme
import com.example.util.QrCodeUtils
import com.example.viewmodel.LobbyViewModel
import com.example.viewmodel.RoomViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val deepLinkRoomCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkRoomCode.value = extractCodeFromIntent(intent)

        setContent {
            GuessPersonalityTheme {
                AppContent(
                    pendingDeepLinkCode = deepLinkRoomCode.value,
                    onDeepLinkConsumed = { deepLinkRoomCode.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractCodeFromIntent(intent)?.let { code ->
            deepLinkRoomCode.value = code
        }
    }

    private fun extractCodeFromIntent(intent: Intent?): String? {
        val dataString = intent?.dataString ?: return null
        return QrCodeUtils.extractRoomCodeFromScan(dataString)
    }
}

@Composable
private fun AppContent(
    pendingDeepLinkCode: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Player"
        key(uid) {
            AuthenticatedAppShell(
                currentUserId = uid,
                defaultDisplayName = displayName,
                pendingDeepLinkCode = pendingDeepLinkCode,
                onDeepLinkConsumed = onDeepLinkConsumed
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUserId: String,
    defaultDisplayName: String,
    pendingDeepLinkCode: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val personalityRepo = remember(context) { PersonalityRepository(context) }
    val allPersonalities = remember(personalityRepo) { personalityRepo.allPersonalities }

    var activeRoomCode by rememberSaveable { mutableStateOf<String?>(null) }

    val lobbyViewModel: LobbyViewModel = viewModel(
        key = "lobby_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LobbyViewModel(
                    repository = GameRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val roomCode = activeRoomCode
    if (roomCode == null) {
        HomeScreen(
            viewModel = lobbyViewModel,
            currentUserId = currentUserId,
            allPersonalities = allPersonalities,
            pendingDeepLinkCode = pendingDeepLinkCode,
            onDeepLinkConsumed = onDeepLinkConsumed,
            onOpenRoom = { code -> activeRoomCode = code },
            onSignOut = {
                activeRoomCode = null
                signOutUser(credentialManager = credentialManager, scope = scope)
            }
        )
    } else {
        key(roomCode) {
            val roomViewModel: RoomViewModel = viewModel(
                key = "room_${currentUserId}_$roomCode",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        RoomViewModel(
                            repository = GameRepository(db),
                            roomCode = roomCode,
                            currentUserId = currentUserId
                        )
                    }
                }
            )
            RoomScreen(
                viewModel = roomViewModel,
                allPersonalities = allPersonalities,
                personalityLookup = { id -> personalityRepo.getById(id) },
                onNavigateBack = { activeRoomCode = null }
            )
        }
    }
}
