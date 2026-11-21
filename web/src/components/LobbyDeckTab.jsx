import React, { useMemo, useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import {
  CheckCircle2,
  Copy,
  Crown,
  Eye,
  Lock,
  Play,
  RotateCcw,
  Share2,
  SlidersHorizontal,
  Trash2,
} from 'lucide-react';
import { buildJoinUrl, getInitials } from '../utils/gameUtils.js';

export default function LobbyDeckTab({
  room,
  playerId,
  isHost,
  personalityById,
  totalCatalogCount = 100,
  imageMap = {},
  onOpenDeckBuilder,
  onFinalizeAndStart,
  onEliminateFromPile,
  onRestoreToPile,
  onGoToForehead,
  onNotify,
}) {
  const [showPileManager, setShowPileManager] = useState(false);
  const [pileSearch, setPileSearch] = useState('');

  const joinUrl = useMemo(() => buildJoinUrl(room.roomCode), [room.roomCode]);
  const activeDeckIds = room.deckIds || [];
  const eliminatedIds = room.eliminatedIds || [];

  const playersList = useMemo(() => {
    return Object.entries(room.players || {}).sort(([uidA, a], [uidB, b]) => {
      if (uidA === room.hostId) return -1;
      if (uidB === room.hostId) return 1;
      return (b.totalScore || 0) - (a.totalScore || 0);
    });
  }, [room.players, room.hostId]);

  // Sort alphabetically and hide assigned card numbers so secret 1..N mappings are never spoiled
  const activePersonalities = useMemo(() => {
    const q = pileSearch.trim().toLowerCase();
    return activeDeckIds
      .map((id) => personalityById[id])
      .filter((person) => {
        if (!person) return false;
        if (!q) return true;
        return (
          person.name.toLowerCase().includes(q) ||
          (person.tamil_name || '').toLowerCase().includes(q)
        );
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [activeDeckIds, personalityById, pileSearch]);

  const eliminatedPersonalities = useMemo(
    () => eliminatedIds.map((id) => personalityById[id]).filter(Boolean),
    [eliminatedIds, personalityById]
  );

  const copyText = async (text, label) => {
    try {
      await navigator.clipboard.writeText(text);
      onNotify(label);
    } catch {
      onNotify(`Copy manually: ${text}`);
    }
  };

  return (
    <div className="space-y-5">
      {/* Iron Thumbs-style QR & Room Code Invite */}
      <div className="rounded-3xl bg-[#120C21] p-6 text-center text-white shadow-lg">
        <p className="font-mono text-xs font-bold uppercase tracking-[0.2em] text-amber-400">
          Scan to Join — Zero Sign-In Needed
        </p>
        <div className="mt-2 font-mono text-4xl font-extrabold tracking-[0.25em]">
          {room.roomCode}
        </div>
        <div className="mx-auto mt-4 inline-block rounded-2xl bg-white p-3.5 shadow-md">
          <QRCodeSVG value={joinUrl} size={184} fgColor="#1D1528" bgColor="#FFFFFF" />
        </div>
        <p className="mt-3 break-all font-mono text-xs text-purple-200/80">{joinUrl}</p>

        <div className="mt-4 grid grid-cols-2 gap-2.5">
          <button
            onClick={() => copyText(room.roomCode, `Copied room code ${room.roomCode}`)}
            className="flex items-center justify-center gap-2 rounded-xl border border-amber-400/60 py-2.5 text-sm font-bold text-amber-300 hover:bg-amber-400/10"
          >
            <Copy className="h-4 w-4" />
            Copy Code
          </button>
          <button
            onClick={() => copyText(joinUrl, 'Copied shareable join link!')}
            className="flex items-center justify-center gap-2 rounded-xl bg-amber-400 py-2.5 text-sm font-bold text-[#120C21] hover:bg-amber-300"
          >
            <Share2 className="h-4 w-4" />
            Copy Join Link
          </button>
        </div>
      </div>

      {/* Host Pile & Elimination Controls */}
      <div className="rounded-3xl border border-[#E7DAC6] bg-white p-5 shadow-sm">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="font-display text-lg font-bold text-[#1D1528]">
              Active Pile: {activeDeckIds.length} Personalities
            </h3>
            <p className="mt-0.5 text-xs text-[#5A4E6C]">
              Max selectable card number is strictly <strong>1–{activeDeckIds.length}</strong>.
              {eliminatedIds.length > 0 && (
                <span> ({eliminatedIds.length} eliminated from pile)</span>
              )}
            </p>
          </div>
          <span
            className={`rounded-lg px-2.5 py-1 font-mono text-xs font-bold ${
              room.deckFinalized
                ? 'bg-teal-100 text-teal-900'
                : 'bg-amber-100 text-amber-900'
            }`}
          >
            {room.deckFinalized ? 'LOCKED' : 'DRAFT'}
          </span>
        </div>

        {isHost && room.status !== 'FINISHED' ? (
          <div className="mt-4 space-y-2.5">
            <div className="grid grid-cols-2 gap-2.5">
              <button
                onClick={onOpenDeckBuilder}
                className="flex items-center justify-center gap-2 rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] py-2.5 text-sm font-bold text-[#1D1528] hover:border-[#D9480F]"
              >
                <SlidersHorizontal className="h-4 w-4" />
                Pick Subset ({totalCatalogCount})
              </button>
              <button
                onClick={onFinalizeAndStart}
                className="flex items-center justify-center gap-2 rounded-xl bg-[#D9480F] py-2.5 text-sm font-bold text-white hover:bg-[#B83B0B]"
              >
                <Play className="h-4 w-4" />
                {room.status === 'PLAYING' ? 'Reshuffle Pile' : 'Finalize & Start'}
              </button>
            </div>

            {/* Host Eliminate Personalities From Pile Drawer */}
            <button
              onClick={() => setShowPileManager((v) => !v)}
              className="flex w-full items-center justify-between rounded-xl border border-rose-200 bg-rose-50/70 px-4 py-2.5 text-left text-sm font-bold text-rose-900 hover:bg-rose-100/70"
            >
              <span className="flex items-center gap-2">
                <Trash2 className="h-4 w-4 text-rose-600" />
                Eliminate Personalities from Active Pile ({activeDeckIds.length})
              </span>
              <span className="text-xs">{showPileManager ? 'Hide ▲' : 'Manage ▼'}</span>
            </button>

            {showPileManager && (
              <div className="rounded-2xl border border-[#E7DAC6] bg-[#FFFBF2] p-3.5 space-y-3">
                <p className="text-xs text-[#5A4E6C]">
                  Eliminate any personality from the active pile. Listed A–Z with card numbers hidden so secret <strong>1–{activeDeckIds.length}</strong> assignments are never spoiled.
                </p>
                <input
                  type="text"
                  value={pileSearch}
                  onChange={(e) => setPileSearch(e.target.value)}
                  placeholder="Filter active pile by name..."
                  className="w-full rounded-xl border border-[#DFCEB8] bg-white px-3 py-2 text-sm outline-none focus:border-[#D9480F]"
                />

                <div className="max-h-64 space-y-1.5 overflow-y-auto pr-1">
                  {activePersonalities.map((person) => (
                    <div
                      key={person.id}
                      className="flex items-center justify-between gap-2 rounded-xl border border-[#E7DAC6] bg-white px-3 py-2"
                    >
                      <div className="flex min-w-0 flex-1 items-center gap-2.5">
                        <div className="relative flex h-9 w-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-[#1D1533] font-display text-xs font-bold text-amber-400">
                          <span>{getInitials(person.name)}</span>
                          {((person.image_url && person.image_url.trim()) || imageMap[person.id]) && (
                            <img
                              src={(person.image_url && person.image_url.trim()) || imageMap[person.id]}
                              alt={person.name}
                              loading="lazy"
                              referrerPolicy="no-referrer"
                              onError={(e) => {
                                e.currentTarget.style.display = 'none';
                              }}
                              className="absolute inset-0 h-full w-full object-cover object-top"
                            />
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="truncate text-sm font-semibold text-[#1D1528]">
                            {person.name}
                          </div>
                          <div className="truncate text-[11px] text-[#5A4E6C]">
                            {person.tamil_name ? `${person.tamil_name} • ` : ''}
                            {person.region} • {person.category}
                          </div>
                        </div>
                      </div>
                      <button
                        onClick={() => onEliminateFromPile(person.id, person.name)}
                        disabled={activeDeckIds.length <= 1}
                        className="flex shrink-0 items-center gap-1 rounded-lg bg-rose-100 px-2.5 py-1.5 text-xs font-bold text-rose-800 hover:bg-rose-200 disabled:opacity-40"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        Eliminate
                      </button>
                    </div>
                  ))}
                </div>

                {eliminatedPersonalities.length > 0 && (
                  <div className="border-t border-[#E7DAC6] pt-3">
                    <p className="mb-2 text-xs font-bold uppercase tracking-wider text-[#5A4E6C]">
                      Eliminated Pile ({eliminatedPersonalities.length})
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {eliminatedPersonalities.map((person) => (
                        <button
                          key={person.id}
                          onClick={() => onRestoreToPile(person.id, person.name)}
                          className="inline-flex items-center gap-1.5 rounded-full border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-700 hover:border-teal-600 hover:text-teal-800"
                          title="Tap to restore back into active pile"
                        >
                          <span className="line-through opacity-75">{person.name}</span>
                          <RotateCcw className="h-3 w-3 text-teal-700" />
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        ) : (
          <button
            onClick={onGoToForehead}
            className="mt-4 w-full rounded-xl bg-[#D9480F] py-3 text-sm font-bold text-white hover:bg-[#B83B0B]"
          >
            Go to Forehead Play (Pick 1–{activeDeckIds.length})
          </button>
        )}
      </div>

      {/* Live Players Roster */}
      <div className="space-y-2.5">
        <div className="flex items-center justify-between px-1">
          <h3 className="font-display text-lg font-bold text-[#1D1528]">
            Players ({playersList.length})
          </h3>
          <span className="font-mono text-xs font-bold text-[#D9480F]">
            Round {room.currentRound} of 3
          </span>
        </div>

        {playersList.map(([uid, p]) => {
          const isMe = uid === playerId;
          const isRoomHost = uid === room.hostId;
          const status =
            !p.selectedCardNumber
              ? 'Picking a number...'
              : p.guessCompleted && p.guessedCorrectly
                ? `Card #${p.selectedCardNumber} • Guessed in ${p.totalQuestions} Qs`
                : p.guessCompleted
                  ? `Card #${p.selectedCardNumber} • Unguessed (0 pts)`
                  : p.cardVisible
                    ? `Card #${p.selectedCardNumber} • DISPLAYING ON FOREHEAD`
                    : `Card #${p.selectedCardNumber} • Hidden (${p.totalQuestions}/15 Qs)`;

          return (
            <div
              key={uid}
              className={`flex items-center justify-between rounded-2xl border p-3.5 ${
                isMe
                  ? 'border-[#D9480F] bg-[#FFE4D6]/35'
                  : 'border-[#E7DAC6] bg-white'
              }`}
            >
              <div className="min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="truncate font-bold text-[#1D1528]">
                    {p.nickname} {isMe && '(You)'}
                  </span>
                  {isRoomHost && <Crown className="h-4 w-4 text-amber-500" />}
                </div>
                <p className="text-xs text-[#5A4E6C]">{status}</p>
              </div>
              <div className="flex items-center gap-2">
                {p.guessCompleted ? (
                  <CheckCircle2 className="h-4 w-4 text-teal-600" />
                ) : p.cardVisible ? (
                  <Eye className="h-4 w-4 text-[#D9480F]" />
                ) : (
                  <Lock className="h-4 w-4 text-stone-400" />
                )}
                <span className="rounded-lg bg-[#F6EDE0] px-2.5 py-1 font-mono text-xs font-bold text-[#1D1528]">
                  {p.totalScore || 0} pts
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
