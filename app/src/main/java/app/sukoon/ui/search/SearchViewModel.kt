package app.sukoon.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.Section
import app.sukoon.domain.usecase.ObserveSectionsUseCase
import app.sukoon.domain.usecase.SearchDuasUseCase
import app.sukoon.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** null results = no query yet (show suggestions). */
data class SearchResults(val query: String, val duas: List<Dua>?)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    search: SearchDuasUseCase,
    observeSections: ObserveSectionsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    val query: StateFlow<String> = savedState.getStateFlow(KEY_QUERY, "")

    val results: StateFlow<LoadState<List<Dua>?>> = search(query)
        .map { LoadState.Success(it) as LoadState<List<Dua>?> }
        .catch { emit(LoadState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Success(null))

    val sectionTitles: StateFlow<Map<String, String>> = observeSections()
        .map { list -> list.associate { s: Section -> s.id to s.title } }
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun onQueryChange(value: String) {
        savedState[KEY_QUERY] = value.take(80)
    }

    fun toggleFavorite(dua: Dua) {
        viewModelScope.launch { toggleFavorite.invoke(dua) }
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}
