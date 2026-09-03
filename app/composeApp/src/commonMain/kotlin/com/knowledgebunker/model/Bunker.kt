package com.knowledgebunker.model

import kotlinx.serialization.Serializable

/**
 * Bunker spec model — preserves markdown hierarchy.
 * Task 2: Bunker Spec Parser — Markdown AST Preserving Indent
 * Bunker = repo, Domain = *.md file (kebab-case), Section = ##, Category = ###/####, Resource = bullet URL/bare ref.
 * Task 7: Serializable for marketplace.json curated index (kebab-case *.md + archive-box/inbox.md exists).
 */
@Serializable
data class BunkerMeta(
    val id: String,
    val name: String,
    val path: String = "",
    // Marketplace optional fields — defaults keep FileBunkerDataSource mapping compatible
    val description: String? = null,
    val url: String? = null,
    val githubUrl: String? = null,
    val stars: Int? = null,
    val author: String? = null
)

/** Alias for test spec: Json.decodeFromString<Marketplace>(json) where Marketplace == List<BunkerMeta> */
typealias Marketplace = List<BunkerMeta>

data class Bunker(
    val id: String,
    val name: String,
    val domains: List<Domain>
)

data class Domain(
    val id: String,
    val name: String,
    val sections: List<Section> = emptyList(),
    val resources: List<Resource> = emptyList()
)

data class Section(
    val name: String,
    val categories: List<Category> = emptyList()
)

data class Category(
    val name: String,
    val resources: List<Resource> = emptyList()
)

/**
 * Resource preserves indent and raw line.
 * @param id domain-index e.g. music-0001
 * @param url normalized URL (malformed htt ps:// fixed, trailing punctuation stripped)
 * @param raw original raw line after bullet strip and malformed fix (e.g. "https://youtu.be/...")
 * @param domain domain id (file stem kebab-case, e.g. "music")
 * @param section ## Section or legacy 'Section' — nullable if flat file
 * @param category ### Category — nullable
 * @param subcategory #### Subcategory — nullable
 * @param typeHint inferred from url contains: video | playlist | channel | shorts | live | website | bare | book | software
 * @param indent leading spaces count (preserved)
 */
data class Resource(
    val id: String,
    val url: String,
    val raw: String,
    val domain: String,
    val section: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val typeHint: String = "website",
    val indent: Int = 0
)
