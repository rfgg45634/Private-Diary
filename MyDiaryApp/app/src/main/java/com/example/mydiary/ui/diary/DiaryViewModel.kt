package com.example.mydiary.ui.diary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.data.NoteRepository
import com.example.mydiary.util.DateUtils
import kotlinx.coroutines.launch

class DiaryViewModel(private val repository: NoteRepository) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val _currentPage = MutableLiveData(1)
    val currentPage: LiveData<Int> = _currentPage

    private val _totalPages = MutableLiveData(1)
    val totalPages: LiveData<Int> = _totalPages

    private val _pageItems = MutableLiveData<List<NoteEntity>>(emptyList())
    val pageItems: LiveData<List<NoteEntity>> = _pageItems

    private val _sortAscending = MutableLiveData(false)
    val sortAscending: LiveData<Boolean> = _sortAscending

    private val _streakDays = MutableLiveData(0)
    val streakDays: LiveData<Int> = _streakDays

    private val _totalDays = MutableLiveData(0)
    val totalDays: LiveData<Int> = _totalDays

    // ---------- 搜索 ----------
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _searchResults = MutableLiveData<List<NoteEntity>>(emptyList())
    val searchResults: LiveData<List<NoteEntity>> = _searchResults

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    init {
        refresh()
        refreshStreak()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val all = repository.getAllDiaryNotes(_sortAscending.value ?: false)
            _searchResults.value = all.filter { note ->
                DateUtils.displayTitle(note.title, note.createdTime).contains(query, ignoreCase = true)
                    || note.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    private fun refreshStreak() {
        viewModelScope.launch {
            val times = repository.getAllDiaryCreatedTimes()
            _streakDays.value = DateUtils.calculateStreak(times)
            _totalDays.value = DateUtils.countDistinctDays(times)
        }
    }

    /** 切换升序/降序，切换后回到第一页，避免页码在两种排序下对不上 */
    fun toggleSortOrder() {
        _sortAscending.value = !(_sortAscending.value ?: false)
        refresh(goToFirstPage = true)
        // 搜索中的话也要按新的排序方向重新搜一遍
        if (_isSearching.value == true) {
            setSearchQuery(_searchQuery.value.orEmpty())
        }
    }

    /** 重新计算总页数；默认停留在当前页（除非页码超出范围或强制回到第一页） */
    fun refresh(goToFirstPage: Boolean = false) {
        viewModelScope.launch {
            val count = repository.getDiaryCount()
            val total = maxOf(1, (count + PAGE_SIZE - 1) / PAGE_SIZE)
            _totalPages.value = total
            val page = if (goToFirstPage) 1 else minOf(_currentPage.value ?: 1, total)
            _currentPage.value = page
            loadPage(page)
        }
        refreshStreak()
    }

    fun nextPage() {
        val page = _currentPage.value ?: 1
        val total = _totalPages.value ?: 1
        if (page < total) {
            _currentPage.value = page + 1
            loadPage(page + 1)
        }
    }

    fun prevPage() {
        val page = _currentPage.value ?: 1
        if (page > 1) {
            _currentPage.value = page - 1
            loadPage(page - 1)
        }
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            _pageItems.value = repository.getDiaryPage(page, PAGE_SIZE, _sortAscending.value ?: false)
        }
    }

    fun addNewDiary(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val note = repository.addNewDiary()
            onCreated(note.id)
        }
    }

    /** 移入回收站（从列表滑动删除时调用） */
    fun moveToRecycleBin(note: NoteEntity) {
        viewModelScope.launch {
            repository.moveToRecycleBin(note)
            refresh()
        }
    }

    fun restore(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(note)
            refresh()
        }
    }

    fun isSearchingValue(): Boolean = _isSearching.value ?: false
}

class DiaryViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DiaryViewModel(repository) as T
    }
}
