import { useState, useEffect, useCallback } from 'react'
import { buildThread, type ChikuThread, type Domain, type Resource } from './parser'
import {
  getPreview,
  youtubeEmbed,
  isFollowed,
  toggleFollow,
  type CardPreview,
} from './preview'
import './App.css'

const DEFAULT_REPO = 'ValiantZippu/Chikura'
const BRANCH = 'main'

async function fetchRawFile(owner: string, repo: string, path: string): Promise<string | null> {
  try {
    const url = `https://raw.githubusercontent.com/${owner}/${repo}/${BRANCH}/${path}`
    const res = await fetch(url)
    if (!res.ok) return null
    return await res.text()
  } catch {
    return null
  }
}

async function fetchGitHubTree(owner: string, repo: string): Promise<string[]> {
  try {
    const res = await fetch(`https://api.github.com/repos/${owner}/${repo}/git/trees/${BRANCH}?recursive=1`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.tree || [])
      .filter((f: any) => f.type === 'blob' && f.path.endsWith('.md') && !f.path.startsWith('.'))
      .map((f: any) => f.path)
  } catch {
    return []
  }
}

function VideoCard({ resource, preview }: { resource: Resource; preview: CardPreview }) {
  const [playing, setPlaying] = useState(false)
  return (
    <div className="video-card">
      {!playing ? (
        <button className="video-shell" onClick={() => setPlaying(true)} aria-label="Play video">
          {preview.image && (
            <img src={preview.image} alt="" loading="lazy" onError={(e) => { e.currentTarget.style.display = 'none' }} />
          )}
          <span className="video-scrim" />
          <span className="play-btn">▶</span>
          <span className="video-tag">VIDEO</span>
        </button>
      ) : (
        <div className="video-shell playing">
          <iframe
            src={youtubeEmbed(preview.videoId ?? '', 0)}
            title={preview.title}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
          <button className="video-stop" onClick={() => setPlaying(false)}>
            STOP
          </button>
        </div>
      )}
      <div className="video-meta">
        <div className="video-title">{preview.title}</div>
        <div className="video-sub">{preview.subtitle}</div>
        <div className="video-urlbar">{resource.url}</div>
        <div className="video-tags">
          {resource.section && <span className="tag-dim">{resource.section}</span>}
          {resource.category && <span className="tag-white">{resource.category}</span>}
          <a className="tag-link" href={resource.url} target="_blank" rel="noopener noreferrer">
            OPEN ↗
          </a>
        </div>
      </div>
    </div>
  )
}

function ChannelCard({ resource, preview }: { resource: Resource; preview: CardPreview }) {
  const [following, setFollowing] = useState(() => isFollowed(preview.handle || resource.url))
  const key = preview.handle || resource.url
  const name = preview.title || preview.handle
  return (
    <div className="channel-row">
      <div className="avatar">{(name[0] ?? '#').toUpperCase()}</div>
      <div className="channel-info">
        <div className="channel-name">{name}</div>
        <div className="channel-sub">{preview.subtitle}</div>
        <div className="channel-tags">
          <span className="pill-white">CHANNEL</span>
        </div>
      </div>
      <div className="channel-actions">
        <a className="open-btn" href={resource.url} target="_blank" rel="noopener noreferrer">
          OPEN
        </a>
        <button
          className={following ? 'follow-btn on' : 'follow-btn'}
          onClick={() => setFollowing(toggleFollow(key))}
        >
          {following ? 'FOLLOWING' : 'FOLLOW'}
        </button>
      </div>
    </div>
  )
}

function PlaylistCard({ resource, preview }: { resource: Resource; preview: CardPreview }) {
  return (
    <div className="channel-row">
      <div className="playlist-glyph" aria-hidden="true">
        <span className="pl-bars">
          <i />
          <i />
          <i />
        </span>
        <span className="pl-play">▶</span>
      </div>
      <div className="channel-info">
        <div className="channel-name">{preview.title || resource.raw}</div>
        <div className="channel-sub">{preview.subtitle || resource.url}</div>
        <div className="channel-tags">
          <span className="pill-white">PLAYLIST</span>
        </div>
      </div>
      <div className="channel-actions">
        <a className="open-btn" href={resource.url} target="_blank" rel="noopener noreferrer">
          OPEN
        </a>
      </div>
    </div>
  )
}

