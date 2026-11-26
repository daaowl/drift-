package app.sukoon.ui.saved

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.repository.UserDataRepository
import app.sukoon.domain.usecase.ToggleFavoriteUseCase
import app.sukoon.ui.navigation.CollectionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val userData: UserDataRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val tab: StateFlow<Int> = savedState.getStateFlow(KEY_TAB, 0)

    val favorites: StateFlow<LoadState<List<Dua>>> = retry.flatMapLatest {
        userData.observeFavorites()
            .map { LoadState.Success(it) as LoadState<List<Dua>> }
            .catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    val collections: StateFlow<LoadState<List<DuaCollection>>> = retry.flatMapLatest {
        userData.observeCollections()
            .map { LoadState.Success(it) as LoadState<List<DuaCollection>> }
            .catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun selectTab(index: Int) { savedState[KEY_TAB] = index }

    fun toggleFavorite(dua: Dua) { viewModelScope.launch { toggleFavorite.invoke(dua) } }

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { userData.createCollection(name) }
    }

    fun retry() { retry.value++ }

    private companion object { const val KEY_TAB = "tab" }
}

data class CollectionContent(val collection: DuaCollection, val duas: List<Dua>)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val userData: UserDataRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {
    private val collectionId = savedState.toRoute<CollectionRoute>().collectionId
    private val retry = MutableStateFlow(0)

    /** Success(null) means the collection was deleted. */
    val state: StateFlow<LoadState<CollectionContent?>> = retry.flatMapLatest {
        combine(userData.observeCollection(collectionId), userData.observeCollectionDuas(collectionId)) { c, duas ->
            LoadState.Success(c?.let { CollectionContent(it, duas) }) as LoadState<CollectionContent?>
        }.catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { userData.renameCollection(collectionId, name) }
    }

    fun delete() { viewModelScope.launch { userData.deleteCollection(collectionId) } }

    fun remove(dua: Dua) { viewModelScope.launch { userData.removeFromCollection(collectionId, dua.id) } }

    fun undoRemove(dua: Dua) { viewModelScope.launch { userData.addToCollection(collectionId, dua.id) } }

    fun toggleFavorite(dua: Dua) { viewModelScope.launch { toggleFavorite.invoke(dua) } }

    fun retry() { retry.value++ }
}
