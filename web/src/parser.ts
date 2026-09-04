/**
 * ChikuThread markdown parser — mirrors com.chikura.parser.BunkerParser.kt
 * Preserves hierarchy: ## Section → ### Category → - https://url
 */

export interface Resource {
  id: string
  url: string
  raw: string
  domain: string
  section: string | null
  category: string | null
  typeHint: string
  indent: number
}

export interface Section {
  name: string
  categories: Category[]
}

export interface Category {
  name: string
  resources: Resource[]
}

export interface Domain {
  id: string
  name: string
  sections: Section[]
  resources: Resource[]
}

export interface ChikuThread {
  id: string
  name: string
  domains: Domain[]
}

const URL_RE = /https?:\/\/\S+/
const BARE_DOMAIN_RE = /^(?:www\.)?[\w.-]+\.[a-z]{2,}(?:\/.*)?$/i
const EXE_RE = /^[\w.-]+\.exe$/i
const HOST_PORT_RE = /^[\w.-]+\.[a-z]{2,}:\d+.*/i

export function inferTypeHint(url: string): string {
  const lower = url.toLowerCase()
  if (lower.includes('playlist')) return 'playlist'
  if (url.includes('@')) return 'channel'
  if (lower.includes('/channel')) return 'channel'
  if (lower.includes('youtu.be')) return 'video'
  if (lower.includes('/shorts')) return 'shorts'
  if (lower.includes('/live')) return 'live'
  if (lower.includes('/watch')) return 'video'
  if (lower.includes('reddit.com')) return 'website'
  if (lower.includes('amazon.')) return 'book'
  if (lower.includes('github.com')) return 'software'
  if (!lower.startsWith('http://') && !lower.startsWith('https://')) return 'bare'
  return 'website'
}

function fixMalformed(s: string): string {
  return s
    .replace(/htt\s*ps:\/\//g, 'https://')
    .replace(/htt\s*p:\/\//g, 'http://')
    .replace(/https\s*:\//g, 'https://')
    .replace(/http\s*:\//g, 'http://')
}

export function parseMarkdown(text: string, domain: string): Resource[] {
  const lines = text.split('\n')
  let currentSection: string | null = null
  let currentCategory: string | null = null
  const resources: Resource[] = []
  let counter = 1

  for (const rawLine of lines) {
    const indent = rawLine.length - rawLine.trimStart().length
    const trimmed = rawLine.trim()

    if (!trimmed || trimmed === '---' || trimmed.startsWith('>') || /^={3,}$/.test(trimmed)) continue

    // Markdown headings
    if (trimmed.startsWith('#')) {
      const hashCount = trimmed.match(/^#+/)?.[0].length ?? 0
      const content = trimmed.slice(hashCount).trim()
      if (!content) continue

      if (hashCount === 2) {
        currentSection = content
        currentCategory = null
      } else if (hashCount === 3) {
        currentCategory = content
      } else if (hashCount >= 4) {
        // subcategory — treat as category for simplicity
        currentCategory = content
      }
      // hashCount === 1 → title, skip
      continue
    }

    // Strip bullet marker
    let candidate = trimmed
    if (/^[-*+]\s?/.test(candidate)) {
      candidate = candidate.replace(/^[-*+]\s?/, '')
    }

    candidate = fixMalformed(candidate)

    // Clean trailing stray quotes
    if (candidate.endsWith("'") && candidate.includes('https://')) {
      candidate = candidate.replace(/'+$/, '')
    }

    const hasUrl = /https?:\/\//.test(candidate)
    let isBare = false
    if (!hasUrl && !candidate.includes(' ')) {
      if (BARE_DOMAIN_RE.test(candidate)) isBare = true
      if (EXE_RE.test(candidate)) isBare = true
      if (HOST_PORT_RE.test(candidate)) isBare = true
    }

    if (hasUrl || isBare) {
      const raw = candidate
      let url: string
      if (hasUrl) {
        const m = URL_RE.exec(raw)
        url = m?.[0] ?? raw
        url = url.replace(/[,.)\]'";>]+$/, '')
      } else {
        url = raw.replace(/[,.)\]'";]+$/, '')
      }

      const id = `${domain}-${String(counter).padStart(4, '0')}`
      counter++

      resources.push({
        id,
        url,
        raw,
        domain,
        section: currentSection,
        category: currentCategory,
        typeHint: inferTypeHint(url),
        indent,
      })
    }
  }

  return resources
}

export function buildThread(
  id: string,
  name: string,
  markdownByDomain: Map<string, string>
): ChikuThread {
  const domains: Domain[] = []

  for (const [domainId, text] of markdownByDomain) {
    const resources = parseMarkdown(text, domainId)

    // Group into sections/categories
    const sectionMap = new Map<string | null, Map<string | null, Resource[]>>()
    for (const r of resources) {
      if (!sectionMap.has(r.section)) sectionMap.set(r.section, new Map())
      const catMap = sectionMap.get(r.section)!
      if (!catMap.has(r.category)) catMap.set(r.category, [])
      catMap.get(r.category)!.push(r)
    }

    const sections: Section[] = []
    for (const [secName, catMap] of sectionMap) {
      if (secName === null) continue // flat resources without section
      const categories: Category[] = []
      for (const [catName, resList] of catMap) {
        categories.push({ name: catName ?? 'Uncategorized', resources: resList })
      }
      sections.push({ name: secName, categories })
    }

    // Flat resources (no section)
    const flatResources = resources.filter(r => r.section === null)

    domains.push({ id: domainId, name: domainId, sections, resources: flatResources.length ? resources : resources })
  }

  return { id, name, domains }
}
