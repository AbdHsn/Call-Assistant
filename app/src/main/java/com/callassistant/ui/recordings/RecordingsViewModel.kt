package com.callassistant.ui.recordings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.util.CallRecorder
import com.callassistant.util.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _recordings.value = CallRecorder.listRecordings(context)
        }
    }

    suspend fun deleteRecording(recording: Recording): Boolean = withContext(Dispatchers.IO) {
        val deleted = recording.file.delete()
        if (deleted) {
            _recordings.value = CallRecorder.listRecordings(context)
        }
        deleted
    }
}
