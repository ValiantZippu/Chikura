/**
 * Chikura web previews — thumbnails + titles for every URL, no backend.
 * - YouTube: deterministic thumbnails + noembed titles (CORS-enabled).
 * - Everything else: microlink free API (CORS-enabled, no key).
 * Sidecar equivalent: localStorage cache, never touches the .md.
 */
import type { Resource } from './parser'

export interface Preview {
  title: string
  description: string
  image: string
  site: string
}

const mem = new Map<string, Preview | null>()

function lsGet(key: string): Preview | null | undefined {
  try {
    const raw = localStorage.getItem('chikura-preview:' + key)
    if (!raw) return undefined
    return JSON.parse(raw) as Preview
  } catch {
    return undefined
  }
}

function lsSet(key: string, value: Preview | null) {
  try {
    localStorage.setItem('chikura-preview:' + key, JSON.stringify(value))
  } catch {
    /* private mode — memory cache still works */
  }
  mem.set(key, value)
}

export function extractYouTubeId(url: string): string | null {
  const lower = url.toLowerCase()
  let id: string | null = null
  if (lower.includes('youtu.be/')) {
    id = url.split('youtu.be/')[1]?.split(/[?&#]/)[0] ?? null
  } else if (lower.includes('watch?v=')) {
    id = url.split('watch?v=')[1]?.split(/[&#]/)[0] ?? null
  } else if (lower.includes('/shorts/')) {
    id = url.split('/shorts/')[1]?.split(/[?&#/]/)[0] ?? null
  } else if (lower.includes('/live/')) {
    id = url.split('/live/')[1]?.split(/[?&#/]/)[0] ?? null
  }
  if (!id) return null
  id = id.trim()
  return id.length >= 6 && id.length <= 20 ? id : null
}

export function youtubeThumb(id: string): string {
  return `https://img.youtube.com/vi/${id}/hqdefault.jpg`
}

export function youtubeEmbed(id: string, startSec = 0): string {
  const start = startSec > 0 ? `&start=${startSec}` : ''
  return `https://www.youtube-nocookie.com/embed/${id}?autoplay=1&modestbranding=1&rel=0&playsinline=1${start}`
}

export function channelHandle(url: string): string {
  const lower = url.toLowerCase()
  if (url.includes('@')) return '@' + url.split('@')[1]!.split(/[/?&#]/)[0]
  if (lower.includes('/channel/')) return url.split('/channel/')[1]!.split(/[/?&#]/)[0]!.slice(0, 14)
  if (lower.includes('/c/')) return url.split('/c/')[1]!.split(/[/?&#]/)[0]!
  if (lower.includes('/user/')) return url.split('/user/')[1]!.split(/[/?&#]/)[0]!
  return url.slice(0, 24)
}

interface Noembed {
  title?: string
  author_name?: string
  thumbnail_url?: string
  provider_name?: string
}

async function fetchNoembed(url: string): Promise<Noembed | null> {
  const key = 'noembed:' + url
  const cached = mem.get(key) ?? lsGet(key)
  if (cached !== undefined) {
    return cached as unknown as Noembed | null
  }
  try {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 10000)
    const res = await fetch(`https://noembed.com/embed?url=${encodeURIComponent(url)}`, { signal: ctrl.signal })
    clearTimeout(timer)
    if (!res.ok) {
      lsSet(key, null)
      return null
    }
    const data = (await res.json()) as Noembed
    if (!data.title) {
      lsSet(key, null)
      return null
    }
    lsSet(key, data as unknown as Preview)
    return data
  } catch {
    lsSet(key, null)
    return null
  }
}

async function fetchMicrolink(url: string): Promise<Preview | null> {
  const key = 'meta:' + url
  const cached = mem.get(key) ?? lsGet(key)
  if (cached !== undefined) return cached
  try {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 10000)
    const res = await fetch(`https://api.microlink.io?url=${encodeURIComponent(url)}&meta=true&timeout=8000`, {
      signal: ctrl.signal,
    })
    clearTimeout(timer)
    if (!res.ok) {
      lsSet(key, null)
      return null
    }
    const json = await res.json()
    const d = json?.data
    if (!d || json?.status !== 'success') {
      lsSet(key, null)
      return null
    }
    const preview: Preview = {
      title: d.title ?? '',
      description: d.description ?? '',
      image: d.image?.url ?? '',
      site: d.publisher ?? d.url?.split('/')[2] ?? '',
    }
    if (!preview.title && !preview.image) {
      lsSet(key, null)
      return null
    }
    lsSet(key, preview)
    return preview
  } catch {
    lsSet(key, null)
    return null
  }
}

export interface CardPreview {
  kind: 'video' | 'channel' | 'playlist' | 'link' | 'bare'
  videoId: string | null
  title: string
  subtitle: string
  image: string
  handle: string
}

export async function getPreview(res: Resource): Promise<CardPreview> {
  const t = res.typeHint
  const vid = extractYouTubeId(res.url)
  if ((t === 'video' || t === 'shorts' || t === 'live' || vid) && vid) {
    const meta = await fetchNoembed(res.url)
    return {
      kind: 'video',
      videoId: vid,
      title: meta?.title ?? res.raw,
      subtitle: meta?.author_name ?? res.url,
      image: meta?.thumbnail_url || youtubeThumb(vid),
      handle: '',
    }
  }
  if (t === 'channel') {
    const handle = channelHandle(res.url)
    const meta = await fetchNoembed(res.url)
    return {
      kind: 'channel',
      videoId: null,
      title: meta?.title || handle,
      subtitle: `${handle} · youtube channel`,
      image: meta?.thumbnail_url ?? '',
      handle,
    }
  }
  if (t === 'playlist') {
    const meta = await fetchNoembed(res.url)
    return {
      kind: 'playlist',
      videoId: null,
      title: meta?.title || res.raw,
      subtitle: meta?.author_name ?? res.url,
      image: meta?.thumbnail_url ?? '',
      handle: '',
    }
  }
  if (t === 'bare' && !res.url.startsWith('http')) {
    return { kind: 'bare', videoId: null, title: res.raw, subtitle: res.url, image: '', handle: '' }
  }
  const meta = await fetchMicrolink(res.url.startsWith('http') ? res.url : 'https://' + res.url)
  if (!meta) {
    return { kind: 'link', videoId: null, title: res.raw, subtitle: res.url, image: '', handle: '' }
  }
  return {
    kind: 'link',
    videoId: null,
    title: meta.title || res.raw,
    subtitle: meta.site ? `${meta.site} · ${res.url}` : res.url,
    image: meta.image,
    handle: meta.description,
  }
}

export function isFollowed(handle: string): boolean {
  try {
    return localStorage.getItem('chikura-follow:' + handle) === '1'
  } catch {
    return false
  }
}

export function toggleFollow(handle: string): boolean {
  const next = !isFollowed(handle)
  try {
    if (next) localStorage.setItem('chikura-follow:' + handle, '1')
    else localStorage.removeItem('chikura-follow:' + handle)
  } catch {
    /* ignore */
  }
  return next
}
