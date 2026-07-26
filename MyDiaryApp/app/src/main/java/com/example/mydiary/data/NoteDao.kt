package com.example.mydiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    // ---------- 知识模块：按自定义顺序展示，一次性全部加载（不分页） ----------
    @Query("SELECT * FROM notes WHERE type = 'KNOWLEDGE' AND isDeleted = 0 ORDER BY orderIndex ASC")
    fun getKnowledgeItems(): Flow<List<NoteEntity>>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM notes WHERE type = 'KNOWLEDGE' AND isDeleted = 0")
    suspend fun getMaxKnowledgeOrderIndex(): Int

    @Update
    suspend fun updateAll(notes: List<NoteEntity>)

    // ---------- 日记模块：按创建时间排序，分页加载；支持升序/降序切换 ----------
    @Query("SELECT * FROM notes WHERE type = 'DIARY' AND isDeleted = 0 ORDER BY createdTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getDiaryPageDesc(limit: Int, offset: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE type = 'DIARY' AND isDeleted = 0 ORDER BY createdTime ASC LIMIT :limit OFFSET :offset")
    suspend fun getDiaryPageAsc(limit: Int, offset: Int): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE type = 'DIARY' AND isDeleted = 0")
    suspend fun getDiaryCount(): Int

    // 搜索用：一次性拿到全部日记（不分页），在内存里按标题过滤
    @Query("SELECT * FROM notes WHERE type = 'DIARY' AND isDeleted = 0 ORDER BY createdTime DESC")
    suspend fun getAllDiaryDesc(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE type = 'DIARY' AND isDeleted = 0 ORDER BY createdTime ASC")
    suspend fun getAllDiaryAsc(): List<NoteEntity>

    // 连续记录天数统计用：只需要创建时间列表
    @Query("SELECT createdTime FROM notes WHERE type = 'DIARY' AND isDeleted = 0")
    suspend fun getAllDiaryCreatedTimes(): List<Long>

    // ---------- 回收站模块：加载 7 天内的被删除卡片，或者物理删除、清空等 ----------
    @Query("SELECT * FROM notes WHERE isDeleted = 1 AND deletedTime >= :threshold ORDER BY deletedTime DESC")
    fun getDeletedNotes(threshold: Long): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedTime < :threshold")
    suspend fun pruneDeletedNotes(threshold: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun clearRecycleBin()
}
