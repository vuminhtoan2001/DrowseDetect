package com.camera.studio.photoeditor.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camera.studio.photoeditor.core.UserSettings
import com.camera.studio.photoeditor.core.UserSettingsStore
import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** My Gallery — tab Photo/Video/Templates (docs/06_GALLERY_MANAGER.md). */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    private val userSettingsStore: UserSettingsStore,
) : ViewModel() {

    val allMedia: StateFlow<List<SavedMedia>> = mediaRepository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<SavedMedia>> = mediaRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userSettings: StateFlow<UserSettings> = userSettingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setViewAllDeviceMedia(enabled: Boolean) {
        viewModelScope.launch { userSettingsStore.setViewAllDeviceMedia(enabled) }
    }
}
