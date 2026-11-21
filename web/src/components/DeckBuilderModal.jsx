import React, { useMemo, useState } from 'react';
import { Check, Search, X } from 'lucide-react';
import { getInitials } from '../utils/gameUtils.js';

const REGIONS = ['All', 'Tamil', 'Pan-Indian'];
const CATEGORIES = [
  'All',
  'Cinema & Actors',
  'Directors & Music',
  'Sports',
  'Literature & History',
  'Public Figures',
];

export default function DeckBuilderModal({
  allPersonalities,
  initialSelectedIds,
  selectionMode = true,
  onConfirm,
  onClose,
}) {
  const [search, setSearch] = useState('');
  const [region, setRegion] = useState('All');
  const [category, setCategory] = useState('All');
  const [selected, setSelected] = useState(() => new Set(initialSelectedIds || []));

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return allPersonalities.filter((p) => {
      if (region !== 'All' && p.region !== region) return false;
      if (category !== 'All' && p.category !== category) return false;
      if (!q) return true;
      return (
        p.name.toLowerCase().includes(q) ||
        (p.tamil_name || '').toLowerCase().includes(q) ||
        p.category.toLowerCase().includes(q) ||
        p.era.toLowerCase().includes(q)
      );
    });
  }, [allPersonalities, search, region, category]);

  const toggleId = (id) => {
    if (!selectionMode) return;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const applyPreset = (type) => {
    if (type === 'clear') {
      setSelected(new Set());
      return;
    }
    if (type === 'all') {
      setSelected(new Set(allPersonalities.map((p) => p.id)));
      return;
    }
    const pool =
      type === 'tamil25'
        ? allPersonalities.filter((p) => p.region === 'Tamil')
        : [...allPersonalities];
    const shuffled = [...pool].sort(() => Math.random() - 0.5);
    const count = type === 'quick15' ? 15 : type === 'tamil25' ? 25 : 40;
    setSelected(new Set(shuffled.slice(0, count).map((p) => p.id)));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-3 backdrop-blur-sm">
      <div className="flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl bg-[#FFFBF2] shadow-2xl">
        {/* Header */}
        <div className="border-b border-[#E7DAC6] bg-white px-5 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-display text-xl font-bold text-[#1D1528]">
                {selectionMode ? 'Host Deck Builder' : '100 Indian Personalities'}
              </h2>
              <p className="text-xs text-[#5A4E6C]">
                {selectionMode
                  ? `${selected.size} selected — players will pick numbers 1 to ${Math.max(1, selected.size)}`
                  : '70 Tamil & 30 Pan-Indian personalities across 5 categories'}
              </p>
            </div>
            <button
              onClick={onClose}
              className="rounded-full p-2 text-[#5A4E6C] hover:bg-stone-100"
              aria-label="Close"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="relative mt-3">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#5A4E6C]" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search English or தமிழ் name..."
              className="w-full rounded-xl border border-[#DFCEB8] bg-[#FFFBF2] py-2.5 pl-9 pr-3 text-sm outline-none focus:border-[#D9480F]"
            />
          </div>

          <div className="mt-2.5 flex flex-wrap gap-1.5">
            {REGIONS.map((r) => (
              <button
                key={r}
                onClick={() => setRegion(r)}
                className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                  region === r
                    ? 'bg-[#D9480F] text-white'
                    : 'bg-[#F6EDE0] text-[#5A4E6C] hover:bg-[#EDE0CC]'
                }`}
              >
                {r === 'All' ? 'All (100)' : r === 'Tamil' ? 'Tamil (70)' : 'Pan-Indian (30)'}
              </button>
            ))}
          </div>

          <div className="mt-2 flex flex-wrap gap-1.5">
            {CATEGORIES.map((c) => (
              <button
                key={c}
                onClick={() => setCategory(c)}
                className={`rounded-full px-2.5 py-1 text-xs font-medium transition ${
                  category === c
                    ? 'bg-[#0F766E] text-white'
                    : 'bg-[#F6EDE0] text-[#5A4E6C] hover:bg-[#EDE0CC]'
                }`}
              >
                {c}
              </button>
            ))}
          </div>

          {selectionMode && (
            <div className="mt-3 flex flex-wrap gap-1.5">
              {[
                ['quick15', 'Quick 15'],
                ['tamil25', 'Tamil 25'],
                ['mix40', 'Mix 40'],
                ['all', 'All 100'],
                ['clear', 'Clear'],
              ].map(([key, label]) => (
                <button
                  key={key}
                  onClick={() => applyPreset(key)}
                  className="rounded-lg border border-[#DFCEB8] bg-white px-2.5 py-1 text-xs font-semibold text-[#1D1528] hover:border-[#D9480F]"
                >
                  {label}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* List */}
        <div className="flex-1 space-y-2 overflow-y-auto p-4">
          {filtered.map((p) => {
            const isSel = selected.has(p.id);
            return (
              <div
                key={p.id}
                onClick={() => toggleId(p.id)}
                className={`flex items-center gap-3 rounded-2xl border p-3 transition ${
                  selectionMode ? 'cursor-pointer' : ''
                } ${
                  selectionMode && isSel
                    ? 'border-[#D9480F] bg-[#FFE4D6]/40'
                    : 'border-[#E7DAC6] bg-white hover:border-[#D97706]'
                }`}
              >
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#1D1533] font-display text-sm font-bold text-amber-400">
                  {getInitials(p.name)}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5">
                    <span className="font-mono text-xs font-bold text-[#D9480F]">#{p.id}</span>
                    <span className="truncate font-semibold text-[#1D1528]">{p.name}</span>
                  </div>
                  {p.tamil_name && (
                    <div className="truncate text-xs text-[#5A4E6C]">{p.tamil_name}</div>
                  )}
                  <div className="mt-1 flex flex-wrap gap-1">
                    <span
                      className={`rounded-md px-1.5 py-0.5 text-[10px] font-semibold ${
                        p.region === 'Tamil'
                          ? 'bg-teal-100 text-teal-900'
                          : 'bg-amber-100 text-amber-900'
                      }`}
                    >
                      {p.region}
                    </span>
                    <span className="rounded-md bg-stone-100 px-1.5 py-0.5 text-[10px] text-stone-700">
                      {p.category}
                    </span>
                    <span className="rounded-md bg-stone-100 px-1.5 py-0.5 text-[10px] text-stone-600">
                      {p.era}
                    </span>
                  </div>
                </div>
                {selectionMode && (
                  <div
                    className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 ${
                      isSel
                        ? 'border-[#D9480F] bg-[#D9480F] text-white'
                        : 'border-[#DFCEB8] bg-white'
                    }`}
                  >
                    {isSel && <Check className="h-3.5 w-3.5" />}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Footer */}
        {selectionMode && onConfirm && (
          <div className="flex gap-2.5 border-t border-[#E7DAC6] bg-white px-5 py-3.5">
            <button
              disabled={selected.size === 0}
              onClick={() => onConfirm([...selected], false)}
              className="flex-1 rounded-xl border border-[#D9480F] px-4 py-3 text-sm font-bold text-[#D9480F] disabled:opacity-40"
            >
              Lock Pile ({selected.size})
            </button>
            <button
              disabled={selected.size === 0}
              onClick={() => onConfirm([...selected], true)}
              className="flex-[1.3] rounded-xl bg-[#D9480F] px-4 py-3 text-sm font-bold text-white shadow-sm hover:bg-[#B83B0B] disabled:opacity-40"
            >
              Finalize &amp; Start ({selected.size})
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
