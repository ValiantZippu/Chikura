import { useState, useEffect, useCallback } from 'react'
import { buildThread, type ChikuThread, type Domain, type Resource } from './parser'
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

function ResourceCard({ resource }: { resource: Resource }) {
  return (
    <a
      href={resource.url}
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
