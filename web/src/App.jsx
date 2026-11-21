import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  BookOpen,
  Clock,
  Eye,
  HelpCircle,
  LogOut,
  PlusCircle,
  QrCode,
  ScanLine,
  Sparkles,
  Trophy,
  Users,
} from 'lucide-react';
import personalitiesData from './data/personalities.json';
import DeckBuilderModal from './components/DeckBuilderModal.jsx';
import LobbyDeckTab from './components/LobbyDeckTab.jsx';
import ForeheadPlayTab from './components/ForeheadPlayTab.jsx';
import PointsTab from './components/PointsTab.jsx';
import InfoTab from './components/InfoTab.jsx';
import QrScannerModal from './components/QrScannerModal.jsx';
import {
  advanceRound,
  createRoom,
  eliminatePersonalityFromPile,
  finalizeDeck,
  finishMatch,
  hostRecordScore,
  isFirebaseConfigured,
  joinRoom,
  pickCard,
  restoreEliminatedPersonality,
  setCardVisibility,
  startNewGame,
  subscribeRoom,
  updateQuestions,
  completeGuess,
} from './services/roomService.js';
import {
  formatRemainingSession,
  getOrCreateBrowserPlayerId,
  normalizeRoomCode,
  shuffleArray,
} from './utils/gameUtils.js';
import { prefetchPersonalityImages } from './utils/wikiImage.js';

