import test from 'node:test';
import assert from 'node:assert/strict';

// Minimal localStorage + window shim for Node testing of local mode
const store = new Map();
globalThis.localStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
  clear: () => store.clear(),
};
globalThis.window = {
  addEventListener: () => {},
  removeEventListener: () => {},
};

const {
  advanceRound,
  completeGuess,
  createRoom,
  eliminatePersonalityFromPile,
  finalizeDeck,
  finishMatch,
  hostRecordScore,
  joinRoom,
  pickCard,
  restoreEliminatedPersonality,
  setCardVisibility,
  startNewGame,
  updateQuestions,
} = await import('../src/services/roomService.js');

const { SESSION_TIMEOUT_MS } = await import('../src/utils/gameUtils.js');

test('Full 3-round multiplayer lifecycle, elimination permutations, scoring, replay & 60-min auto-clear', async () => {
  store.clear();

  // 1. Host creates room with 10 personalities
  const created = await createRoom({
    roomName: 'Diwali Night',
    hostId: 'host_1',
    hostNickname: 'Karthik',
    initialDeckIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
  });
  assert.equal(created.deckIds.length, 10);
  assert.equal(created.status, 'LOBBY');
  assert.equal(created.currentRound, 1);

  // 2. Two players join; invalid room rejected
  await assert.rejects(() =>
    joinRoom({ roomCode: 'ZZZZZZ', playerId: 'p2', nickname: 'Priya' })
  );
  const afterJoin = await joinRoom({
    roomCode: created.roomCode,
    playerId: 'p2',
    nickname: 'Priya',
  });
  await joinRoom({
    roomCode: created.roomCode,
    playerId: 'p3',
    nickname: 'Arjun',
  });
  assert.equal(Object.keys(afterJoin.players).length, 2);

  // 3. Host finalizes a 5-personality subset (max selectable N = 5)
  const locked = await finalizeDeck({
    roomCode: created.roomCode,
    hostId: 'host_1',
    selectedIds: [10, 20, 30, 40, 50],
    startPlaying: true,
  });
  assert.equal(locked.deckIds.length, 5);
  assert.equal(locked.status, 'PLAYING');

  // Non-host cannot modify deck
  await assert.rejects(() =>
    finalizeDeck({
      roomCode: created.roomCode,
      hostId: 'p2',
      selectedIds: [1, 2],
      startPlaying: true,
    })
  );

  // 4. Card picking strictly enforced 1..5 and ALWAYS hidden by default
  await assert.rejects(() =>
    pickCard({ roomCode: created.roomCode, playerId: 'p2', cardNumber: 0 })
  );
  await assert.rejects(() =>
    pickCard({ roomCode: created.roomCode, playerId: 'p2', cardNumber: 6 })
  );

  const hostPicked = await pickCard({ roomCode: created.roomCode, playerId: 'host_1', cardNumber: 1 });
  const p2Picked = await pickCard({ roomCode: created.roomCode, playerId: 'p2', cardNumber: 3 });
  const p3Picked = await pickCard({ roomCode: created.roomCode, playerId: 'p3', cardNumber: 5 });
  assert.equal(hostPicked.cardVisible, false);
  assert.equal(p2Picked.cardVisible, false);
  assert.equal(p3Picked.cardVisible, false);

  const p3OriginalPersonalityId = locked.deckIds[5 - 1];
  const card3PersonalityId = locked.deckIds[3 - 1];

  // 5. Display / Hide toggle
  const displayed = await setCardVisibility({
    roomCode: created.roomCode,
    playerId: 'p2',
    visible: true,
  });
  assert.equal(displayed.cardVisible, true);

  // 6. Host eliminates card #3 (held by p2):
  //    - host (#1, below #3) stays #1
  //    - p2 (#3, equal to #3) resets to 0 and hidden!
  //    - p3 (#5, above #3) shifts to #4 and STILL points to exact same personality!
  const afterElim = await eliminatePersonalityFromPile({
    roomCode: created.roomCode,
    hostId: 'host_1',
    personalityId: card3PersonalityId,
  });
  assert.equal(afterElim.deckIds.length, 4);
  assert.deepEqual(afterElim.eliminatedIds, [card3PersonalityId]);
  assert.equal(afterElim.players.host_1.selectedCardNumber, 1);
  assert.equal(afterElim.players.p2.selectedCardNumber, 0);
  assert.equal(afterElim.players.p2.cardVisible, false);
  assert.equal(afterElim.players.p3.selectedCardNumber, 4);
  assert.equal(afterElim.deckIds[4 - 1], p3OriginalPersonalityId);

  // Non-host cannot eliminate
  await assert.rejects(() =>
    eliminatePersonalityFromPile({
      roomCode: created.roomCode,
      hostId: 'p2',
      personalityId: afterElim.deckIds[0],
    })
  );

  // Restore eliminated personality
  const afterRestore = await restoreEliminatedPersonality({
    roomCode: created.roomCode,
    hostId: 'host_1',
    personalityId: card3PersonalityId,
  });
  assert.equal(afterRestore.deckIds.length, 5);
  assert.equal(afterRestore.eliminatedIds.length, 0);

  // 7. Questions clamping & Round 1 guess completion (Score = 15 - Q)
  const qUpdated = await updateQuestions({
    roomCode: created.roomCode,
    playerId: 'host_1',
    roundQuestions: 99,
    totalQuestions: 7,
  });
  assert.equal(qUpdated.roundQuestions, 5); // clamped at 5
  assert.equal(qUpdated.totalQuestions, 7);

  const hostR1 = await completeGuess({
    roomCode: created.roomCode,
    playerId: 'host_1',
    guessedCorrectly: true,
  });
  assert.equal(hostR1.round1Score, 8); // 15 - 7 = 8
  assert.equal(hostR1.totalScore, 8);
  assert.equal(hostR1.cardVisible, false);

  await updateQuestions({
    roomCode: created.roomCode,
    playerId: 'p3',
    roundQuestions: 5,
    totalQuestions: 15,
  });
  const p3R1 = await completeGuess({
    roomCode: created.roomCode,
    playerId: 'p3',
    guessedCorrectly: false,
  });
  assert.equal(p3R1.round1Score, 0);
  assert.equal(p3R1.totalScore, 0);

  // 8. Host manual score edit works & non-host is rejected
  const editedP2 = await hostRecordScore({
    roomCode: created.roomCode,
    hostId: 'host_1',
    targetPlayerId: 'p2',
    round: 1,
    score: 12,
  });
  assert.equal(editedP2.round1Score, 12);
  assert.equal(editedP2.totalScore, 12);
  await assert.rejects(() =>
    hostRecordScore({
      roomCode: created.roomCode,
      hostId: 'p2',
      targetPlayerId: 'p2',
      round: 1,
      score: 15,
    })
  );

  // 9. Advance to Round 2 with eliminateUsedCards = true
  const round2 = await advanceRound({
    roomCode: created.roomCode,
    hostId: 'host_1',
    nextRound: 2,
    eliminateUsedCards: true,
  });
  assert.equal(round2.currentRound, 2);
  // host (#1) and p3 (#4) were picked -> 2 personalities auto-eliminated, leaving 3
  assert.equal(round2.deckIds.length, 3);
  assert.equal(round2.eliminatedIds.length, 2);
  assert.equal(round2.players.host_1.selectedCardNumber, 0);
  assert.equal(round2.players.host_1.round1Score, 8); // preserved!
  assert.equal(round2.players.p2.round1Score, 12); // preserved!

  // 10. Advance to Round 3 & record cumulative scores
  await advanceRound({
    roomCode: created.roomCode,
    hostId: 'host_1',
    nextRound: 3,
    eliminateUsedCards: false,
  });
  const hostR3 = await hostRecordScore({
    roomCode: created.roomCode,
    hostId: 'host_1',
    targetPlayerId: 'host_1',
    round: 3,
    score: 11,
  });
  assert.equal(hostR3.totalScore, 19); // 8 + 0 + 11

  // 11. Finish match blocks new joins
  const finished = await finishMatch({ roomCode: created.roomCode, hostId: 'host_1' });
  assert.equal(finished.status, 'FINISHED');
  await assert.rejects(() =>
    joinRoom({ roomCode: created.roomCode, playerId: 'p4', nickname: 'Late' })
  );

  // 12. Start New Game resets scores, restores eliminated when requested, and shuffles
  const restarted = await startNewGame({
    roomCode: created.roomCode,
    hostId: 'host_1',
    restoreEliminated: true,
  });
  assert.equal(restarted.status, 'PLAYING');
  assert.equal(restarted.currentRound, 1);
  assert.equal(restarted.deckIds.length, 5);
  assert.equal(restarted.eliminatedIds.length, 0);
  assert.equal(restarted.players.host_1.totalScore, 0);
  assert.equal(restarted.players.p2.totalScore, 0);

  // 13. Cannot eliminate the last remaining personality
  let latest = restarted;
  while (latest.deckIds.length > 1) {
    latest = await eliminatePersonalityFromPile({
      roomCode: created.roomCode,
      hostId: 'host_1',
      personalityId: latest.deckIds[0],
    });
  }
  await assert.rejects(() =>
    eliminatePersonalityFromPile({
      roomCode: created.roomCode,
      hostId: 'host_1',
      personalityId: latest.deckIds[0],
    })
  );

  // 14. 60-minute inactivity auto-clear deletes stale room automatically
  const key = `gtp_room_${created.roomCode}`;
  const stored = JSON.parse(localStorage.getItem(key));
  stored.lastActiveAt = Date.now() - SESSION_TIMEOUT_MS - 5000;
  localStorage.setItem(key, JSON.stringify(stored));

  await assert.rejects(() =>
    joinRoom({ roomCode: created.roomCode, playerId: 'p5', nickname: 'AfterHour' })
  );
  assert.equal(localStorage.getItem(key), null, 'Expired room must be auto-cleared from storage');
});

