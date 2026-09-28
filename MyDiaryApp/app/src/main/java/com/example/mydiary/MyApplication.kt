package com.example.mydiary

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
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
        // 关键：恢复用户上次手动选择的日/夜间模式。
        // 必须在任何 Activity 创建之前调用，否则 App 重启后又会回到系统默认（日间）模式。
        applySavedNightMode()
        super.onCreate()
        // 每次启动时清理回收站里超过7天的内容
        applicationScope.launch {
            repository.pruneExpiredNotes()
        }
    }

    private fun applySavedNightMode() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        // 默认值 MODE_NIGHT_UNSPECIFIED(-100) 表示跟随系统；用户点过切换按钮后才会落库
        val savedMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
        AppCompatDelegate.setDefaultNightMode(savedMode)
    }
}
