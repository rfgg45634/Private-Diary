package com.example.mydiary.ui.recycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.data.NoteRepository
import kotlinx.coroutines.launch

class RecycleBinViewModel(private val repository: NoteRepository) : ViewModel() {

    val deletedNotes: LiveData<List<NoteEntity>> = repository.getDeletedNotes().asLiveData()

    fun restore(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(note)
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.permanentlyDelete(note)
        }
    }

    fun emptyAll() {
        viewModelScope.launch {
            repository.clearRecycleBin()
        }
    }

    fun pruneExpired() {
        viewModelScope.launch {
            repository.pruneExpiredNotes()
        }
    }
}

class RecycleBinViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecycleBinViewModel(repository) as T
    }
}
