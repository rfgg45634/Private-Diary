package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: NoteType,
    // 标题为 null 表示用户未自定义，展示时会用创建时间代替
    var title: String? = null,
    var content: String = "",
    val createdTime: Long = System.currentTimeMillis(),
    var updatedTime: Long = System.currentTimeMillis(),
    // 仅知识模块使用：拖拽排序的顺序号，不受编辑等操作影响
    var orderIndex: Int = 0,
    // 是否已被删除移入回收站
    val isDeleted: Boolean = false,
    // 移入回收站的时间戳
    val deletedTime: Long = 0
)