test('Edge cases: rejoin preserves score, all-cards-used round advance guard, and startNewGame keeping eliminated', async () => {
  store.clear();
  const created = await createRoom({
    roomName: 'Edge Case Room',
    hostId: 'h1',
    hostNickname: 'HostA',
    initialDeckIds: [1, 2],
  });
  await joinRoom({ roomCode: created.roomCode, playerId: 'p1', nickname: 'PlayerOne' });
  await finalizeDeck({
    roomCode: created.roomCode,
    hostId: 'h1',
    selectedIds: [1, 2],
    startPlaying: true,
  });

  // Both players pick the only 2 cards in the deck
  await pickCard({ roomCode: created.roomCode, playerId: 'h1', cardNumber: 1 });
  await pickCard({ roomCode: created.roomCode, playerId: 'p1', cardNumber: 2 });
  await completeGuess({ roomCode: created.roomCode, playerId: 'p1', guessedCorrectly: true });

  // Rejoining as p1 updates nickname without wiping round1Score (15)
  const rejoined = await joinRoom({
    roomCode: created.roomCode,
    playerId: 'p1',
    nickname: 'PlayerOneRenamed',
  });
  assert.equal(rejoined.players.p1.nickname, 'PlayerOneRenamed');
  assert.equal(rejoined.players.p1.round1Score, 15);
  assert.equal(rejoined.players.p1.totalScore, 15);

  // Advancing round with eliminateUsedCards=true when ALL cards were used safely keeps the 2 cards
  const r2 = await advanceRound({
    roomCode: created.roomCode,
    hostId: 'h1',
    nextRound: 2,
    eliminateUsedCards: true,
  });
  assert.equal(r2.deckIds.length, 2);
  assert.equal(r2.currentRound, 2);

  // Eliminate 1 card, finish match, startNewGame with restoreEliminated=false keeps it eliminated
  await eliminatePersonalityFromPile({
    roomCode: created.roomCode,
    hostId: 'h1',
    personalityId: r2.deckIds[0],
  });
  await finishMatch({ roomCode: created.roomCode, hostId: 'h1' });
  const freshKeepElim = await startNewGame({
    roomCode: created.roomCode,
    hostId: 'h1',
    restoreEliminated: false,
  });
  assert.equal(freshKeepElim.deckIds.length, 1);
  assert.equal(freshKeepElim.eliminatedIds.length, 1);
  assert.equal(freshKeepElim.players.p1.totalScore, 0);
});
