package app.sukoon.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.ReaderList
import app.sukoon.domain.model.WidgetMode
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.repository.UserDataRepository
import app.sukoon.domain.usecase.ObserveReaderListUseCase
import app.sukoon.domain.usecase.ToggleFavoriteUseCase
import app.sukoon.ui.navigation.ReaderRoute
import app.sukoon.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderContent(
    val list: ReaderList,
    val sectionTitle: String?,
    val duas: List<Dua>,
    val initialIndex: Int,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val duaRepository: DuaRepository,
    private val userData: UserDataRepository,
    private val settingsRepository: SettingsRepository,
    private val observeReaderList: ObserveReaderListUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val route = savedState.toRoute<ReaderRoute>()
    private val retry = MutableStateFlow(0)

    private val currentDuaId = MutableStateFlow<String?>(savedState[KEY_CURRENT] ?: route.duaId)

    private val resolvedList = flow {
        val list = when (route.listType) {
            ReaderRoute.SECTION -> ReaderList.SectionList(route.listKey)
            ReaderRoute.FAVORITES -> ReaderList.Favorites
            ReaderRoute.COLLECTION -> ReaderList.Collection(route.listKey.toLongOrNull() ?: -1L)
            else -> {
                val dua = duaRepository.getDua(route.duaId) ?: throw NoSuchElementException(route.duaId)
                ReaderList.SectionList(dua.sectionId)
            }
        }
        emit(list)
    }

    val state: StateFlow<LoadState<ReaderContent>> = retry.flatMapLatest {
        resolvedList.flatMapLatest { list ->
            val titleFlow = (list as? ReaderList.SectionList)?.let { l ->
                duaRepository.observeSection(l.sectionId).map { it?.title }
            } ?: flowOf(null)
            combine(observeReaderList(list), titleFlow) { duas, title ->
                val target = currentDuaId.value ?: route.duaId
                val index = duas.indexOfFirst { it.id == target }.coerceAtLeast(0)
                LoadState.Success(ReaderContent(list, title, duas, index)) as LoadState<ReaderContent>
            }
        }.catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    val collections: StateFlow<List<DuaCollection>> = userData.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectionsOfCurrent: StateFlow<Set<Long>> = currentDuaId.filterNotNull()
        .flatMapLatest { userData.observeCollectionIdsContaining(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun onPageSettled(dua: Dua) {
        if (currentDuaId.value == dua.id) return
        currentDuaId.value = dua.id
        savedState[KEY_CURRENT] = dua.id
        rememberLastRead(dua.id)
    }

    fun onFirstShown(dua: Dua) {
        currentDuaId.value = dua.id
        rememberLastRead(dua.id)
    }

    private fun rememberLastRead(id: String) {
        viewModelScope.launch {
            settingsRepository.update { if (it.resumeLastRead) it.copy(lastReadDuaId = id) else it }
        }
    }

    fun toggleFavorite(dua: Dua) {
        viewModelScope.launch { toggleFavorite.invoke(dua) }
    }

    fun setInCollection(collectionId: Long, duaId: String, include: Boolean) {
        viewModelScope.launch {
            if (include) userData.addToCollection(collectionId, duaId) else userData.removeFromCollection(collectionId, duaId)
        }
    }

    fun createCollectionWith(name: String, duaId: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = userData.createCollection(name)
            userData.addToCollection(id, duaId)
        }
    }

    fun pinToWidget(duaId: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(widgetMode = WidgetMode.PINNED, widgetPinnedDuaId = duaId) }
            widgetUpdater.refresh()
        }
    }

    fun setShowArabic(v: Boolean) = updateSettings { it.copy(showArabic = v || (!it.showTransliteration && !it.showTranslation)) }
    fun setShowTransliteration(v: Boolean) = updateSettings { it.copy(showTransliteration = v || (!it.showArabic && !it.showTranslation)) }
    fun setShowTranslation(v: Boolean) = updateSettings { it.copy(showTranslation = v || (!it.showArabic && !it.showTransliteration)) }

    private fun updateSettings(transform: (app.sukoon.domain.model.UserSettings) -> app.sukoon.domain.model.UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun retry() { retry.value++ }

    private companion object {
        const val KEY_CURRENT = "current_dua"
    }
}
