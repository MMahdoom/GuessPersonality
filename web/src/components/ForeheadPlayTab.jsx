import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  Dices,
  Eye,
  EyeOff,
  Lock,
  Minus,
  Plus,
  RotateCcw,
  Trash2,
} from 'lucide-react';
import { calculateScore, getInitials } from '../utils/gameUtils.js';

export default function ForeheadPlayTab({
  room,
  playerId,
  isHost,
  personalityById,
  onPickCard,
  onSetCardVisible,
  onUpdateQuestions,
  onCompleteGuess,
  onEliminateCurrentCard,
}) {
  const [imgFailed, setImgFailed] = useState(false);

  const myPlayer = room.players?.[playerId] || {};
  const deckIds = room.deckIds || [];
  const maxN = deckIds.length;
  const pickedNum = Number(myPlayer.selectedCardNumber || 0);
  const assignedId = pickedNum >= 1 && pickedNum <= maxN ? deckIds[pickedNum - 1] : null;
  const personality = assignedId ? personalityById[assignedId] : null;
  const isFinished = room.status === 'FINISHED';

  // Reset image error state whenever assigned personality changes
  useEffect(() => {
    setImgFailed(false);
  }, [assignedId]);

  // Screen Wake Lock while displaying card on forehead
  useEffect(() => {
    let wakeLock = null;
    if (myPlayer.cardVisible && 'wakeLock' in navigator) {
      navigator.wakeLock
        .request('screen')
        .then((lock) => {
          wakeLock = lock;
        })
        .catch(() => {});
    }
    return () => {
      wakeLock?.release?.().catch(() => {});
    };
  }, [myPlayer.cardVisible]);

  const takenByOthers = useMemo(() => {
    const map = {};
    for (const [uid, p] of Object.entries(room.players || {})) {
      if (uid !== playerId && p.selectedCardNumber > 0) {
        map[p.selectedCardNumber] = p.nickname;
      }
    }
    return map;
  }, [room.players, playerId]);

  const roundQ = Number(myPlayer.roundQuestions || 0);
  const totalQ = Number(myPlayer.totalQuestions || 0);
  const potentialScore = calculateScore(totalQ, true);

  const handleRandomPick = () => {
    if (maxN <= 0 || isFinished) return;
    const available = [];
    for (let i = 1; i <= maxN; i++) {
      if (!takenByOthers[i]) available.push(i);
    }
    const pool = available.length ? available : Array.from({ length: maxN }, (_, i) => i + 1);
    const chosen = pool[Math.floor(Math.random() * pool.length)];
    onPickCard(chosen);
  };

  return (
    <div className="space-y-5">
      {/* 1. FOREHEAD DISPLAY STAGE (HIDDEN BY DEFAULT -> DISPLAY -> HIDE / LOCK) */}
      {!pickedNum || !personality ? (
        <div className="rounded-3xl bg-[#120C21] p-7 text-center text-white shadow-lg">
          <Lock className="mx-auto h-11 w-11 text-amber-400" />
          <h2 className="mt-3 font-display text-2xl font-bold">
            Pick a Secret Number (1–{maxN})
          </h2>
          <p className="mt-2 text-sm text-purple-200/85">
            Your personality stays <strong>HIDDEN</strong> while you pick a number and place your phone on your forehead.
          </p>
        </div>
      ) : !myPlayer.cardVisible ? (
        <div className="rounded-3xl bg-[#120C21] p-6 text-center text-white shadow-xl">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-400/70 bg-amber-400/15 px-3.5 py-1 font-mono text-xs font-bold text-amber-300">
            <Lock className="h-3.5 w-3.5" />
            CARD #{pickedNum} • HIDDEN
          </span>

          <div className="mx-auto mt-5 flex h-24 w-24 items-center justify-center rounded-full bg-[#24193D] font-mono text-3xl font-extrabold text-amber-400">
            #{pickedNum}
          </div>

          <h2 className="mt-4 font-display text-2xl font-bold">Place Phone on Forehead</h2>
          <p className="mt-1.5 text-sm text-purple-200/85">
            Hold screen facing outward toward friends, then tap the big <strong>DISPLAY</strong> button!
          </p>

          <button
            disabled={isFinished}
            onClick={() => onSetCardVisible(true)}
            className="mt-6 flex w-full items-center justify-center gap-3 rounded-2xl bg-amber-400 py-5 font-display text-2xl font-extrabold tracking-wider text-[#120C21] shadow-lg active:scale-[0.99] hover:bg-amber-300 disabled:opacity-40"
          >
            <Eye className="h-7 w-7" />
            DISPLAY
          </button>
        </div>
      ) : (
        /* REVEALED FOREHEAD CARD (HIGH-CONTRAST) */
        <div className="overflow-hidden rounded-3xl border-2 border-amber-400 bg-[#120C21] text-white shadow-2xl">
          <div className="relative h-60 w-full bg-gradient-to-br from-[#281A4A] via-[#1B1233] to-[#120C21]">
            {personality.image_url && !imgFailed ? (
              <img
                src={personality.image_url}
                alt={personality.name}
                onError={() => setImgFailed(true)}
                className="h-full w-full object-cover"
              />
            ) : (
              /* Clean Fallback Clue Card */
              <div className="flex h-full w-full flex-col items-center justify-center p-4 text-center">
                <div className="flex h-24 w-24 items-center justify-center rounded-full border-4 border-amber-400 bg-[#120C21]/90 font-display text-4xl font-extrabold text-amber-400 shadow-lg">
                  {getInitials(personality.name)}
                </div>
                <span className="mt-3 font-mono text-xs font-bold uppercase tracking-widest text-amber-300">
                  Clue Card • {personality.category}
                </span>
              </div>
            )}

            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#120C21] via-transparent to-black/40" />

            <div className="absolute left-3.5 right-3.5 top-3.5 flex items-center justify-between">
              <span className="rounded-full bg-amber-400 px-3 py-1 font-mono text-xs font-extrabold text-[#120C21]">
                CARD #{pickedNum}
              </span>
              <span className="rounded-full border border-teal-400/60 bg-[#120C21]/85 px-3 py-1 font-mono text-xs font-bold uppercase text-teal-300">
                {personality.region}
              </span>
            </div>
          </div>

          <div className="px-5 pb-6 pt-3 text-center">
            <h1 className="font-display text-4xl font-extrabold leading-tight tracking-tight text-white">
              {personality.name}
            </h1>
            {personality.tamil_name && (
              <p className="mt-1.5 text-xl font-bold text-amber-400">{personality.tamil_name}</p>
            )}

            <div className="mt-3 flex flex-wrap items-center justify-center gap-2">
              <span className="rounded-lg bg-white/10 px-3 py-1 text-xs font-semibold text-purple-100">
                {personality.category}
              </span>
              <span className="rounded-lg bg-white/10 px-3 py-1 text-xs font-semibold text-purple-100">
                {personality.era}
              </span>
            </div>

            <button
              disabled={isFinished}
              onClick={() => onSetCardVisible(false)}
              className="mt-6 flex w-full items-center justify-center gap-2.5 rounded-2xl bg-rose-600 py-4 font-display text-xl font-extrabold tracking-wider text-white shadow-lg hover:bg-rose-500 disabled:opacity-40"
            >
              <EyeOff className="h-6 w-6" />
              HIDE / LOCK
            </button>
          </div>
        </div>
      )}

      {/* 2. 3 ROUNDS / 15 QUESTIONS MAX TRACKER */}
      {pickedNum > 0 && (
        <div className="rounded-3xl border border-[#E7DAC6] bg-white p-5 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-display text-base font-bold text-[#1D1528]">
                Round {room.currentRound} of 3 • Yes/No Questions
              </h3>
              <p className="text-xs text-[#5A4E6C]">Score = max(0, 15 − Questions Asked)</p>
            </div>
            <span className="rounded-xl bg-[#FFE4D6] px-3 py-1.5 font-mono text-sm font-extrabold text-[#D9480F]">
              {potentialScore} pts
            </span>
          </div>

          <div className="space-y-2.5">
            <div>
              <div className="mb-1 flex justify-between text-xs font-semibold">
                <span>This Round Questions</span>
                <span className="font-mono text-[#D9480F]">{roundQ} / 5</span>
              </div>
              <div className="h-2.5 w-full overflow-hidden rounded-full bg-[#F6EDE0]">
                <div
                  className="h-full rounded-full bg-[#D9480F] transition-all"
                  style={{ width: `${(roundQ / 5) * 100}%` }}
                />
              </div>
            </div>

            <div>
              <div className="mb-1 flex justify-between text-xs font-semibold">
                <span>Total Match Questions</span>
                <span className="font-mono text-[#0F766E]">{totalQ} / 15</span>
              </div>
              <div className="h-2.5 w-full overflow-hidden rounded-full bg-[#F6EDE0]">
                <div
                  className="h-full rounded-full bg-[#0F766E] transition-all"
                  style={{ width: `${(totalQ / 15) * 100}%` }}
                />
              </div>
            </div>
          </div>

          {myPlayer.guessCompleted ? (
            <div className="space-y-2.5">
              <div
                className={`flex items-center gap-2.5 rounded-2xl p-3.5 text-sm font-semibold ${
                  myPlayer.guessedCorrectly
                    ? 'bg-teal-50 text-teal-900'
                    : 'bg-rose-50 text-rose-900'
                }`}
              >
                <CheckCircle2 className="h-5 w-5 shrink-0" />
                <span>
                  {myPlayer.guessedCorrectly
                    ? `Guessed in ${totalQ} questions! Recorded ${potentialScore} pts for Round ${room.currentRound}.`
                    : `Marked unguessed — 0 pts recorded for Round ${room.currentRound}.`}
                </span>
              </div>
              {isHost && personality && deckIds.length > 1 && !isFinished && (
                <button
                  onClick={() => onEliminateCurrentCard(personality.id, personality.name)}
                  className="flex w-full items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 py-2.5 text-xs font-bold text-rose-800 hover:bg-rose-100"
                >
                  <Trash2 className="h-4 w-4" />
                  Eliminate &ldquo;{personality.name}&rdquo; from Pile
                </button>
              )}
            </div>
          ) : (
            <div className="space-y-2.5">
              <div className="flex gap-2.5">
                <button
                  disabled={isFinished || totalQ <= 0}
                  onClick={() =>
                    onUpdateQuestions(Math.max(0, roundQ - 1), Math.max(0, totalQ - 1))
                  }
                  className="flex flex-1 items-center justify-center gap-1.5 rounded-xl border border-[#DFCEB8] py-3 text-sm font-bold text-[#1D1528] disabled:opacity-40"
                >
                  <Minus className="h-4 w-4" />
                  Undo
                </button>
                <button
                  disabled={isFinished || roundQ >= 5 || totalQ >= 15}
                  onClick={() => onUpdateQuestions(roundQ + 1, totalQ + 1)}
                  className="flex-[1.8] flex items-center justify-center gap-1.5 rounded-xl bg-[#D9480F] py-3 text-sm font-bold text-white hover:bg-[#B83B0B] disabled:opacity-40"
                >
                  <Plus className="h-4 w-4" />
                  +1 Yes/No Question
                </button>
              </div>

              {roundQ >= 5 && totalQ < 15 && (
                <button
                  disabled={isFinished}
                  onClick={() => onUpdateQuestions(0, totalQ)}
                  className="flex w-full items-center justify-center gap-2 rounded-xl border border-[#0F766E] py-2.5 text-xs font-bold text-[#0F766E] hover:bg-teal-50"
                >
                  <RotateCcw className="h-4 w-4" />
                  Start Next 5-Question Round Block ({totalQ}/15 total)
                </button>
              )}

              <div className="flex gap-2.5">
                <button
                  disabled={isFinished}
                  onClick={() => onCompleteGuess(false)}
                  className="flex-1 rounded-xl border border-rose-300 py-3 text-sm font-bold text-rose-700 hover:bg-rose-50 disabled:opacity-40"
                >
                  Unguessed (0)
                </button>
                <button
                  disabled={isFinished}
                  onClick={() => onCompleteGuess(true)}
                  className="flex-[1.4] flex items-center justify-center gap-1.5 rounded-xl bg-[#0F766E] py-3 text-sm font-bold text-white hover:bg-teal-800 disabled:opacity-40"
                >
                  <CheckCircle2 className="h-4 w-4" />
                  Guessed! (+{potentialScore})
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 3. NUMBER PICKER GRID (STRICTLY 1 TO N = ACTIVE PILE SIZE) */}
      <div className="rounded-3xl border border-[#E7DAC6] bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between gap-2">
          <div>
            <h3 className="font-display text-base font-bold text-[#1D1528]">
              Select Card Number (1–{maxN})
            </h3>
            <p className="text-xs text-[#5A4E6C]">
              Max number equals active pile size ({maxN} personalities).
            </p>
          </div>
          <button
            disabled={isFinished || maxN <= 0}
            onClick={handleRandomPick}
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] px-3 py-2 text-xs font-bold text-[#1D1528] hover:border-[#D9480F] disabled:opacity-40"
          >
            <Dices className="h-4 w-4 text-[#D9480F]" />
            Random
          </button>
        </div>

        <div className="mt-3.5 grid grid-cols-5 gap-2 sm:grid-cols-8">
          {Array.from({ length: maxN }, (_, i) => i + 1).map((num) => {
            const isMine = num === pickedNum;
            const takenBy = takenByOthers[num];
            return (
              <button
                key={num}
                disabled={isFinished}
                onClick={() => onPickCard(num)}
                className={`flex h-12 flex-col items-center justify-center rounded-xl border font-mono text-sm font-bold transition ${
                  isMine
                    ? 'border-[#D9480F] bg-[#D9480F] text-white shadow-sm'
                    : takenBy
                      ? 'border-stone-200 bg-stone-100 text-stone-500'
                      : 'border-[#DFCEB8] bg-[#FFFBF2] text-[#1D1528] hover:border-[#D9480F]'
                }`}
              >
                <span>{num}</span>
                {takenBy && !isMine && (
                  <span className="text-[9px] font-sans uppercase leading-none">
                    {takenBy.slice(0, 3)}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