function LinkCard({ resource, preview }: { resource: Resource; preview: CardPreview }) {
  const href = resource.url.startsWith('http') ? resource.url : 'https://' + resource.url
  return (
    <div className="link-row">
      {preview.image && (
        <div className="link-thumb">
          <img src={preview.image} alt="" loading="lazy" onError={(e) => { e.currentTarget.style.display = 'none' }} />
        </div>
      )}
      <div className="channel-info">
        <div className="channel-name">{preview.title || resource.raw}</div>
        {preview.handle && <div className="link-desc">{preview.handle}</div>}
        <div className="channel-sub">{preview.subtitle || resource.url}</div>
        <div className="channel-tags">
          <span className="pill-dim">{resource.typeHint.toUpperCase().slice(0, 8)}</span>
          <a className="tag-link" href={href} target="_blank" rel="noopener noreferrer">
            OPEN ↗
          </a>
        </div>
      </div>
    </div>
  )
}

function ResourceCard({ resource }: { resource: Resource }) {
  const [preview, setPreview] = useState<CardPreview | null>(null)

  useEffect(() => {
    let dead = false
    getPreview(resource).then((p) => {
      if (!dead) setPreview(p)
    })
    return () => {
      dead = true
    }
  }, [resource])

  if (!preview) {
    return (
      <div className="resource-card loading" aria-label="Loading preview">
        <div className="resource-thumb">
          <span>{resource.typeHint.slice(0, 4).toUpperCase()}</span>
        </div>
        <div className="resource-info">
          <div className="resource-url">{resource.raw}</div>
          <div className="resource-meta">
            <span className="resource-type">{resource.typeHint}</span>
          </div>
        </div>
      </div>
    )
  }

  if (preview.kind === 'video') return <VideoCard resource={resource} preview={preview} />
  if (preview.kind === 'channel') return <ChannelCard resource={resource} preview={preview} />
  if (preview.kind === 'playlist') return <PlaylistCard resource={resource} preview={preview} />
  if (preview.kind === 'link') return <LinkCard resource={resource} preview={preview} />
  return (
    <a
      href={resource.url.startsWith('http') ? resource.url : 'https://' + resource.url}
      target="_blank"
      rel="noopener noreferrer"
      className="resource-card"
    >
      <div className="resource-thumb">
        <span>{resource.typeHint.slice(0, 4).toUpperCase()}</span>
      </div>
      <div className="resource-info">
        <div className="resource-url">{resource.raw}</div>
        <div className="resource-meta">
          {resource.section && <span>## {resource.section}</span>}
          {resource.category && <span>### {resource.category}</span>}
          <span className="resource-type">{resource.typeHint}</span>
        </div>
      </div>
    </a>
  )
}

function Sidebar({
  thread,
  selected,
  onSelect,
}: {
  thread: ChikuThread
  selected: string | null
  onSelect: (id: string | null) => void
}) {
  const totalResources = thread.domains.reduce((sum, d) => sum + d.resources.length, 0)

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-title">CHIKUTHREADS</div>
        <div className="sidebar-subtitle">
          {thread.domains.length} files · {totalResources} links
        </div>
      </div>
      <div className="sidebar-list">
        <button
          className={`sidebar-item ${selected === null ? 'active' : ''}`}
          onClick={() => onSelect(null)}
        >
          <span className="sidebar-label">ALL DOMAINS</span>
          <span className="sidebar-count">{totalResources}</span>
        </button>
        {thread.domains.map((domain) => (
          <button
            key={domain.id}
            className={`sidebar-item ${selected === domain.id ? 'active' : ''}`}
            onClick={() => onSelect(domain.id)}
          >
            <span className="sidebar-label">{domain.id}</span>
            <span className="sidebar-count">{domain.resources.length}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function DomainSection({ domain }: { domain: Domain }) {
  const [expanded, setExpanded] = useState(true)

  return (
    <div className="domain-section">
      <button className="domain-header" onClick={() => setExpanded(!expanded)}>
        <span className="domain-toggle">{expanded ? '▼' : '▶'}</span>
        <span className="domain-name">{domain.id}</span>
        <span className="domain-count">{domain.resources.length}</span>
      </button>
      {expanded && (
        <div className="domain-content">
          {domain.sections.length === 0 ? (
            domain.resources.map((res) => (
              <ResourceCard key={res.id} resource={res} />
            ))
          ) : (
            domain.sections.map((section) => (
              <SectionBlock key={section.name} section={section} />
            ))
          )}
        </div>
      )}
    </div>
  )
}

function SectionBlock({ section }: { section: { name: string; categories: { name: string; resources: Resource[] }[] } }) {
  const [expanded, setExpanded] = useState(true)

  return (
    <div className="section-block">
      <button className="section-header" onClick={() => setExpanded(!expanded)}>
        <span className="section-toggle">{expanded ? '▾' : '▸'}</span>
        <span>## {section.name}</span>
      </button>
      {expanded && (
        <div className="section-content">
          {section.categories.map((cat) => (
            <div key={cat.name} className="category-block">
              <div className="category-name">### {cat.name}</div>
              {cat.resources.map((res) => (
                <ResourceCard key={res.id} resource={res} />
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function EmptyState({ loading, error }: { loading: boolean; error: string | null }) {
  return (
    <div className="empty-state">
      <div className="empty-box">
        <div className="empty-title">Chikura — 知蔵</div>
        <div className="empty-subtitle">knowledge vault</div>
        <div className="empty-platform">Web Wiki (read-only)</div>
      </div>
      {loading && <div className="empty-hint">Loading thread from GitHub...</div>}
      {error && <div className="empty-error">⚠ {error}</div>}
      {!loading && !error && (
        <div className="empty-hint">
          Enter a GitHub repo URL above to view a ChikuThread
        </div>
      )}
    </div>
  )
}

export default function App() {
  const [repoInput, setRepoInput] = useState(DEFAULT_REPO)
  const [thread, setThread] = useState<ChikuThread | null>(null)
  const [selectedDomain, setSelectedDomain] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadThread = useCallback(async (repo: string) => {
    const parts = repo.replace('https://github.com/', '').split('/')
    if (parts.length < 2) {
      setError('Enter as owner/repo (e.g. ValiantZippu/Chikura)')
      return
    }

    const [owner, repoName] = parts
    setLoading(true)
    setError(null)
    setThread(null)

    const files = await fetchGitHubTree(owner, repoName)
    if (files.length === 0) {
      setError(`No .md files found in ${owner}/${repoName}`)
      setLoading(false)
      return
    }

    const markdownByDomain = new Map<string, string>()
    for (const file of files) {
      const name = file.split('/').pop()!
      if (name === 'README.md') continue
      const text = await fetchRawFile(owner, repoName, file)
      if (text) {
        const domainId = name.replace('.md', '')
        // For nested files like archive-box/inbox.md, use path as domain id
        const key = file.includes('/') ? file.replace(/\//g, '-').replace('.md', '') : domainId
        markdownByDomain.set(key, text)
      }
    }

    if (markdownByDomain.size === 0) {
      setError('Failed to fetch markdown files')
      setLoading(false)
      return
    }

    const threadName = repoName
    setThread(buildThread(threadName, threadName, markdownByDomain))
    setLoading(false)
    setSelectedDomain(null)
  }, [])

  // Load default repo on mount
  useEffect(() => {
    loadThread(DEFAULT_REPO)
  }, [loadThread])

  const handleLoad = () => {
    if (repoInput.trim()) loadThread(repoInput.trim())
  }

  const visibleDomains = thread
    ? selectedDomain
      ? thread.domains.filter((d) => d.id === selectedDomain)
      : thread.domains
    : []

  const totalResources = visibleDomains.reduce((sum, d) => sum + d.resources.length, 0)

  return (
    <div className="app">
      {/* Repo input bar */}
      <div className="repo-bar">
        <span className="repo-label">CHIKURA</span>
        <span className="repo-kanji">知蔵</span>
        <div className="repo-input-group">
          <input
            className="repo-input"
            value={repoInput}
            onChange={(e) => setRepoInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleLoad()}
            placeholder="owner/repo"
          />
          <button className="repo-button" onClick={handleLoad}>
            LOAD
          </button>
        </div>
        {thread && (
          <span className="repo-stats">
            {thread.domains.length} domains · {thread.domains.reduce((s, d) => s + d.resources.length, 0)} links
          </span>
        )}
        <a
          className="repo-edit-link"
          href={`https://github.com/${repoInput}`}
          target="_blank"
          rel="noopener noreferrer"
        >
          VIEW ON GITHUB ↗
        </a>
      </div>

      <div className="main-layout">
        {thread && (
          <Sidebar
            thread={thread}
            selected={selectedDomain}
            onSelect={setSelectedDomain}
          />
        )}

        <div className="content-area">
          {!thread && !loading && !error && <EmptyState loading={false} error={null} />}
          {loading && <EmptyState loading={true} error={null} />}
          {error && <EmptyState loading={false} error={error} />}
          {thread && (
            <>
              <div className="content-header">
                <span>BUNKER: {thread.name}</span>
                <span className="content-stats">
                  {visibleDomains.length} domains · {totalResources} resources
                </span>
              </div>
              <div className="content-scroll">
                {visibleDomains.map((domain) => (
                  <DomainSection key={domain.id} domain={domain} />
                ))}
                {visibleDomains.length === 0 && (
                  <div className="empty-hint">(no resources)</div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
