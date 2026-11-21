import React, { useMemo, useState } from 'react';
import { Edit3, Flag, Minus, Plus, SkipForward, Trophy } from 'lucide-react';
import { calculateScore } from '../utils/gameUtils.js';

export default function PointsTab({
  room,
  playerId,
  isHost,
  onHostRecordScore,
  onHostAdvanceRound,
  onHostFinishMatch,
}) {
  const [editing, setEditing] = useState(null); // { uid, player }
  const [eliminateUsedOnNext, setEliminateUsedOnNext] = useState(true);

  const standings = useMemo(() => {
    return Object.entries(room.players || {}).sort(([, a], [, b]) => {
      const diff = (b.totalScore || 0) - (a.totalScore || 0);
      if (diff !== 0) return diff;
      return (a.nickname || '').localeCompare(b.nickname || '');
    });
  }, [room.players]);

  return (
    <div className="space-y-5">
      {/* Cumulative Standings Banner */}
      <div className="rounded-3xl bg-[#120C21] p-5 text-center text-white shadow-lg">
        <div className="inline-flex items-center gap-2">
          <Trophy className="h-6 w-6 text-amber-400" />
          <h2 className="font-display text-xl font-bold">
            {room.status === 'FINISHED' ? 'Final Match Standings' : 'Points & Cumulative Standings'}
          </h2>
        </div>
        <p className="mt-1 text-xs text-amber-300">
          Round {room.currentRound} of 3 • Max 15 pts/round (Score = 15 − Questions Asked)
        </p>

        {standings.length > 0 && (
          <div className="mt-4 grid grid-cols-3 gap-2.5">
            {standings.slice(0, 3).map(([uid, p], idx) => (
              <div
                key={uid}
                className="rounded-2xl border border-amber-400/40 bg-white/10 p-3"
              >
                <div className="mx-auto flex h-6 w-6 items-center justify-center rounded-full bg-amber-400 font-mono text-xs font-extrabold text-[#120C21]">
                  #{idx + 1}
                </div>
                <div className="mt-1.5 truncate text-xs font-bold">{p.nickname}</div>
                <div className="font-mono text-base font-extrabold text-amber-400">
                  {p.totalScore || 0} pts
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Host Round Controls */}
      {isHost && room.status !== 'FINISHED' && (
        <div className="rounded-3xl border border-[#F5C2A8] bg-[#FFE4D6]/40 p-4 space-y-3">
          <div>
            <h3 className="font-display text-base font-bold text-[#1D1528]">
              Host Round &amp; Scoring Controls
            </h3>
            <p className="text-xs text-[#5A4E6C]">
              Tap the edit icon next to any player to record or review Round 1–3 scores.
            </p>
          </div>

          {Number(room.currentRound) < 3 && (
            <label className="flex cursor-pointer items-center gap-2 text-xs font-semibold text-[#1D1528]">
              <input
                type="checkbox"
                checked={eliminateUsedOnNext}
                onChange={(e) => setEliminateUsedOnNext(e.target.checked)}
                className="h-4 w-4 accent-[#D9480F]"
              />
              Auto-eliminate this round&apos;s picked personalities from the pile
            </label>
          )}

          <div className="flex gap-2.5">
            {Number(room.currentRound) < 3 && (
              <button
                onClick={() =>
                  onHostAdvanceRound(Number(room.currentRound) + 1, eliminateUsedOnNext)
                }
                className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-[#D9480F] py-3 text-sm font-bold text-white hover:bg-[#B83B0B]"
              >
                Start Round {Number(room.currentRound) + 1}
                <SkipForward className="h-4 w-4" />
              </button>
            )}
            <button
              onClick={onHostFinishMatch}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-xl border border-[#1D1528] bg-white py-3 text-sm font-bold text-[#1D1528] hover:bg-stone-100"
            >
              <Flag className="h-4 w-4" />
              Finish Match
            </button>
          </div>
        </div>
      )}

      {/* Full 3-Round Scoreboard Table */}
      <div className="overflow-hidden rounded-3xl border border-[#E7DAC6] bg-white shadow-sm">
        <div className="grid grid-cols-12 border-b border-[#E7DAC6] bg-[#F6EDE0] px-4 py-2.5 font-mono text-[11px] font-bold text-[#5A4E6C]">
          <span className="col-span-5">PLAYER</span>
          <span className="col-span-1 text-center">R1</span>
          <span className="col-span-1 text-center">R2</span>
          <span className="col-span-1 text-center">R3</span>
          <span className="col-span-2 text-right text-[#D9480F]">TOTAL</span>
          <span className="col-span-2 text-right">{isHost ? 'EDIT' : ''}</span>
        </div>

        <div className="divide-y divide-[#F0E6D8]">
          {standings.map(([uid, p], idx) => (
            <div
              key={uid}
              className={`grid grid-cols-12 items-center px-4 py-3 text-sm ${
                uid === playerId ? 'bg-teal-50/60' : ''
              }`}
            >
              <div className="col-span-5 flex items-center gap-2 min-w-0">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#F6EDE0] font-mono text-xs font-bold">
                  {idx + 1}
                </span>
                <span className="truncate font-semibold text-[#1D1528]">
                  {p.nickname} {uid === playerId && '(You)'}
                </span>
              </div>
              <span className="col-span-1 text-center font-mono">{p.round1Score || 0}</span>
              <span className="col-span-1 text-center font-mono">{p.round2Score || 0}</span>
              <span className="col-span-1 text-center font-mono">{p.round3Score || 0}</span>
              <span className="col-span-2 text-right font-mono font-extrabold text-[#D9480F]">
                {p.totalScore || 0}
              </span>
              <div className="col-span-2 flex justify-end">
                {isHost && room.status !== 'FINISHED' && (
                  <button
                    onClick={() => setEditing({ uid, player: p })}
                    className="rounded-lg p-1.5 text-[#D9480F] hover:bg-[#FFE4D6]"
                    aria-label={`Edit score for ${p.nickname}`}
                  >
                    <Edit3 className="h-4 w-4" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {editing && (
        <HostScoreEditModal
          player={editing.player}
          defaultRound={Number(room.currentRound || 1)}
          onSave={(round, score) => {
            onHostRecordScore(editing.uid, round, score);
            setEditing(null);
          }}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}

function HostScoreEditModal({ player, defaultRound, onSave, onClose }) {
  const [round, setRound] = useState(defaultRound);
  const existingScore =
    round === 1
      ? Number(player.round1Score || 0)
      : round === 2
        ? Number(player.round2Score || 0)
        : Number(player.round3Score || 0);

  const [questions, setQuestions] = useState(Math.max(0, Math.min(15, 15 - existingScore)));
  const [unguessed, setUnguessed] = useState(existingScore === 0);

  const handleRoundChange = (nextRound) => {
    setRound(nextRound);
    const s =
      nextRound === 1
        ? Number(player.round1Score || 0)
        : nextRound === 2
          ? Number(player.round2Score || 0)
          : Number(player.round3Score || 0);
    setQuestions(Math.max(0, Math.min(15, 15 - s)));
    setUnguessed(s === 0);
  };

  const score = calculateScore(questions, !unguessed);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-sm rounded-3xl bg-white p-5 shadow-2xl space-y-4">
        <h3 className="font-display text-lg font-bold text-[#1D1528]">
          Record Score • {player.nickname}
        </h3>

        <div className="flex gap-2">
          {[1, 2, 3].map((r) => (
            <button
              key={r}
              onClick={() => handleRoundChange(r)}
              className={`flex-1 rounded-xl py-2 text-xs font-bold ${
                round === r
                  ? 'bg-[#D9480F] text-white'
                  : 'bg-[#F6EDE0] text-[#5A4E6C]'
              }`}
            >
              Round {r}
            </button>
          ))}
        </div>

        <div className="flex items-center justify-between rounded-2xl bg-[#FFFBF2] p-3 border border-[#E7DAC6]">
          <button
            onClick={() => {
              setUnguessed(false);
              setQuestions((q) => Math.max(0, q - 1));
            }}
            className="rounded-xl border border-[#DFCEB8] bg-white p-2.5"
          >
            <Minus className="h-4 w-4" />
          </button>
          <div className="text-center">
            <div className="font-mono text-lg font-extrabold">
              {unguessed ? 'Unguessed' : `${questions} Qs`}
            </div>
            <div className="text-xs font-bold text-[#D9480F]">Score = {score} pts</div>
          </div>
          <button
            onClick={() => {
              setUnguessed(false);
              setQuestions((q) => Math.min(15, q + 1));
            }}
            className="rounded-xl border border-[#DFCEB8] bg-white p-2.5"
          >
            <Plus className="h-4 w-4" />
          </button>
        </div>

        <button
          onClick={() => {
            setUnguessed((u) => !u);
            if (!unguessed) setQuestions(15);
          }}
          className={`w-full rounded-xl border py-2.5 text-xs font-bold ${
            unguessed
              ? 'border-rose-400 bg-rose-50 text-rose-700'
              : 'border-[#DFCEB8] text-[#5A4E6C]'
          }`}
        >
          {unguessed ? 'Marked Unguessed (0 pts)' : 'Mark as Unguessed (0 pts)'}
        </button>

        <div className="flex gap-2">
          <button
            onClick={onClose}
            className="flex-1 rounded-xl border border-[#DFCEB8] py-2.5 text-sm font-bold text-[#5A4E6C]"
          >
            Cancel
          </button>
          <button
            onClick={() => onSave(round, score)}
            className="flex-1 rounded-xl bg-[#D9480F] py-2.5 text-sm font-bold text-white"
          >
            Save {score} pts
          </button>
        </div>
      </div>
    </div>
  );
}
