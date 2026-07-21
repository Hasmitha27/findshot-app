package com.findshot.data

/**
 * Turns a natural-language query like
 * "find that image which shows the wifi password"
 * into meaningful keywords to search for: ["wifi", "password"]
 *
 * Deliberately simple and rule-based (not an LLM) — deterministic, instant,
 * needs no network/model, and is easy to test and reason about. A good
 * on-device search UX doesn't need an LLM for this step; it needs to be fast
 * and predictable. See README for why this is a deliberate design choice,
 * not a missing feature.
 */
object QueryParser {

    private val stopWords = setOf(
        "a", "an", "the", "find", "show", "shows", "showing", "image", "images",
        "photo", "photos", "picture", "pictures", "screenshot", "screenshots",
        "which", "that", "with", "has", "have", "having", "for", "of", "to",
        "in", "on", "at", "my", "i", "took", "taken", "search", "get", "give",
        "me", "please", "can", "you", "is", "are", "was", "were", "it", "this",
        "about", "containing", "contains", "where", "and", "or", "some"
    )

    fun extractKeywords(query: String): List<String> {
        val keywords = query
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() && it !in stopWords }
            .distinct()

        return keywords.ifEmpty {
            query.trim().takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
        }
    }
}