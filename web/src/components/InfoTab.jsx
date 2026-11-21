import React from 'react';
import { HelpCircle, QrCode, Shuffle, Smartphone, MessageCircleQuestion, Trophy, X } from 'lucide-react';

const RULES = [
  {
    num: '1',
    title: 'Create or Join a Room',
    body: 'Host creates a room and shows the QR code. Friends tap Scan or enter the 6-letter code with a nickname — no sign-in needed.',
    icon: QrCode,
    badge: 'bg-amber-100 text-amber-900',
  },
  {
    num: '2',
    title: 'Pick a Secret Number',
    body: 'Tap any number from 1 to N. Numbers are freshly shuffled every game and your personality stays hidden from you.',
    icon: Shuffle,
    badge: 'bg-orange-100 text-orange-900',
  },
  {
    num: '3',
    title: 'Place on Forehead & DISPLAY',
    body: 'Hold your phone facing friends, tap DISPLAY, and tap the photo anytime for Landscape Full-Screen mode.',
    icon: Smartphone,
    badge: 'bg-teal-100 text-teal-900',
  },
  {
    num: '4',
    title: 'Ask Yes/No Questions',
    body: 'Ask up to 5 Yes/No questions per round across 3 rounds (15 max). Tap HIDE / LOCK before lowering your phone.',
    icon: MessageCircleQuestion,
    badge: 'bg-indigo-100 text-indigo-900',
  },
  {
    num: '5',
    title: 'Score & Win',
    body: 'Score = 15 minus questions asked (0 if unguessed). Highest total after 3 rounds wins! Rooms auto-clear after 60 mins idle.',
    icon: Trophy,
    badge: 'bg-rose-100 text-rose-900',
  },
];

export default function InfoTab({ onClose }) {
  return (
    <div className="space-y-2.5">
      <div className="flex items-center justify-between rounded-2xl bg-[#120C21] px-4 py-3 text-white shadow-sm">
        <div className="flex items-center gap-2.5">
          <HelpCircle className="h-5 w-5 shrink-0 text-amber-400" />
          <div>
            <h2 className="font-display text-base font-extrabold leading-tight">
              How to Play
            </h2>
            <p className="text-[11px] text-purple-200/85">
              Quick rules — 3 rounds, 15 Yes/No questions max
            </p>
          </div>
        </div>
        {onClose ? (
          <button
            type="button"
            onClick={onClose}
            aria-label="Close rules"
            className="rounded-full bg-white/15 p-1.5 text-white hover:bg-white/25"
          >
            <X className="h-4 w-4" />
          </button>
        ) : (
          <span className="rounded-lg bg-amber-400 px-2 py-0.5 font-mono text-[10px] font-extrabold text-[#120C21]">
            5 STEPS
          </span>
        )}
      </div>

      <div className="space-y-2">
        {RULES.map(({ num, title, body, icon: IconComp, badge }) => (
          <div
            key={num}
            className="flex items-start gap-3 rounded-2xl border border-[#E7DAC6] bg-white px-3.5 py-2.5 shadow-sm"
          >
            <div
              className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-xl font-mono text-xs font-extrabold ${badge}`}
            >
              <IconComp className="h-4 w-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-1.5">
                <span className="font-mono text-[11px] font-extrabold text-[#D9480F]">
                  0{num}
                </span>
                <h3 className="font-display text-sm font-bold leading-snug text-[#1D1528]">
                  {title}
                </h3>
              </div>
              <p className="mt-0.5 text-xs leading-snug text-[#5A4E6C]">{body}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
