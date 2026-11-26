package app.sukoon.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedFile(
    val contentVersion: Int,
    val source: String,
    val sections: List<SeedSection>,
    val duas: List<SeedDua>,
)

@Serializable
data class SeedSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val order: Int,
)

@Serializable
data class SeedDua(
    val id: String,
    val sectionId: String,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String,
    val recitationNote: String? = null,
    val repeatCount: Int? = null,
    val order: Int,
    val kind: String = "DUA",
)
