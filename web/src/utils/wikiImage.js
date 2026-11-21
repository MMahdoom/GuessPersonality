import { useEffect, useState } from 'react';

const CACHE_STORAGE_KEY = 'gtp_wiki_portraits_v1';
const memoryCache = new Map(); // key: lookupTitle -> url string (or '' if verified missing)
let cacheLoaded = false;

function loadCacheOnce() {
  if (cacheLoaded) return;
  cacheLoaded = true;
  try {
    const raw = localStorage.getItem(CACHE_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      for (const [k, v] of Object.entries(parsed)) {
        if (typeof v === 'string' && v.startsWith('http')) {
          memoryCache.set(k, v);
        }
      }
    }
  } catch {
    // Ignore storage access issues
  }
}

function persistCache() {
  try {
    const obj = {};
    for (const [k, v] of memoryCache.entries()) {
      if (v) obj[k] = v;
    }
    localStorage.setItem(CACHE_STORAGE_KEY, JSON.stringify(obj));
  } catch {
    // Ignore quota errors
  }
}

function getLookupTitle(person) {
  if (!person) return '';
  return String(person.wiki_title || person.name || '').trim();
}

/**
 * Fetches verified 600px portrait URLs in batches of 45 from Wikipedia's
 * public CORS-enabled MediaWiki API (no API key or login required).
 */
export async function prefetchPersonalityImages(personalities) {
  loadCacheOnce();

  const result = {};
  const missingTitles = [];

  for (const p of personalities) {
    if (p.image_url && p.image_url.trim()) {
      result[p.id] = p.image_url.trim();
      continue;
    }
    const title = getLookupTitle(p);
    if (!title) continue;
    if (memoryCache.has(title)) {
      const cached = memoryCache.get(title);
      if (cached) result[p.id] = cached;
    } else {
      missingTitles.push({ id: p.id, title });
    }
  }

  if (missingTitles.length === 0) return result;

  const BATCH_SIZE = 45;
  for (let i = 0; i < missingTitles.length; i += BATCH_SIZE) {
    const chunk = missingTitles.slice(i, i + BATCH_SIZE);
    const titlesParam = chunk.map((c) => c.title).join('|');
    const url =
      `https://en.wikipedia.org/w/api.php?action=query&redirects=1&prop=pageimages` +
      `&piprop=thumbnail&pithumbsize=600&format=json&origin=*&titles=${encodeURIComponent(titlesParam)}`;

    try {
      const res = await fetch(url);
      if (!res.ok) continue;
      const data = await res.json();
      const query = data?.query || {};

      const normMap = {};
      for (const n of query.normalized || []) {
        normMap[n.from] = n.to;
      }
      const redirMap = {};
      for (const r of query.redirects || []) {
        redirMap[r.from] = r.to;
      }

      const thumbByTitle = {};
      for (const page of Object.values(query.pages || {})) {
        if (page?.title && page?.thumbnail?.source) {
          thumbByTitle[page.title] = page.thumbnail.source;
        }
      }

      for (const item of chunk) {
        const step1 = normMap[item.title] || item.title.replace(/_/g, ' ');
        const finalTitle = redirMap[step1] || step1;
        const foundUrl = thumbByTitle[finalTitle] || '';
        memoryCache.set(item.title, foundUrl);
        if (foundUrl) {
          result[item.id] = foundUrl;
        }
      }
    } catch {
      // Network offline or blocked; fallback clue card will display cleanly
    }
  }

  persistCache();
  return result;
}

/**
 * Resolves a single personality's portrait URL, prioritizing custom `image_url`
 * in personalities.json, then cached/live Wikipedia portrait, then fallback clue card.
 */
export async function resolveSinglePersonalityImage(person) {
  loadCacheOnce();
  if (!person) return '';
  if (person.image_url && person.image_url.trim()) {
    return person.image_url.trim();
  }

  const title = getLookupTitle(person);
  if (!title) return '';
  if (memoryCache.has(title) && memoryCache.get(title)) {
    return memoryCache.get(title);
  }

  // Try REST summary endpoint for single lookup
  try {
    const encoded = encodeURIComponent(title.replace(/\s+/g, '_'));
    const res = await fetch(`https://en.wikipedia.org/api/rest_v1/page/summary/${encoded}`);
    if (res.ok) {
      const data = await res.json();
      const src = data?.thumbnail?.source || data?.originalimage?.source || '';
      if (src) {
        memoryCache.set(title, src);
        persistCache();
        return src;
      }
    }
  } catch {
    // Ignore and fall back
  }

  // Fallback to Wikipedia search query if direct title didn't match
  try {
    const searchUrl =
      `https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(person.name)}` +
      `&gsrlimit=1&prop=pageimages&piprop=thumbnail&pithumbsize=600&format=json&origin=*`;
    const res = await fetch(searchUrl);
    if (res.ok) {
      const data = await res.json();
      const pages = Object.values(data?.query?.pages || {});
      const src = pages[0]?.thumbnail?.source || '';
      memoryCache.set(title, src);
      if (src) persistCache();
      return src;
    }
  } catch {
    // Ignore
  }

  return '';
}

export function usePersonalityImage(person, preloadedMap) {
  const preloaded = person
    ? (person.image_url && person.image_url.trim()) || preloadedMap?.[person.id] || ''
    : '';
  const [url, setUrl] = useState(preloaded);
  const [loading, setLoading] = useState(!preloaded && Boolean(person));

  useEffect(() => {
    if (!person) {
      setUrl('');
      setLoading(false);
      return;
    }
    const immediate =
      (person.image_url && person.image_url.trim()) || preloadedMap?.[person.id] || '';
    if (immediate) {
      setUrl(immediate);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    resolveSinglePersonalityImage(person).then((resolved) => {
      if (!cancelled) {
        setUrl(resolved);
        setLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [person?.id, person?.image_url, preloadedMap]);

  return { url, loading };
}
