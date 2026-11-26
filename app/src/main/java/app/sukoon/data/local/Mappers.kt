package app.sukoon.data.local

import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.Section

fun DuaWithFavorite.toDomain(): Dua = Dua(
    id = dua.id,
    sectionId = dua.sectionId,
    title = dua.title,
    arabic = dua.arabic,
    transliteration = dua.transliteration,
    translation = dua.translation,
    reference = dua.reference,
    recitationNote = dua.recitationNote,
    repeatCount = dua.repeatCount,
    order = dua.sortOrder,
    kind = runCatching { DuaKind.valueOf(dua.kind) }.getOrDefault(DuaKind.DUA),
    isFavorite = isFavorite,
)

fun SectionWithCount.toDomain(): Section = Section(
    id = section.id,
    title = section.title,
    subtitle = section.subtitle,
    order = section.sortOrder,
    duaCount = duaCount,
)

fun CollectionWithCount.toDomain(): DuaCollection = DuaCollection(
    id = collection.id,
    name = collection.name,
    itemCount = itemCount,
    createdAt = collection.createdAt,
)

fun CounterEntity?.toDomain(duaId: String): CounterProgress =
    CounterProgress(duaId = duaId, count = this?.count ?: 0, completedRounds = this?.completedRounds ?: 0)
