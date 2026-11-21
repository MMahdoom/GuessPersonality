const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const HOST_UID = "host_user_001";
const PLAYER_UID = "player_user_002";
const OUTSIDER_UID = "outsider_user_003";
const ROOM_CODE = "KVR7M2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function makePlayer(nickname, overrides = {}) {
  return {
    nickname,
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
    ...overrides,
  };
}

function makeRoomPayload(createdAt, overrides = {}) {
  return {
    roomCode: ROOM_CODE,
    roomName: "Chennai Cinema Night",
    hostId: HOST_UID,
    hostName: "Karthik",
    status: "LOBBY",
    currentRound: 1,
    deckIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
    deckFinalized: false,
    memberUids: [HOST_UID],
    players: {
      [HOST_UID]: makePlayer("Karthik"),
    },
    createdAt,
    updatedAt: createdAt,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. UNAUTHENTICATED DENIALS ---

test("Unauthenticated users cannot read or write profiles or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(HOST_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(HOST_UID).set({
      userId: HOST_UID,
      nickname: "Anon",
      gamesPlayed: 0,
      totalPoints: 0,
      bestRoundScore: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("rooms").doc(ROOM_CODE).get());
  await assertFails(unauthDb.collection("rooms").get());
  await assertFails(
    unauthDb.collection("rooms").doc(ROOM_CODE).set(makeRoomPayload(now))
  );
});

// --- 2. USER PROFILE ISOLATION & VALIDATION ---

