package com.example.mydiary.ui.knowledge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.data.NoteRepository
import com.example.mydiary.util.DateUtils
import kotlinx.coroutines.launch

class KnowledgeViewModel(private val repository: NoteRepository) : ViewModel() {

    private val allKnowledgeItems: LiveData<List<NoteEntity>> = repository.getKnowledgeItems().asLiveData()

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    /** 按标题过滤后的列表；没有搜索词时就是全部内容 */
    val knowledgeItems: MediatorLiveData<List<NoteEntity>> = MediatorLiveData<List<NoteEntity>>().apply {
        fun recompute() {
            val all = allKnowledgeItems.value.orEmpty()
            val query = _searchQuery.value.orEmpty().trim()
            value = if (query.isEmpty()) {
                all
            } else {
                all.filter { note ->
                    DateUtils.displayTitle(note.title, note.createdTime).contains(query, ignoreCase = true)
                }
            }
        }
        addSource(allKnowledgeItems) { recompute() }
        addSource(_searchQuery) { recompute() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNewKnowledge(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val note = repository.addNewKnowledge()
            onCreated(note.id)
        }
    }

    /** 拖拽结束后调用，持久化新顺序；不会因为其他操作被覆盖 */
    fun reorder(notes: List<NoteEntity>) {
        viewModelScope.launch {
            repository.reorderKnowledge(notes)
        }
    }

    /** 移入回收站（从列表滑动删除时调用） */
    fun moveToRecycleBin(note: NoteEntity) {
        viewModelScope.launch {
            repository.moveToRecycleBin(note)
        }
    }

    fun restore(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(note)
        }
    }
}

class KnowledgeViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return KnowledgeViewModel(repository) as T
    }
}
