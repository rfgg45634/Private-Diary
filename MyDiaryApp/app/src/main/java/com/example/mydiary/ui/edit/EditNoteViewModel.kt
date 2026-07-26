package com.example.mydiary.ui.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.data.NoteRepository
import kotlinx.coroutines.launch

class EditNoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _note = MutableLiveData<NoteEntity?>()
    val note: LiveData<NoteEntity?> = _note

    fun load(id: Long) {
        viewModelScope.launch {
            _note.value = repository.getNoteById(id)
        }
    }

    /** 实时保存：标题/正文变化即触发，只在有实际内容变更时落库，并复用内存缓存 */
    fun save(id: Long, title: String?, content: String) {
        val current = _note.value ?: return
        val cleanTitle = if (title.isNullOrBlank()) null else title
        // 若没有实质性内容修改，则跳过保存
        if (current.title == cleanTitle && current.content == content) {
            return
        }
        viewModelScope.launch {
            val updated = current.copy(
                title = cleanTitle,
                content = content,
                updatedTime = System.currentTimeMillis()
            )
            repository.update(updated)
            // 更新当前内存缓存，确保下一次无差分比对正常工作
            _note.value = updated
        }
    }

    fun delete(note: NoteEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            // 改为移入回收站逻辑
            repository.moveToRecycleBin(note)
            onDeleted()
        }
    }

    /**
     * 标题和正文都是空的情况下，说明用户只是点了"新建"看看，什么都没写就退出了，
     * 这种记录直接彻底删除，不进回收站（回收站是给"曾经写过内容后来删掉"的场景准备的）。
     */
    fun discardIfEmpty(id: Long) {
        viewModelScope.launch {
            val current = repository.getNoteById(id) ?: return@launch
            if (current.title.isNullOrBlank() && current.content.isBlank()) {
                repository.delete(current)
            }
        }
    }
}

class EditNoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EditNoteViewModel(repository) as T
    }
}
