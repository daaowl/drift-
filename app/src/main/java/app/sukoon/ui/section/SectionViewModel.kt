package app.sukoon.ui.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.Section
import app.sukoon.domain.usecase.ObserveSectionDuasUseCase
import app.sukoon.domain.usecase.ObserveSectionUseCase
import app.sukoon.domain.usecase.ToggleFavoriteUseCase
import app.sukoon.ui.navigation.SectionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionContent(val section: Section, val duas: List<Dua>)

@HiltViewModel
class SectionViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    observeSection: ObserveSectionUseCase,
    observeDuas: ObserveSectionDuasUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {
    val sectionId: String = savedState.toRoute<SectionRoute>().sectionId
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<SectionContent>> = retry.flatMapLatest {
        combine(observeSection(sectionId), observeDuas(sectionId)) { section, duas ->
            if (section == null) LoadState.Error(NoSuchElementException(sectionId))
            else LoadState.Success(SectionContent(section, duas))
        }.catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    /** Selected dua in the two-pane layout (expanded width). Survives process death. */
    private val _selected = MutableStateFlow<String?>(savedState[KEY_SELECTED])
    val selected: StateFlow<String?> = _selected.asStateFlow()

    fun select(duaId: String) {
        _selected.value = duaId
        savedState[KEY_SELECTED] = duaId
    }

    fun toggleFavorite(dua: Dua) {
        viewModelScope.launch { toggleFavorite.invoke(dua) }
    }

    fun retry() { retry.value++ }

    private companion object {
        const val KEY_SELECTED = "selected"
    }
}
