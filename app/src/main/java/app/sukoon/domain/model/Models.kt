package app.sukoon.domain.model

data class Section(
    val id: String,
    val title: String,
    val subtitle: String,
    val order: Int,
    val duaCount: Int,
)

enum class DuaKind { DUA, GUIDANCE }

data class Dua(
    val id: String,
    val sectionId: String,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String,
    val recitationNote: String?,
    val repeatCount: Int?,
    val order: Int,
    val kind: DuaKind,
    val isFavorite: Boolean = false,
)

data class DuaCollection(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val createdAt: Long,
)

data class CounterProgress(
    val duaId: String,
    val count: Int,
    val completedRounds: Int,
)

/** Which list the reader pages through. */
sealed interface ReaderList {
    data class SectionList(val sectionId: String) : ReaderList
    data object Favorites : ReaderList
    data class Collection(val collectionId: Long) : ReaderList
}
