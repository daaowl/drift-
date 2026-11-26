package app.sukoon.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object SavedRoute
@Serializable data object SettingsRoute

@Serializable data class SectionRoute(val sectionId: String)

/**
 * @param listType one of [ReaderRoute.SECTION], [ReaderRoute.FAVORITES], [ReaderRoute.COLLECTION], [ReaderRoute.AUTO].
 * AUTO resolves to the dua's own section (used for search results, notifications and the widget).
 */
@Serializable
data class ReaderRoute(
    val duaId: String,
    val listType: String = AUTO,
    val listKey: String = "",
) {
    companion object {
        const val SECTION = "section"
        const val FAVORITES = "favorites"
        const val COLLECTION = "collection"
        const val AUTO = "auto"
    }
}

@Serializable data class CounterRoute(val duaId: String)
@Serializable data class CollectionRoute(val collectionId: Long)
@Serializable data object PinSetupRoute
@Serializable data object AboutRoute
@Serializable data class DocumentRoute(val document: String) {
    companion object {
        const val LICENSES = "licenses"
        const val PRIVACY = "privacy"
        const val CHANGELOG = "changelog"
    }
}
