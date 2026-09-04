package com.chikura.app

/**
 * Cross-platform platform name — expect/actual per Task 1.
 * Black & white terminal theme, neat folder app/.
 */
expect fun getPlatformName(): String

/**
 * Task 7: READ_ONLY flag — true on web/wasmJs (read-only wiki), false on desktop/android.
 * Web: no Monaco, no drag, show "Edit in App" button, reads via raw.githubusercontent.com / GitHub API, not FS.
 */
expect val READ_ONLY: Boolean
