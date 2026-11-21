import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  SESSION_TIMEOUT_MS,
  calculateScore,
  extractRoomCodeFromScan,
  formatRemainingSession,
  generateRoomCode,
  getInitials,
  isRoomExpired,
  normalizeRoomCode,
  shuffleArray,
  toIdArray,
} from '../src/utils/gameUtils.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const personalities = JSON.parse(
  fs.readFileSync(path.join(__dirname, '../src/data/personalities.json'), 'utf8')
);

test('Dataset integrity: 62 curated personalities with zero removed names', () => {
  assert.equal(personalities.length, 62);
  const tamil = personalities.filter((p) => p.region === 'Tamil');
  const panIndian = personalities.filter((p) => p.region === 'Pan-Indian');
  assert.equal(tamil.length, 45);
  assert.equal(panIndian.length, 17);

  const ids = new Set(personalities.map((p) => p.id));
  assert.equal(ids.size, 62);

  const removedNames = [
    'Goundamani',
    'Senthil',
    'K. Balachander',
    'S. P. Balasubrahmanyam',
    'Bharathiraja',
    'Harris Jayaraj',
    'Santhosh Narayanan',
    'Krishnamachari Srikkanth',
    'Sharath Kamal',
    'Mariyappan Thangavelu',
    'Joshna Chinappa',
    'Washington Sundar',
    'Thiruvalluvar',
    'Subramania Bharati',
    'Avvaiyar',
    'Kalki Krishnamurthy',
    'Raja Raja Chola I',
    'Kannadasan',
    'Vairamuthu',
    'Sujatha Rangarajan',
    'V. O. Chidambaram Pillai',
    'Velu Nachiyar',
    'Srinivasa Ramanujan',
    'C. V. Raman',
    'M. S. Swaminathan',
    'Satyajit Ray',
    'Lata Mangeshkar',
    'Arijit Singh',
    'P. V. Sindhu',
    'Neeraj Chopra',
    'Mary Kom',
    'Sunil Chhetri',
    'Rabindranath Tagore',
    'Swami Vivekananda',
    'R. K. Narayan',
    'Kalpana Chawla',
    'Ratan Tata',
    'Vikram Sarabhai',
  ];

  const existingNames = new Set(personalities.map((p) => p.name));
  for (const name of removedNames) {
    assert.equal(existingNames.has(name), false, `Expected "${name}" to be removed`);
  }

  for (const p of personalities) {
    assert.ok(p.name.trim().length > 0);
    assert.ok(p.wiki_title.trim().length > 0);
    assert.ok(p.category.trim().length > 0);
    assert.ok(p.era.trim().length > 0);
    assert.ok(['Tamil', 'Pan-Indian'].includes(p.region));
  }
});

test('Scoring formula: Score = clamp(15 - questionsAsked, 0, 15) and 0 if unguessed', () => {
  for (let q = 0; q <= 20; q++) {
    const expected = Math.max(0, 15 - q);
    assert.equal(calculateScore(q, true), expected, `Failed for q=${q}`);
    assert.equal(calculateScore(q, false), 0, `Unguessed must be 0 for q=${q}`);
  }
  assert.equal(calculateScore(-3, true), 15);
  assert.equal(calculateScore(undefined, true), 15);
});

test('QR scanner room code extraction handles all URL & raw formats', () => {
  assert.equal(
    extractRoomCodeFromScan('https://mmahdoom.github.io/GuessPersonality/?room=KVR7M2'),
    'KVR7M2'
  );
  assert.equal(
    extractRoomCodeFromScan('https://example.com/game/?foo=1&room=ab12cd&bar=2'),
    'AB12CD'
  );
  assert.equal(extractRoomCodeFromScan('?room=xyz987'), 'XYZ987');
  assert.equal(extractRoomCodeFromScan('kvr7m2'), 'KVR7M2');
  assert.equal(extractRoomCodeFromScan('  KVR-7M2  '), 'KVR7M2');
  assert.equal(extractRoomCodeFromScan('https://google.com'), '');
  assert.equal(extractRoomCodeFromScan('AB'), '');
  assert.equal(extractRoomCodeFromScan(''), '');
});

test('Shuffling produces non-static random permutations preserving all IDs', () => {
  const input = personalities.map((p) => p.id);
  const s1 = shuffleArray(input);
  const s2 = shuffleArray(input);
  assert.equal(s1.length, input.length);
  assert.deepEqual([...s1].sort((a, b) => a - b), [...input].sort((a, b) => a - b));
  assert.notDeepEqual(s1, input);
  assert.notDeepEqual(s1, s2);
  assert.deepEqual(shuffleArray([]), []);
  assert.deepEqual(shuffleArray([42]), [42]);
});

test('60-minute session inactivity timeout and countdown formatting', () => {
  const start = 1_700_000_000_000;
  const room = { lastActiveAt: start };
  assert.equal(isRoomExpired(room, start), false);
  assert.equal(isRoomExpired(room, start + SESSION_TIMEOUT_MS - 1), false);
  assert.equal(isRoomExpired(room, start + SESSION_TIMEOUT_MS), true);
  assert.equal(isRoomExpired(room, start + SESSION_TIMEOUT_MS + 10_000), true);

  assert.equal(formatRemainingSession(room, start), '60m');
  assert.equal(formatRemainingSession(room, start + 30 * 60_000), '30m');
  assert.equal(formatRemainingSession(room, start + 59.5 * 60_000), '<1m');
});

test('toIdArray handles standard arrays and Firebase RTDB sparse objects', () => {
  assert.deepEqual(toIdArray([1, '2', 3, 0, -5, 'bad']), [1, 2, 3]);
  assert.deepEqual(toIdArray({ 0: 10, 2: 20, 5: '30' }), [10, 20, 30]);
  assert.deepEqual(toIdArray(null), []);
  assert.deepEqual(toIdArray(undefined), []);
});

test('Room code generation and normalization', () => {
  const code = generateRoomCode(6);
  assert.equal(code.length, 6);
  assert.match(code, /^[A-Z0-9]{6}$/);
  assert.equal(normalizeRoomCode(' ab-12_cd! '), 'AB12CD');
  assert.equal(getInitials('A. P. J. Abdul Kalam'), 'AP');
  assert.equal(getInitials('Vijay'), 'V');
});
