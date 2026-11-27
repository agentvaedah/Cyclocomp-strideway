package id.web.agentvaedah.strideway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.web.agentvaedah.strideway.core.route.ElevationProfile
import id.web.agentvaedah.strideway.core.route.EventCutoff
import id.web.agentvaedah.strideway.data.db.EventRoute
import id.web.agentvaedah.strideway.data.prefs.SettingsRepository
import id.web.agentvaedah.strideway.data.repo.RouteRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class HomeEvent(
    val choices: List<EventRoute>,
    val route: EventRoute?,
    val profile: ElevationProfile? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class NextEventViewModel @Inject constructor(
    private val routes: RouteRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val event = combine(routes.eventChoices(), settings.homeEvent) { choices, key ->
        HomeEvent(choices, choices.firstOrNull { it.selectionKey() == key })
    }.distinctUntilChanged().flatMapLatest { state ->
        val route = state.route
        if (route == null) flowOf(state) else routes.elevationProfile(route.id)
            .map { profile -> state.copy(profile = profile) }
            .onStart { emit(state) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(route: EventRoute?) {
        viewModelScope.launch { settings.setHomeEvent(route?.selectionKey()) }
    }

    fun saveCutoff(route: EventRoute, cutoff: EventCutoff?) {
        viewModelScope.launch { routes.setCutoff(route.id, cutoff) }
    }
}
