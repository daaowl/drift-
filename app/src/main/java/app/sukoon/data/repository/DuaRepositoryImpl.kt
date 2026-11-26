package app.sukoon.data.repository

import app.sukoon.data.local.DuaDao
import app.sukoon.data.local.toDomain
import app.sukoon.data.search.SearchNormalizer
import app.sukoon.data.seed.SeedLoader
import app.sukoon.di.IoDispatcher
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.Section
import app.sukoon.domain.repository.DuaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuaRepositoryImpl @Inject constructor(
    private val dao: DuaDao,
    private val seedLoader: SeedLoader,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DuaRepository {

    override suspend fun ensureSeeded() = seedLoader.ensureSeeded()

    override fun observeSections(): Flow<List<Section>> =
        dao.observeSections().map { list -> list.map { it.toDomain() } }.flowOn(io)

    override fun observeSection(sectionId: String): Flow<Section?> =
        dao.observeSection(sectionId).map { it?.toDomain() }.flowOn(io)

    override fun observeDuasInSection(sectionId: String): Flow<List<Dua>> =
        dao.observeDuasInSection(sectionId).map { list -> list.map { it.toDomain() } }.flowOn(io)

    override fun observeDua(duaId: String): Flow<Dua?> =
        dao.observeDua(duaId).map { it?.toDomain() }.flowOn(io)

    override fun search(query: String): Flow<List<Dua>> {
        val match = SearchNormalizer.toMatchQuery(query) ?: return flowOf(emptyList())
        return dao.search(match).map { list -> list.map { it.toDomain() } }.flowOn(io)
    }

    override suspend fun getDua(duaId: String): Dua? = withContext(io) { dao.getDua(duaId)?.toDomain() }

    override suspend fun getDuasInSection(sectionId: String): List<Dua> =
        withContext(io) { dao.getDuasInSection(sectionId).map { it.toDomain() } }

    override suspend fun getAllDuas(): List<Dua> = withContext(io) { dao.getAll().map { it.toDomain() } }
}
