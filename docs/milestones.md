# Chikura — Milestones TODO

> One-by-one, professional, no filler. Each milestone is shippable.

## M0 Foundation (DONE)
- `gradle.properties:2` Xmx 2048m + `kotlin.daemon.jvmargs`
- Delete 700× `NotionBlock0001`/`YouTubeBlock0001`/`DiscordServer0001` filler → `NotionBlock(text,number)`, `MediaCard`, `DiscordServerIcon` single pro components
- Rename `NotionBlockEditor.kt→BlockEditor.kt`, `DiscordSidebar.kt→ChannelRail.kt`, `ObsidianGraph.kt→GraphBoard.kt`

## M1 Core Data (DONE)
- `parser/ChikuThreadParser.kt:70` `parseMarkdown` preserves indent + `inferTypeHint`
- `app/App.kt:83` `loadRealChikuThread()` reads `ChikuThreads/ValiantZippu/ChikuThread 1/*.md` via `platform/FileSystem.kt` → `buildChikuThread` → 26 files · 1005 links (was 4·11 sample)
- `hydrator/Hydrator.kt:69` sidecar `MediaCache` hydrates title/thumb/author via `noembed.com`/`youtube/oembed`

## M2 Design (DONE)
- `ui/theme/ChikuraLogo.kt:1` `ChikuraMark`/`Wordmark`/`Horizontal`/`Bunker` vault, AMOLED `RoundedCornerShape(16)` 1px
- `ui/theme/ChikuraTheme.kt` AMOLED tokens, `JetBrains Mono`
- `ui/components/SettingsPanel.kt:1` slide-in 420dp bunker for GitHub sync/appearance/playback

## M3 Layout (DONE)
- `app/App.kt:43` `ViewMode {LIST, WHITEBOARD}` (KANBAN removed) + `EditMode {VIEW, EDIT}` pill
- Resizable sidebar `App.kt:245` drag handle 180-380dp
- `App.kt:304` `InfiniteWhiteboard` → section-organized columns (320dp per `##` section) + `transformable` pan/zoom `Offset/scale`, HUD `WHITEBOARD · sections · drag/pinch`
- Text bump 7sp→10-11sp, `SET` not `⚙`

## M4 Media (IN PROGRESS)
- Discord thumbnails: `ui/components/ResourceCard.kt:43` `thumbUrl = hydra.thumb ?: img.youtube.com/vi/ID/hqdefault.jpg` — wired, needs `kamel-image` re-enabled for actual bitmap (rolled back to keep 7G host stable, now `YT:ID` text placeholder)
- Real WebView: `media/YouTubeEmbed.kt:1` expect/actual — `jvm` `Desktop.browse` fallback, `wasm` iframe. Swap to `compose-webview-multiplatform` `WebView(state)` for true inline `youtube-nocookie.com/embed`
- Incognito toggle `App.kt:185` `INCOGNITO` vs `SIGNED IN` → embed domain switch, no YT sync

## M5 Notion — all features
- `notion/BlockEditor.kt:1` blocks: `Heading H1-3`, `Paragraph`, `Todo(check)`, `Bulleted`, `Toggle(expand)`, `Quote`, `Code(lang)`, `Divider`, `Callout(emoji)`, slash menu `/heading /todo /toggle /code /quote`
- Inline editing `ResourceCard.kt:176` `EditMode.EDIT` → `TextField` block with `Tab→indent`
- TODO: drag reorder, `Ctrl+]` indent, `moveResourceInMarkdown` round-trip, block drag handles

## M6 Discord — inspired, not clone
- `discord/ChannelRail.kt:1` `DiscordServerIcon` + `DiscordChannelRow` + `CategoryHeader` + `ChannelSidebar` + `PostCard` with `Follow`/`React`/`thumb` embed
- Integrate into `App.kt:211` left rail (server icons) + sidebar channels per domain, threads view on click, embeds show YouTube preview as in Discord screenshots
- Add reactions, follow, message bar `Send a message in "..."`

## M7 GitHub Sync — Notion alternative core
- `auth/GitHubAuth.kt` OAuth/device flow → token
- `marketplace/GitClone.*.kt` `JGit` clone/pull/push
- `SettingsPanel` wired: `Connect GitHub → Create repo (private) → Connect private URL → Sync Now` — uses `platform/FileSystem` to write `.md`, `git diff` clean
- Marketplace `marketplace.json` curated index

## M8 Polish
- Deterministic sort: `curated (markdown order)` vs `A-Z` vs `recent`, remove hash randomness (`App.kt:327` `hashCode%12` replaced by section columns)
- Search `Search or create a post...` filtering `ResourceCard`
- Drag reorder whiteboard nodes + keyboard nav
- Perf: pagination for 1005 links, lazy hydrate batch

---
Next: M4 thumbnails (re-enable Kamel) → M5 Notion drag → M6 Discord threads → M7 GitHub private sync. Confirm order or start M5 now?
