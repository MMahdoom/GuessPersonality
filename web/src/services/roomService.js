import { initializeApp, getApps } from 'firebase/app';
import { getDatabase, ref, get, set, update, remove, onValue } from 'firebase/database';
import {
  SESSION_TIMEOUT_MS,
  calculateScore,
  generateRoomCode,
  isRoomExpired,
  normalizeRoomCode,
  shuffleArray,
} from '../utils/gameUtils.js';

const rawDbUrl = (import.meta.env.VITE_FIREBASE_DATABASE_URL || '').trim().replace(/\/+$/, '');

const firebaseConfig = {
  ...(import.meta.env.VITE_FIREBASE_API_KEY
    ? { apiKey: import.meta.env.VITE_FIREBASE_API_KEY.trim() }
    : {}),
  ...(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN
    ? { authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN.trim() }
    : {}),
  ...(rawDbUrl ? { databaseURL: rawDbUrl } : {}),
  ...(import.meta.env.VITE_FIREBASE_PROJECT_ID
    ? { projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID.trim() }
    : {}),
  ...(import.meta.env.VITE_FIREBASE_APP_ID
    ? { appId: import.meta.env.VITE_FIREBASE_APP_ID.trim() }
    : {}),
};

// Firebase Realtime Database only requires databaseURL for unauthenticated room-code sync
export const isFirebaseConfigured = Boolean(rawDbUrl);

let rtdb = null;
if (isFirebaseConfigured) {
  const app = getApps().length ? getApps()[0] : initializeApp(firebaseConfig);
  rtdb = getDatabase(app, rawDbUrl);
}

function formatDbError(err) {
  const msg = String(err?.message || err || '');
  if (msg.toLowerCase().includes('permission_denied') || msg.toLowerCase().includes('permission denied')) {
    return new Error(
      'Firebase Realtime Database denied access. Open Firebase Console → Realtime Database → Rules, paste web/database.rules.json, and click Publish.'
    );
  }
  return err instanceof Error ? err : new Error(msg || 'Database sync error.');
}

// Local multi-tab BroadcastChannel fallback when Firebase env vars are not set yet
const LOCAL_PREFIX = 'gtp_room_';
const channel =
  typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel('gtp_rooms_sync') : null;

