package com.camera.studio.photoeditor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    /** 3 mục mới nhất hiển thị trên Home (nếu sau này cần — hiện `home.jpg` không có dải này). */
    val recentMedia: StateFlow<List<SavedMedia>> = mediaRepository.allMedia
        .map { it.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
