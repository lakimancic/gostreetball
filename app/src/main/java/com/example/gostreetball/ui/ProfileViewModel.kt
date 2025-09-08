package com.example.gostreetball.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val application: Application,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _isTrackingOn = MutableStateFlow(false)
    val isTrackingOn: StateFlow<Boolean> = _isTrackingOn.asStateFlow()

    private val _checkRadius = MutableStateFlow(100)
    val checkRadius: StateFlow<Int> = _checkRadius.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isTrackingEnabled.collectLatest { enabled ->
                _isTrackingOn.value = enabled
            }
        }

        viewModelScope.launch {
            preferences.checkRadiusMeters.collectLatest { radius ->
                _checkRadius.value = radius
            }
        }
    }
}