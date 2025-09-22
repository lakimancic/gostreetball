package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.location.LocationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortType { NONE, NAME_ASC, NAME_DESC, RATING_ASC, RATING_DESC }

data class CourtUiState (
    val courts: List<Court> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTypes: Set<CourtType> = emptySet(),
    val selectedBoardTypes: Set<BoardType> = emptySet(),
    val searchQuery: String = "",
    val debounceQuery: String = "",
    val sortKey: SortType = SortType.NONE,
    val radius: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

@HiltViewModel
class CourtsViewModel @Inject constructor(
    locationRepository: LocationRepository,
    private val courtRepository: CourtRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CourtUiState())
    val uiState: StateFlow<CourtUiState> = _uiState.asStateFlow()

    private val _filteredCourts = MutableStateFlow<List<Court>>(emptyList())
    val filteredCourts: StateFlow<List<Court>> = _filteredCourts.asStateFlow()

    private var currLocation: LatLng = LatLng(0.0, 0.0)
    private var searchTypeJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.collect { updateFilteredCourts() }
        }
        viewModelScope.launch {
            locationRepository.currentLocation.collect { latLng ->
                currLocation = latLng?.let { LatLng(it.latitude, it.longitude) } ?: currLocation
            }
        }
        fetchCourts()
    }

    private fun updateFilteredCourts() {
        val query = _uiState.value.debounceQuery.trim().lowercase()
        val types = _uiState.value.selectedTypes
        val boardTypes = _uiState.value.selectedBoardTypes
        val startDate = _uiState.value.startDate
        val endDate = _uiState.value.endDate

        val filtered = _uiState.value.courts.filter { court ->
            val type = types.isEmpty() || types.contains(court.type)
            val boardType = boardTypes.isEmpty() || boardTypes.contains(court.boardType)

            val search = if (query.isEmpty()) true else {
                "${court.name} ${court.city} ${court.country}".lowercase().contains(query)
            }

            val dates = if (startDate != null && endDate != null) {
                court.createdAt >= startDate && court.createdAt <= endDate
            } else true

            type && boardType && search && dates
        }

        val sorted = when (_uiState.value.sortKey) {
            SortType.NONE -> filtered
            SortType.NAME_ASC -> filtered.sortedBy { it.name }
            SortType.NAME_DESC -> filtered.sortedByDescending { it.name }
            SortType.RATING_ASC -> filtered.sortedBy { it.rating }
            SortType.RATING_DESC -> filtered.sortedByDescending { it.rating }
        }

        _filteredCourts.value = sorted
    }

    fun toggleTypeFilter(type: CourtType) {
        val newSet = uiState.value.selectedTypes.toMutableSet().apply {
            if (contains(type)) remove(type) else add(type)
        }.toSet()
        _uiState.update { it.copy(selectedTypes = newSet) }
    }

    fun toggleBoardTypeFilter(type: BoardType) {
        val newSet = uiState.value.selectedBoardTypes.toMutableSet().apply {
            if (contains(type)) remove(type) else add(type)
        }.toSet()
        _uiState.update { it.copy(selectedBoardTypes = newSet) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchTypeJob?.cancel()
        searchTypeJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(debounceQuery = query) }
        }
    }

    fun setSortKey(key: SortType) {
        if (_uiState.value.sortKey != key) {
            _uiState.update { it.copy(sortKey = key) }
        }
    }

    fun setRadius(radius: Int?) {
        _uiState.update { it.copy(radius = radius) }
    }

    fun resetFilters() {
        _uiState.update { it.copy(
            selectedTypes = emptySet(),
            selectedBoardTypes = emptySet(),
            sortKey = SortType.NONE,
            radius = null,
            startDate = null,
            endDate = null
        ) }
    }

    fun setDateRange(start: Long?, end: Long?) {
        _uiState.update { it.copy(
            startDate = start,
            endDate = end
        ) }
    }

    fun fetchCourts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (uiState.value.radius != null) {
                courtRepository.getCourtsInRadius(
                    currLocation.latitude,
                    currLocation.longitude,
                    uiState.value.radius!!.toDouble()
                )
            } else {
                courtRepository.getAllCourts()
            }

            result.onSuccess { courts ->
                _uiState.update { it.copy(courts = courts, isLoading = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to fetch courts",
                        isLoading = false
                    )
                }
            }
        }
    }
}