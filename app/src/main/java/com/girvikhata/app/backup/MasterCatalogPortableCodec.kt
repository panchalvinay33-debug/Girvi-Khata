package com.girvikhata.app.backup

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import org.json.JSONArray
import org.json.JSONObject

object MasterCatalogPortableCodec {
    private const val VERSION = 1
    private const val MAX_ENTRIES = 2_000

    fun encode(catalog: MasterCatalog): ByteArray = JSONObject().apply {
        put("version", VERSION)
        put("entries", JSONArray().apply {
            catalog.entries.forEach { entry ->
                put(JSONObject().apply {
                    put("id", entry.id)
                    put("kind", entry.kind.name)
                    put("name", entry.name)
                    put("active", entry.active)
                    put("categoryName", entry.categoryName)
                    put("rateBasisPoints", entry.rateBasisPoints)
                })
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): MasterCatalog {
        require(bytes.isNotEmpty()) { "Master catalog payload empty" }
        val root = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("Master catalog JSON damaged") }
        require(root.optInt("version", 0) == VERSION) { "Master catalog version unsupported" }
        val array = root.optJSONArray("entries") ?: JSONArray()
        require(array.length() <= MAX_ENTRIES) { "Too many master entries" }
        val entries = List(array.length()) { index ->
            array.getJSONObject(index).run {
                MasterEntry(
                    id = requiredText("id"),
                    kind = runCatching { MasterKind.valueOf(requiredText("kind")) }
                        .getOrElse { throw IllegalArgumentException("Invalid master kind") },
                    name = requiredText("name"),
                    active = optBoolean("active", true),
                    categoryName = optString("categoryName").trim(),
                    rateBasisPoints = optInt("rateBasisPoints", 0).also {
                        require(it in 0..100_000) { "Invalid master interest rate" }
                    },
                )
            }
        }
        require(entries.map { it.id }.distinct().size == entries.size) { "Duplicate master IDs" }
        require(entries.all { it.name.length <= 60 }) { "Master name too long" }
        return MasterCatalog(entries)
    }

    private fun JSONObject.requiredText(name: String): String = optString(name).trim().also {
        require(it.isNotEmpty()) { "Missing $name in master catalog" }
    }
}
