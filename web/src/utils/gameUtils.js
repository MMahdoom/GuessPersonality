export const SESSION_TIMEOUT_MS = 60 * 60 * 1000; // 60 minutes of inactivity
export const MAX_ROUNDS = 3;
export const MAX_QUESTIONS_PER_ROUND = 5;
export const MAX_TOTAL_QUESTIONS = 15;

const ROOM_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

export function generateRoomCode(length = 6) {
  let code = '';
  for (let i = 0; i < length; i++) {
    code += ROOM_ALPHABET[Math.floor(Math.random() * ROOM_ALPHABET.length)];
  }
  return code;
}

export function normalizeRoomCode(raw) {
  return String(raw || '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 10);
}

/**
 * Exact scoring formula:
 * Score = 15 - (number of questions asked), clamped at 0 minimum if
 * questions reach/exceed 15 or unguessed.
 */
export function calculateScore(questionsAsked, guessedCorrectly = true) {
  if (!guessedCorrectly) return 0;
  return Math.max(0, Math.min(MAX_TOTAL_QUESTIONS, MAX_TOTAL_QUESTIONS - Number(questionsAsked || 0)));
}

function randomInt(maxExclusive) {
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const buf = new Uint32Array(1);
    crypto.getRandomValues(buf);
    return buf[0] % maxExclusive;
  }
  return Math.floor(Math.random() * maxExclusive);
}

export function shuffleArray(list) {
  const arr = [...list];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = randomInt(i + 1);
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

export function isRoomExpired(room, now = Date.now()) {
  if (!room || !room.lastActiveAt) return false;
  return now - room.lastActiveAt >= SESSION_TIMEOUT_MS;
}

export function formatRemainingSession(room, now = Date.now()) {
  if (!room || !room.lastActiveAt) return '60m';
  const remainingMs = Math.max(0, SESSION_TIMEOUT_MS - (now - room.lastActiveAt));
  const totalMins = Math.ceil(remainingMs / 60000);
  if (totalMins <= 1) return '<1m';
  return `${totalMins}m`;
}

export function getOrCreateBrowserPlayerId() {
  const key = 'gtp_browser_player_id';
  let id = sessionStorage.getItem(key);
  if (!id) {
    id = `p_${Math.random().toString(36).slice(2, 10)}${Date.now().toString(36)}`;
    sessionStorage.setItem(key, id);
  }
  return id;
}

export function buildJoinUrl(roomCode) {
  const url = new URL(window.location.href);
  url.search = `?room=${encodeURIComponent(roomCode)}`;
  url.hash = '';
  return url.toString();
}

export function getInitials(name) {
  return String(name || '?')
    .split(/\s+/)
    .filter((w) => w && /[a-zA-Z]/.test(w[0]))
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('') || '?';
}
