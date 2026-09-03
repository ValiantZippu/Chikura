package com.chikura.repo

import com.chikura.model.ChikuThread
import com.chikura.model.ChikuThreadMeta

/**
 * Task 3: Domain Model & File-System Loader
 * Shared interface consumed by desktop (FileChikuThreadDataSource) and future wasm fetch.
 */
interface ChikuThreadDataSource {
    suspend fun listChikuThreads(): List<ChikuThreadMeta>
    suspend fun loadChikuThread(id: String): ChikuThread
}

/**
 * Thin repository wrapping a ChikuThreadDataSource.
 * Desktop injects FileChikuThreadDataSource; wasm will inject RemoteDataSource.
 */
class ChikuThreadRepository(private val dataSource: ChikuThreadDataSource) : ChikuThreadDataSource {
    override suspend fun listChikuThreads(): List<ChikuThreadMeta> = dataSource.listChikuThreads()
    override suspend fun loadChikuThread(id: String): ChikuThread = dataSource.loadChikuThread(id)
}
