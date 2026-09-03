package com.knowledgebunker.repo

import com.knowledgebunker.model.Bunker
import com.knowledgebunker.model.BunkerMeta

/**
 * Task 3: Domain Model & File-System Loader
 * Shared interface consumed by desktop (FileBunkerDataSource) and future wasm fetch.
 */
interface BunkerDataSource {
    suspend fun listBunkers(): List<BunkerMeta>
    suspend fun loadBunker(id: String): Bunker
}

/**
 * Thin repository wrapping a BunkerDataSource.
 * Desktop injects FileBunkerDataSource; wasm will inject RemoteDataSource.
 */
class BunkerRepository(private val dataSource: BunkerDataSource) : BunkerDataSource {
    override suspend fun listBunkers(): List<BunkerMeta> = dataSource.listBunkers()
    override suspend fun loadBunker(id: String): Bunker = dataSource.loadBunker(id)
}
