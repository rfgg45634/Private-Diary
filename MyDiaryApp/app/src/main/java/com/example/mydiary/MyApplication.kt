package com.example.mydiary

import android.app.Application
import com.example.mydiary.data.AppDatabase
import com.example.mydiary.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {

    val repository: NoteRepository by lazy {
        NoteRepository(AppDatabase.getInstance(this).noteDao())
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 每次启动时清理回收站里超过7天的内容
        applicationScope.launch {
            repository.pruneExpiredNotes()
        }
    }
}