function localRead(code) {
  try {
    const raw = localStorage.getItem(LOCAL_PREFIX + code);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function localWrite(code, room) {
  if (room == null) {
    localStorage.removeItem(LOCAL_PREFIX + code);
  } else {
    localStorage.setItem(LOCAL_PREFIX + code, JSON.stringify(room));
  }
  channel?.postMessage({ roomCode: code, room });
}

function makeDefaultPlayer(nickname) {
  return {
    nickname: String(nickname || 'Player').trim().slice(0, 30) || 'Player',
    selectedCardNumber: 0,
    cardVisible: false,
    roundQuestions: 0,
    totalQuestions: 0,
    guessCompleted: false,
    guessedCorrectly: false,
    round1Score: 0,
    round2Score: 0,
    round3Score: 0,
    totalScore: 0,
  };
}

function touchActivity(now = Date.now()) {
  return {
    lastActiveAt: now,
    expiresAt: now + SESSION_TIMEOUT_MS,
  };
}

async function readRoomRaw(roomCode) {
  const code = normalizeRoomCode(roomCode);
  if (isFirebaseConfigured && rtdb) {
    try {
      const snap = await get(ref(rtdb, `rooms/${code}`));
      return snap.exists() ? snap.val() : null;
    } catch (err) {
      throw formatDbError(err);
    }
  }
  return localRead(code);
}

export async function clearRoom(roomCode) {
  const code = normalizeRoomCode(roomCode);
  if (!code) return;
  if (isFirebaseConfigured && rtdb) {
    try {
      await remove(ref(rtdb, `rooms/${code}`));
    } catch {
      // Ignore cleanup errors if already removed
    }
  } else {
    localWrite(code, null);
  }
}

async function readActiveRoomOrAutoClear(roomCode) {
  const code = normalizeRoomCode(roomCode);
  const room = await readRoomRaw(code);
  if (!room) return null;
  if (isRoomExpired(room)) {
    await clearRoom(code);
    return null;
  }
  return room;
}

async function writeFullRoom(roomCode, roomObj) {
  const code = normalizeRoomCode(roomCode);
  const next = { ...roomObj, ...touchActivity() };
  if (isFirebaseConfigured && rtdb) {
    try {
      await set(ref(rtdb, `rooms/${code}`), next);
    } catch (err) {
      throw formatDbError(err);
    }
  } else {
    localWrite(code, next);
  }
  return next;
}

export async function createRoom({ roomName, hostId, hostNickname, initialDeckIds }) {
  let code = generateRoomCode(6);
  for (let i = 0; i < 5; i++) {
    const existing = await readActiveRoomOrAutoClear(code);
    if (!existing) break;
    code = generateRoomCode(6);
  }

  const now = Date.now();
  const deckIds = shuffleArray(
    (initialDeckIds && initialDeckIds.length ? initialDeckIds : Array.from({ length: 15 }, (_, i) => i + 1))
      .map(Number)
      .filter((id) => id >= 1 && id <= 100)
  );

  const cleanHost = String(hostNickname || 'Host').trim().slice(0, 30) || 'Host';
  const cleanRoomName = String(roomName || 'Party Night').trim().slice(0, 60) || 'Party Night';

  const room = {
    roomCode: code,
    roomName: cleanRoomName,
    hostId,
    hostName: cleanHost,
    status: 'LOBBY',
    currentRound: 1,
    deckIds,
    eliminatedIds: [],
    deckFinalized: false,
    players: {
      [hostId]: makeDefaultPlayer(cleanHost),
    },
    createdAt: now,
    ...touchActivity(now),
  };

  await writeFullRoom(code, room);
  return room;
}

export async function joinRoom({ roomCode, playerId, nickname }) {
  const code = normalizeRoomCode(roomCode);
  if (code.length < 4) {
    throw new Error('Please enter a valid room code.');
  }
  const room = await readActiveRoomOrAutoClear(code);
  if (!room) {
    if (!isFirebaseConfigured) {
      throw new Error(
        `Room ${code} was not found on this device. Your site is in Single-Device Mode — add VITE_FIREBASE_DATABASE_URL in GitHub Actions Secrets/Variables and redeploy to join from another phone.`
      );
    }
    throw new Error(`Room ${code} does not exist or expired after 60 minutes of inactivity.`);
  }
  if (room.status === 'FINISHED') {
    throw new Error(`Room ${code} has already finished.`);
  }

  const cleanNick = String(nickname || 'Player').trim().slice(0, 30) || 'Player';
  const players = { ...(room.players || {}) };
  if (players[playerId]) {
    players[playerId] = { ...players[playerId], nickname: cleanNick };
  } else {
    if (Object.keys(players).length >= 25) {
      throw new Error(`Room ${code} is full.`);
    }
    players[playerId] = makeDefaultPlayer(cleanNick);
  }

  const updated = { ...room, players };
  await writeFullRoom(code, updated);
  return updated;
}

export function subscribeRoom(roomCode, onData, onExpired) {
  const code = normalizeRoomCode(roomCode);
  let lastRoom = null;

  const handleRoomValue = async (room) => {
    if (!room) {
      lastRoom = null;
      onData(null);
      return;
    }
    if (isRoomExpired(room)) {
      lastRoom = null;
      await clearRoom(code);
      onExpired?.();
      return;
    }
    lastRoom = room;
    onData(room);
  };

  // Periodic 30-second inactivity check to auto-clear stale rooms
  const intervalId = setInterval(() => {
    if (lastRoom && isRoomExpired(lastRoom)) {
      clearRoom(code).then(() => onExpired?.());
    }
  }, 30000);

  if (isFirebaseConfigured && rtdb) {
    const roomRef = ref(rtdb, `rooms/${code}`);
    const unsubscribe = onValue(roomRef, (snap) => {
      handleRoomValue(snap.exists() ? snap.val() : null);
    });
    return () => {
      clearInterval(intervalId);
      unsubscribe();
    };
  }

  // Local BroadcastChannel + storage listener fallback
  handleRoomValue(localRead(code));
  const onMsg = (evt) => {
    if (evt.data?.roomCode === code) {
      handleRoomValue(evt.data.room);
    }
  };
  const onStorage = (evt) => {
    if (evt.key === LOCAL_PREFIX + code) {
      handleRoomValue(evt.newValue ? JSON.parse(evt.newValue) : null);
    }
  };
  channel?.addEventListener('message', onMsg);
  window.addEventListener('storage', onStorage);

  return () => {
    clearInterval(intervalId);
    channel?.removeEventListener('message', onMsg);
    window.removeEventListener('storage', onStorage);
  };
}

export async function finalizeDeck({ roomCode, hostId, selectedIds, startPlaying }) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired or not found.');
  if (room.hostId !== hostId) throw new Error('Only the host can modify the deck.');
  if (!selectedIds.length) throw new Error('Select at least 1 personality.');

  const shuffled = shuffleArray([...new Set(selectedIds.map(Number).filter((id) => id >= 1 && id <= 100))]);
  const eliminated = (room.eliminatedIds || []).filter((id) => !shuffled.includes(id));

  const nextPlayers = {};
  for (const [uid, p] of Object.entries(room.players || {})) {
    const outOfRange = p.selectedCardNumber > shuffled.length;
    nextPlayers[uid] = {
      ...p,
      selectedCardNumber: startPlaying || outOfRange ? 0 : p.selectedCardNumber,
      cardVisible: false,
      roundQuestions: startPlaying ? 0 : p.roundQuestions,
      totalQuestions: startPlaying ? 0 : p.totalQuestions,
      guessCompleted: startPlaying ? false : p.guessCompleted,
      guessedCorrectly: startPlaying ? false : p.guessedCorrectly,
    };
  }

  return writeFullRoom(roomCode, {
    ...room,
    deckIds: shuffled,
    eliminatedIds: eliminated,
    deckFinalized: true,
    status: startPlaying ? 'PLAYING' : room.status,
    players: nextPlayers,
  });
}

/**
 * Host eliminates a personality from the active pile.
 * Removes it from deckIds, adds it to eliminatedIds, and adjusts any player card
 * numbers that were pointing at or above the removed index so 1..N stays exact.
 */
export async function eliminatePersonalityFromPile({ roomCode, hostId, personalityId }) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired or not found.');
  if (room.hostId !== hostId) throw new Error('Only the host can eliminate personalities.');

  const currentDeck = (room.deckIds || []).map(Number);
  const removedIndex = currentDeck.indexOf(Number(personalityId));
  if (removedIndex === -1) return room;
  if (currentDeck.length <= 1) {
    throw new Error('Keep at least 1 personality in the active pile.');
  }

  const nextDeck = currentDeck.filter((id) => id !== Number(personalityId));
  const nextEliminated = [...new Set([...(room.eliminatedIds || []), Number(personalityId)])];

  const removedCardNum = removedIndex + 1;
  const nextPlayers = {};
  for (const [uid, p] of Object.entries(room.players || {})) {
    let nextCard = p.selectedCardNumber;
    let nextVisible = p.cardVisible;
    if (nextCard === removedCardNum) {
      nextCard = 0;
      nextVisible = false;
    } else if (nextCard > removedCardNum) {
      nextCard = nextCard - 1;
    }
    nextPlayers[uid] = {
      ...p,
      selectedCardNumber: nextCard,
      cardVisible: nextVisible,
    };
  }

  return writeFullRoom(roomCode, {
    ...room,
    deckIds: nextDeck,
    eliminatedIds: nextEliminated,
    players: nextPlayers,
  });
}