test("Authenticated owner can create and update own profile; cross-user access denied", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    hostDb.collection("users").doc(HOST_UID).set({
      userId: HOST_UID,
      nickname: "Karthik",
      gamesPlayed: 0,
      totalPoints: 0,
      bestRoundScore: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update
  await assertSucceeds(
    hostDb.collection("users").doc(HOST_UID).update({
      nickname: "Karthik M",
      gamesPlayed: 1,
      totalPoints: 34,
      bestRoundScore: 13,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject cross-user read & write
  await assertFails(playerDb.collection("users").doc(HOST_UID).get());
  await assertFails(
    playerDb.collection("users").doc(HOST_UID).update({
      nickname: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field (Shadow Update test)
  await assertFails(
    hostDb.collection("users").doc(HOST_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    hostDb.collection("users").doc(HOST_UID).update({
      nickname: "FutureUser",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject invalid bestRoundScore (> 15)
  await assertFails(
    hostDb.collection("users").doc(HOST_UID).update({
      bestRoundScore: 20,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject listing /users collection
  await assertFails(hostDb.collection("users").get());
});

// --- 3. ROOM CREATION, SCOPED LISTING & JOINING ---

test("Host creates room, filtered list succeeds, unfiltered list fails, player joins cleanly", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create valid room
  await assertSucceeds(
    hostDb.collection("rooms").doc(ROOM_CODE).set(makeRoomPayload(created))
  );

  // Reject spoofing hostId
  await assertFails(
    playerDb.collection("rooms").doc("FAKE99").set(
      makeRoomPayload(created, {
        roomCode: "FAKE99",
        hostId: HOST_UID,
      })
    )
  );

  // Scoped list query matches Kotlin repository: whereArrayContains("memberUids", uid)
  await assertSucceeds(
    hostDb.collection("rooms").where("memberUids", "array-contains", HOST_UID).get()
  );

  // Unfiltered list query MUST fail
  await assertFails(hostDb.collection("rooms").get());
  await assertFails(outsiderDb.collection("rooms").get());

  // Player looks up room by code and joins
  await assertSucceeds(playerDb.collection("rooms").doc(ROOM_CODE).get());
  await assertSucceeds(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      memberUids: [HOST_UID, PLAYER_UID],
      [`players.${PLAYER_UID}`]: makePlayer("Priya"),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Joined player can now list rooms they belong to
  await assertSucceeds(
    playerDb
      .collection("rooms")
      .where("memberUids", "array-contains", PLAYER_UID)
      .get()
  );
});

// --- 4. FOREHEAD DISPLAY, CARD BOUNDS & CROSS-PLAYER ISOLATION ---

test("Player picks valid card 1..N, toggles Display, cannot pick > N or edit another player", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("rooms")
      .doc(ROOM_CODE)
      .set(
        makeRoomPayload(created, {
          status: "PLAYING",
          deckIds: [1, 5, 12, 24, 33], // N = 5
          deckFinalized: true,
          memberUids: [HOST_UID, PLAYER_UID],
          players: {
            [HOST_UID]: makePlayer("Karthik"),
            [PLAYER_UID]: makePlayer("Priya"),
          },
        })
      );
  });

  // Player picks valid card #5 (<= N=5) with cardVisible = false (HIDDEN)
  await assertSucceeds(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      [`players.${PLAYER_UID}`]: makePlayer("Priya", {
        selectedCardNumber: 5,
        cardVisible: false,
      }),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Player taps Display -> cardVisible = true, asks 3 questions, guesses correctly (Score = 15 - 3 = 12)
  await assertSucceeds(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      [`players.${PLAYER_UID}`]: makePlayer("Priya", {
        selectedCardNumber: 5,
        cardVisible: true,
        roundQuestions: 3,
        totalQuestions: 3,
        guessCompleted: true,
        guessedCorrectly: true,
        round1Score: 12,
        totalScore: 12,
      }),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // REJECT picking card #6 when finalized deck has only N = 5 cards!
  await assertFails(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      [`players.${PLAYER_UID}`]: makePlayer("Priya", {
        selectedCardNumber: 6,
        cardVisible: false,
      }),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // REJECT player tampering with host's player state!
  await assertFails(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      [`players.${HOST_UID}`]: makePlayer("HackedHost", {
        round1Score: 15,
        totalScore: 15,
      }),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // REJECT non-host changing room status or deckIds!
  await assertFails(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      status: "FINISHED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // REJECT inconsistent totalScore (must equal r1 + r2 + r3)
  await assertFails(
    playerDb.collection("rooms").doc(ROOM_CODE).update({
      [`players.${PLAYER_UID}`]: makePlayer("Priya", {
        round1Score: 10,
        totalScore: 45,
      }),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

// --- 5. HOST DECK FINALIZATION, POINTS TAB, TERMINAL STATE LOCKING & LEAVE ---

test("Host updates Points Tab scores, finishes match (locking updates), and only host can delete", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("rooms")
      .doc(ROOM_CODE)
      .set(
        makeRoomPayload(created, {
          status: "PLAYING",
          currentRound: 2,
          deckFinalized: true,
          memberUids: [HOST_UID, PLAYER_UID],
          players: {
            [HOST_UID]: makePlayer("Karthik", { round1Score: 11, totalScore: 11 }),
            [PLAYER_UID]: makePlayer("Priya", { round1Score: 13, totalScore: 13 }),
          },
        })
      );
  });

  // Host records Round 2 score for Priya in Points Tab
  await assertSucceeds(
    hostDb.collection("rooms").doc(ROOM_CODE).update({
      [`players.${PLAYER_UID}`]: makePlayer("Priya", {
        round1Score: 13,
        round2Score: 10,
        totalScore: 23,
      }),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Non-host cannot delete room
  await assertFails(playerDb.collection("rooms").doc(ROOM_CODE).delete());

  // Host marks match FINISHED
  await assertSucceeds(
    hostDb.collection("rooms").doc(ROOM_CODE).update({
      status: "FINISHED",
      currentRound: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: NO further updates permitted after FINISHED
  await assertFails(
    hostDb.collection("rooms").doc(ROOM_CODE).update({
      roomName: "Reopened?",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Host can still delete finished room
  await assertSucceeds(hostDb.collection("rooms").doc(ROOM_CODE).delete());
});
