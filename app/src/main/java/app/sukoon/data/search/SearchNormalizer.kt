package app.sukoon.data.search

import java.text.Normalizer

/**
 * Normalises transliterated text so that the many spelling variants used for the same
 * Arabic word match each other: "Hasbunallahu", "hasbunallah", "Ḥasbunallāh" all reduce
 * to the same stem.
 */
object SearchNormalizer {
    private val marks = "\\p{Mn}+".toRegex()
    private val apostrophes = "['’‘`ʿʾ´\\-]".toRegex()
    private val nonAlnum = "[^a-z0-9\\s]".toRegex()
    private val spaces = "\\s+".toRegex()
    private val repeated = "(.)\\1+".toRegex()

    fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        return decomposed
            .replace(marks, "")
            .replace(apostrophes, "")
            .replace(nonAlnum, " ")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("ou", "u")
            .replace(repeated, "$1")
            .replace(spaces, " ")
            .trim()
    }

    /** Builds an FTS4 MATCH expression with prefix matching per token, or null if empty. */
    fun toMatchQuery(input: String): String? {
        val tokens = normalize(input).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }
}