export async function restoreEliminatedPersonality({ roomCode, hostId, personalityId }) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired or not found.');
  if (room.hostId !== hostId) throw new Error('Only the host can restore personalities.');

  const idNum = Number(personalityId);
  const currentDeck = (room.deckIds || []).map(Number);
  const nextDeck = currentDeck.includes(idNum) ? currentDeck : [...currentDeck, idNum];
  const nextEliminated = (room.eliminatedIds || []).filter((id) => Number(id) !== idNum);

  return writeFullRoom(roomCode, {
    ...room,
    deckIds: nextDeck,
    eliminatedIds: nextEliminated,
  });
}

async function patchPlayer(roomCode, playerId, mutatePlayerFn) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired after 60 minutes of inactivity.');
  const existing = room.players?.[playerId];
  if (!existing) throw new Error('You are not in this room.');

  const updatedPlayer = mutatePlayerFn(existing, room);
  const now = Date.now();
  const activity = touchActivity(now);

  if (isFirebaseConfigured && rtdb) {
    try {
      await update(ref(rtdb, `rooms/${normalizeRoomCode(roomCode)}`), {
        [`players/${playerId}`]: updatedPlayer,
        ...activity,
      });
    } catch (err) {
      throw formatDbError(err);
    }
  } else {
    localWrite(normalizeRoomCode(roomCode), {
      ...room,
      players: { ...room.players, [playerId]: updatedPlayer },
      ...activity,
    });
  }
  return updatedPlayer;
}