export default function App() {
  const playerId = useMemo(() => getOrCreateBrowserPlayerId(), []);
  const personalityById = useMemo(() => {
    const map = {};
    for (const p of personalitiesData) map[p.id] = p;
    return map;
  }, []);
  const { totalCount, tamilCount, panIndianCount } = useMemo(() => {
    const tamil = personalitiesData.filter((p) => p.region === 'Tamil').length;
    return {
      totalCount: personalitiesData.length,
      tamilCount: tamil,
      panIndianCount: personalitiesData.length - tamil,
    };
  }, []);

  const [imageMap, setImageMap] = useState({});

  useEffect(() => {
    let active = true;
    prefetchPersonalityImages(personalitiesData).then((resolved) => {
      if (active) setImageMap(resolved);
    });
    return () => {
      active = false;
    };
  }, []);

  const [activeRoomCode, setActiveRoomCode] = useState(null);
  const [room, setRoom] = useState(null);
  const [expiredNotice, setExpiredNotice] = useState(false);
  const [activeTab, setActiveTab] = useState('lobby'); // 'lobby' | 'play' | 'points' | 'info'
  const [toast, setToast] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [showDeckModal, setShowDeckModal] = useState(false);
  const [showCatalogModal, setShowCatalogModal] = useState(false);
  const [showHomeInfoModal, setShowHomeInfoModal] = useState(false);
  const [showQrScanner, setShowQrScanner] = useState(false);
  const [nowTick, setNowTick] = useState(Date.now());
  const joinNickInputRef = useRef(null);

  // Home form state (ZERO sign-in required!)
  const [roomName, setRoomName] = useState('Chennai Party Night');
  const [hostNick, setHostNick] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const [joinNick, setJoinNick] = useState('');
  const [busy, setBusy] = useState(false);

  // Read ?room=ABCDEF from QR code / shared link automatically
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const fromUrl = normalizeRoomCode(params.get('room'));
    if (fromUrl) {
      setJoinCode(fromUrl);
    }
  }, []);

  // Live 60-minute inactivity countdown refresh every 30s
  useEffect(() => {
    const id = setInterval(() => setNowTick(Date.now()), 30000);
    return () => clearInterval(id);
  }, []);

  // Subscribe to active room locked strictly by roomCode
  useEffect(() => {
    if (!activeRoomCode) {
      setRoom(null);
      return;
    }
    const unsub = subscribeRoom(
      activeRoomCode,
      (data) => {
        setRoom(data);
        if (!data) {
          setErrorMsg('This room session is no longer active.');
        }
      },
      () => {
        setRoom(null);
        setActiveRoomCode(null);
        setExpiredNotice(true);
      }
    );
    return unsub;
  }, [activeRoomCode]);

  const safeReplaceUrl = (nextUrl) => {
    try {
      window.history.replaceState({}, '', nextUrl);
    } catch {
      // Ignore in restricted webviews
    }
  };

  const notify = (msg) => {
    setErrorMsg('');
    setToast(msg);
    setTimeout(() => setToast((cur) => (cur === msg ? '' : cur)), 3200);
  };

  const handleCreateRoom = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setExpiredNotice(false);
    setBusy(true);
    try {
      const initialIds = shuffleArray(personalitiesData.map((p) => p.id));
      const created = await createRoom({
        roomName,
        hostId: playerId,
        hostNickname: hostNick || 'Host',
        initialDeckIds: initialIds,
      });
      setActiveRoomCode(created.roomCode);
      setActiveTab('lobby');
      safeReplaceUrl(`?room=${created.roomCode}`);
    } catch (err) {
      setErrorMsg(err.message || 'Could not create room.');
    } finally {
      setBusy(false);
    }
  };

  const handleJoinRoom = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setExpiredNotice(false);
    setBusy(true);
    try {
      const joined = await joinRoom({
        roomCode: joinCode,
        playerId,
        nickname: joinNick || 'Player',
      });
      setActiveRoomCode(joined.roomCode);
      setActiveTab(joined.status === 'PLAYING' ? 'play' : 'lobby');
      safeReplaceUrl(`?room=${joined.roomCode}`);
    } catch (err) {
      setErrorMsg(err.message || 'Could not join room.');
    } finally {
      setBusy(false);
    }
  };

  const leaveSession = () => {
    setErrorMsg('');
    setActiveRoomCode(null);
    setRoom(null);
    setActiveTab('lobby');
    safeReplaceUrl(window.location.pathname);
  };

  const isHost = room?.hostId === playerId;

  return (
    <div className="min-h-screen bg-[#FFFBF2] text-[#1D1528]">
      {/* Top Bar */}
      <header className="sticky top-0 z-30 border-b border-[#E7DAC6] bg-[#FFFBF2]/95 backdrop-blur">
        <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-3">
          <div>
            <div className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-[#D9480F]" />
              <span className="font-display text-lg font-extrabold tracking-tight">
                Guess the Personality
              </span>
            </div>
            {room && (
              <div className="mt-0.5 flex items-center gap-2 font-mono text-[11px] text-[#5A4E6C]">
                <span className="font-bold text-[#D9480F]">ROOM {room.roomCode}</span>
                <span>•</span>
                <span className="inline-flex items-center gap-1">
                  <Clock className="h-3 w-3" />
                  Auto-clears in {formatRemainingSession(room, nowTick)}
                </span>
              </div>
            )}
          </div>

          {room ? (
            <button
              onClick={leaveSession}
              className="inline-flex items-center gap-1.5 rounded-xl border border-[#DFCEB8] bg-white px-3 py-1.5 text-xs font-bold text-[#5A4E6C] hover:border-rose-400 hover:text-rose-700"
            >
              <LogOut className="h-3.5 w-3.5" />
              Exit
            </button>
          ) : (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowHomeInfoModal(true)}
                className="inline-flex items-center gap-1 rounded-xl border border-[#DFCEB8] bg-white px-2.5 py-1.5 text-xs font-bold text-[#1D1528] hover:border-[#D9480F]"
              >
                <HelpCircle className="h-3.5 w-3.5 text-[#0F766E]" />
                Info
              </button>
              <button
                onClick={() => setShowCatalogModal(true)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#DFCEB8] bg-white px-2.5 py-1.5 text-xs font-bold text-[#1D1528] hover:border-[#D9480F]"
              >
                <BookOpen className="h-3.5 w-3.5 text-[#D9480F]" />
                {totalCount} Icons
              </button>
            </div>
          )}
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 pb-24 pt-4">
        {(!room || activeTab !== 'info') &&
          (!isFirebaseConfigured ? (
            <div className="mb-4 rounded-2xl border border-amber-300 bg-amber-50 px-4 py-2.5 text-xs text-amber-950">
              <strong>Single-Device Mode (Local Only):</strong> Rooms created here are only saved on this browser. To join from other phones, add <code className="font-mono font-bold">VITE_FIREBASE_DATABASE_URL</code> in GitHub <strong>Settings → Secrets and variables → Actions</strong> (or <code className="font-mono">web/.env</code>) and redeploy.
            </div>
          ) : (
            <div className="mb-4 flex items-center gap-2 rounded-2xl border border-teal-200 bg-teal-50 px-4 py-2 text-xs font-semibold text-teal-900">
              <span className="h-2 w-2 rounded-full bg-teal-500" />
              Live Multi-Device Sync Connected (Zero Sign-In)
            </div>
          ))}

        {expiredNotice && (
          <div className="mb-4 rounded-2xl border border-rose-300 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-900">
            Room session was automatically cleared after 60 minutes of inactivity. Create a new room to play again!
          </div>
        )}

        {errorMsg && (
          <div className="mb-4 rounded-2xl border border-rose-300 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-900">
            {errorMsg}
          </div>
        )}

        {/* HOME VIEW (ZERO SIGN-IN) */}
        {!room ? (
          <div className="space-y-5">
            <div className="rounded-3xl bg-gradient-to-br from-[#120C21] via-[#241642] to-[#3B193F] p-6 text-white shadow-xl">
              <span className="inline-block rounded-full bg-amber-400 px-3 py-1 font-mono text-[11px] font-extrabold text-[#120C21]">
                ZERO SIGN-IN • 60-MIN AUTO-CLEAR SESSIONS
              </span>
              <h1 className="mt-3 font-display text-3xl font-extrabold leading-tight">
                Tamil &amp; Pan-Indian Forehead Party Game
              </h1>
              <p className="mt-2 text-sm text-purple-200/90">
                {tamilCount} Tamil + {panIndianCount} Pan-Indian legends. Create a room, scan the QR code, eliminate cards from the pile, and play 3 rounds (15 Yes/No questions max) straight in any phone browser.
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              {/* Join Room Form with In-Place QR Scanner */}
              <form
                onSubmit={handleJoinRoom}
                className="rounded-3xl border border-[#E7DAC6] bg-white p-5 shadow-sm space-y-3"
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 text-[#0F766E]">
                    <QrCode className="h-5 w-5" />
                    <h2 className="font-display text-lg font-bold text-[#1D1528]">Join a Room</h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setErrorMsg('');
                      setShowQrScanner(true);
                    }}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-teal-50 border border-teal-300 px-3 py-1.5 text-xs font-extrabold text-[#0F766E] hover:bg-teal-100"
                  >
                    <ScanLine className="h-4 w-4" />
                    Scan QR
                  </button>
                </div>
                <input
                  type="text"
                  required
                  value={joinCode}
                  onChange={(e) => setJoinCode(normalizeRoomCode(e.target.value))}
                  placeholder="Room Code (e.g. KVR7M2)"
                  className="w-full rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] px-3.5 py-2.5 font-mono text-sm font-bold uppercase tracking-widest outline-none focus:border-[#0F766E]"
                />
                <input
                  ref={joinNickInputRef}
                  type="text"
                  required
                  maxLength={30}
                  value={joinNick}
                  onChange={(e) => setJoinNick(e.target.value)}
                  placeholder="Your Nickname"
                  className="w-full rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] px-3.5 py-2.5 text-sm outline-none focus:border-[#0F766E]"
                />
                <button
                  type="submit"
                  disabled={busy || joinCode.length < 4 || !joinNick.trim()}
                  className="w-full rounded-xl bg-[#0F766E] py-3 text-sm font-bold text-white hover:bg-teal-800 disabled:opacity-40"
                >
                  {busy ? 'Joining...' : 'Join Room'}
                </button>
              </form>

              {/* Host Create Room Form */}
              <form
                onSubmit={handleCreateRoom}
                className="rounded-3xl border border-[#E7DAC6] bg-white p-5 shadow-sm space-y-3"
              >
                <div className="flex items-center gap-2 text-[#D9480F]">
                  <PlusCircle className="h-5 w-5" />
                  <h2 className="font-display text-lg font-bold text-[#1D1528]">Host New Room</h2>
                </div>
                <input
                  type="text"
                  required
                  maxLength={60}
                  value={roomName}
                  onChange={(e) => setRoomName(e.target.value)}
                  placeholder="Room Title"
                  className="w-full rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] px-3.5 py-2.5 text-sm outline-none focus:border-[#D9480F]"
                />
                <input
                  type="text"
                  required
                  maxLength={30}
                  value={hostNick}
                  onChange={(e) => setHostNick(e.target.value)}
                  placeholder="Host Nickname"
                  className="w-full rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] px-3.5 py-2.5 text-sm outline-none focus:border-[#D9480F]"
                />
                <button
                  type="submit"
                  disabled={busy || !roomName.trim() || !hostNick.trim()}
                  className="w-full rounded-xl bg-[#D9480F] py-3 text-sm font-bold text-white hover:bg-[#B83B0B] disabled:opacity-40"
                >
                  {busy ? 'Creating...' : 'Create Room & QR'}
                </button>
              </form>
            </div>
          </div>
        ) : (
          /* ACTIVE ROOM VIEW */
          <>
            {activeTab === 'lobby' && (
              <LobbyDeckTab
                room={room}
                playerId={playerId}
                isHost={isHost}
                personalityById={personalityById}
                totalCatalogCount={totalCount}
                imageMap={imageMap}
                onOpenDeckBuilder={() => setShowDeckModal(true)}
                onFinalizeAndStart={async () => {
                  await finalizeDeck({
                    roomCode: room.roomCode,
                    hostId: playerId,
                    selectedIds: room.deckIds,
                    startPlaying: true,
                  });
                  notify(`Locked & shuffled ${room.deckIds.length} personalities!`);
                  setActiveTab('play');
                }}
                onEliminateFromPile={async (pid, name) => {
                  try {
                    await eliminatePersonalityFromPile({
                      roomCode: room.roomCode,
                      hostId: playerId,
                      personalityId: pid,
                    });
                    notify(`Eliminated "${name}" from active pile.`);
                  } catch (err) {
                    setErrorMsg(err.message);
                  }
                }}
                onRestoreToPile={async (pid, name) => {
                  await restoreEliminatedPersonality({
                    roomCode: room.roomCode,
                    hostId: playerId,
                    personalityId: pid,
                  });
                  notify(`Restored "${name}" to active pile.`);
                }}
                onGoToForehead={() => setActiveTab('play')}
                onNotify={notify}
              />
            )}

            {activeTab === 'play' && (
              <ForeheadPlayTab
                room={room}
                playerId={playerId}
                isHost={isHost}
                personalityById={personalityById}
                imageMap={imageMap}
                onPickCard={async (num) => {
                  await pickCard({ roomCode: room.roomCode, playerId, cardNumber: num });
                  notify(`Card #${num} selected & hidden. Raise phone to forehead!`);
                }}
                onSetCardVisible={(visible) =>
                  setCardVisibility({ roomCode: room.roomCode, playerId, visible })
                }
                onUpdateQuestions={(rQ, tQ) =>
                  updateQuestions({
                    roomCode: room.roomCode,
                    playerId,
                    roundQuestions: rQ,
                    totalQuestions: tQ,
                  })
                }
                onCompleteGuess={async (correct) => {
                  await completeGuess({
                    roomCode: room.roomCode,
                    playerId,
                    guessedCorrectly: correct,
                  });
                  notify(correct ? 'Guess recorded in Points Tab!' : 'Marked unguessed (0 pts).');
                }}
                onEliminateCurrentCard={async (pid, name) => {
                  try {
                    await eliminatePersonalityFromPile({
                      roomCode: room.roomCode,
                      hostId: playerId,
                      personalityId: pid,
                    });
                    notify(`Eliminated "${name}" from the pile.`);
                  } catch (err) {
                    setErrorMsg(err.message);
                  }
                }}
              />
            )}

            {activeTab === 'points' && (
              <PointsTab
                room={room}
                playerId={playerId}
                isHost={isHost}
                onHostRecordScore={async (targetUid, round, score) => {
                  await hostRecordScore({
                    roomCode: room.roomCode,
                    hostId: playerId,
                    targetPlayerId: targetUid,
                    round,
                    score,
                  });
                  notify(`Saved Round ${round} score (${score} pts).`);
                }}
                onHostAdvanceRound={async (nextRound, eliminateUsed) => {
                  await advanceRound({
                    roomCode: room.roomCode,
                    hostId: playerId,
                    nextRound,
                    eliminateUsedCards: eliminateUsed,
                  });
                  notify(`Moved to Round ${nextRound}!`);
                }}
                onHostFinishMatch={async () => {
                  await finishMatch({ roomCode: room.roomCode, hostId: playerId });
                  notify('Match finished! Final standings locked.');
                }}
                onHostStartNewGame={async (restoreEliminated) => {
                  await startNewGame({
                    roomCode: room.roomCode,
                    hostId: playerId,
                    restoreEliminated,
                  });
                  notify('New game started with freshly randomized card numbers!');
                  setActiveTab('play');
                }}
              />
            )}

            {activeTab === 'info' && <InfoTab />}
          </>
        )}
      </main>

      {/* Bottom Tab Navigation inside Active Room */}
      {room && (
        <nav className="fixed bottom-0 left-0 right-0 z-30 border-t border-[#E7DAC6] bg-white/95 backdrop-blur">
          <div className="mx-auto grid max-w-2xl grid-cols-4">
            {[
              ['lobby', 'Lobby', Users],
              ['play', 'Play', Eye],
              ['points', 'Points', Trophy],
              ['info', 'Info', HelpCircle],
            ].map(([id, label, IconComp]) => {
              const active = activeTab === id;
              return (
                <button
                  key={id}
                  onClick={() => setActiveTab(id)}
                  className={`flex flex-col items-center justify-center gap-1 py-2.5 text-xs font-bold transition ${
                    active ? 'text-[#D9480F]' : 'text-[#5A4E6C]'
                  }`}
                >
                  <IconComp className="h-5 w-5" />
                  <span>{label}</span>
                </button>
              );
            })}
          </div>
        </nav>
      )}

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-20 left-1/2 z-40 -translate-x-1/2 rounded-full bg-[#120C21] px-4 py-2 text-xs font-bold text-amber-300 shadow-lg">
          {toast}
        </div>
      )}

      {/* Host Deck Builder Modal */}
      {showDeckModal && room && (
        <DeckBuilderModal
          allPersonalities={personalitiesData}
          initialSelectedIds={room.deckIds || []}
          imageMap={imageMap}
          selectionMode={true}
          onConfirm={async (chosenIds, startNow) => {
            await finalizeDeck({
              roomCode: room.roomCode,
              hostId: playerId,
              selectedIds: chosenIds,
              startPlaying: startNow,
            });
            setShowDeckModal(false);
            notify(`Deck updated (${chosenIds.length} personalities).`);
            if (startNow) setActiveTab('play');
          }}
          onClose={() => setShowDeckModal(false)}
        />
      )}

      {/* Catalog Modal */}
      {showCatalogModal && (
        <DeckBuilderModal
          allPersonalities={personalitiesData}
          initialSelectedIds={[]}
          imageMap={imageMap}
          selectionMode={false}
          onClose={() => setShowCatalogModal(false)}
        />
      )}

      {/* Home Page Info Rules Modal (No-Scroll Layman Guide) */}
      {showHomeInfoModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-3 backdrop-blur-sm"
          role="dialog"
          aria-modal="true"
          aria-label="How to Play"
          onClick={(e) => {
            if (e.target === e.currentTarget) setShowHomeInfoModal(false);
          }}
        >
          <div className="w-full max-w-md rounded-3xl bg-[#FFFBF2] p-3.5 shadow-2xl">
            <InfoTab onClose={() => setShowHomeInfoModal(false)} />
          </div>
        </div>
      )}

      {/* Home Page In-Place QR Scanner Modal */}
      {showQrScanner && (
        <QrScannerModal
          onDetected={(scannedCode) => {
            setShowQrScanner(false);
            setJoinCode(scannedCode);
            notify(`Scanned room ${scannedCode}! Enter your nickname and tap Join.`);
            setTimeout(() => joinNickInputRef.current?.focus(), 80);
          }}
          onClose={() => setShowQrScanner(false)}
        />
      )}
    </div>
  );
}