export async function pickCard({ roomCode, playerId, cardNumber }) {
  return patchPlayer(roomCode, playerId, (p, room) => {
    const maxN = (room.deckIds || []).length;
    const num = Number(cardNumber);
    if (num < 1 || num > maxN) {
      throw new Error(`Choose a number between 1 and ${maxN}.`);
    }
    return {
      ...p,
      selectedCardNumber: num,
      cardVisible: false, // ALWAYS hidden by default for forehead mode
      roundQuestions: 0,
      totalQuestions: 0,
      guessCompleted: false,
      guessedCorrectly: false,
    };
  });
}

export async function setCardVisibility({ roomCode, playerId, visible }) {
  return patchPlayer(roomCode, playerId, (p) => ({
    ...p,
    cardVisible: Boolean(visible),
  }));
}

export async function updateQuestions({ roomCode, playerId, roundQuestions, totalQuestions }) {
  return patchPlayer(roomCode, playerId, (p) => ({
    ...p,
    roundQuestions: Math.max(0, Math.min(5, Number(roundQuestions))),
    totalQuestions: Math.max(0, Math.min(15, Number(totalQuestions))),
  }));
}

export async function completeGuess({ roomCode, playerId, guessedCorrectly }) {
  return patchPlayer(roomCode, playerId, (p, room) => {
    const score = calculateScore(p.totalQuestions, guessedCorrectly);
    const round = Number(room.currentRound || 1);
    const r1 = round === 1 ? score : Number(p.round1Score || 0);
    const r2 = round === 2 ? score : Number(p.round2Score || 0);
    const r3 = round === 3 ? score : Number(p.round3Score || 0);
    return {
      ...p,
      guessCompleted: true,
      guessedCorrectly: Boolean(guessedCorrectly),
      cardVisible: false,
      round1Score: r1,
      round2Score: r2,
      round3Score: r3,
      totalScore: r1 + r2 + r3,
    };
  });
}

export async function hostRecordScore({ roomCode, hostId, targetPlayerId, round, score }) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired.');
  if (room.hostId !== hostId) throw new Error('Only the host can edit scores.');
  const target = room.players?.[targetPlayerId];
  if (!target) throw new Error('Player not found.');

  const safeScore = Math.max(0, Math.min(15, Number(score)));
  const r1 = round === 1 ? safeScore : Number(target.round1Score || 0);
  const r2 = round === 2 ? safeScore : Number(target.round2Score || 0);
  const r3 = round === 3 ? safeScore : Number(target.round3Score || 0);

  return patchPlayer(roomCode, targetPlayerId, (p) => ({
    ...p,
    round1Score: r1,
    round2Score: r2,
    round3Score: r3,
    totalScore: r1 + r2 + r3,
  }));
}

export async function advanceRound({ roomCode, hostId, nextRound, eliminateUsedCards = true }) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired.');
  if (room.hostId !== hostId) throw new Error('Only the host can advance rounds.');

  const currentDeck = (room.deckIds || []).map(Number);
  const usedIds = new Set();
  if (eliminateUsedCards) {
    for (const p of Object.values(room.players || {})) {
      if (p.selectedCardNumber >= 1 && p.selectedCardNumber <= currentDeck.length) {
        usedIds.add(currentDeck[p.selectedCardNumber - 1]);
      }
    }
  }

  let nextDeck = currentDeck.filter((id) => !usedIds.has(id));
  let nextEliminated = [...new Set([...(room.eliminatedIds || []), ...usedIds])];
  if (nextDeck.length === 0) {
    // Keep current deck if eliminating used cards would empty the pile
    nextDeck = currentDeck;
    nextEliminated = room.eliminatedIds || [];
  }

  const nextPlayers = {};
  for (const [uid, p] of Object.entries(room.players || {})) {
    nextPlayers[uid] = {
      ...p,
      selectedCardNumber: 0,
      cardVisible: false,
      roundQuestions: 0,
      totalQuestions: 0,
      guessCompleted: false,
      guessedCorrectly: false,
    };
  }

  return writeFullRoom(roomCode, {
    ...room,
    status: 'PLAYING',
    currentRound: Math.max(1, Math.min(3, Number(nextRound))),
    deckIds: shuffleArray(nextDeck),
    eliminatedIds: nextEliminated,
    players: nextPlayers,
  });
}

export async function finishMatch({ roomCode, hostId }) {
  const room = await readActiveRoomOrAutoClear(roomCode);
  if (!room) throw new Error('Room expired.');
  if (room.hostId !== hostId) throw new Error('Only the host can finish the match.');
  return writeFullRoom(roomCode, { ...room, status: 'FINISHED' });
}
