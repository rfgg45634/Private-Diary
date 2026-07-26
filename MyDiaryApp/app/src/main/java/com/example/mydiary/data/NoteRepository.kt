package com.example.mydiary.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    fun getKnowledgeItems(): Flow<List<NoteEntity>> = dao.getKnowledgeItems()

    suspend fun getDiaryPage(page: Int, pageSize: Int, ascending: Boolean = false): List<NoteEntity> {
        val offset = (page - 1) * pageSize
        return if (ascending) {
            dao.getDiaryPageAsc(pageSize, offset)
        } else {
            dao.getDiaryPageDesc(pageSize, offset)
        }
    }

    suspend fun getDiaryCount(): Int = dao.getDiaryCount()

    /** 搜索/连续天数统计用：一次性拿到全部日记 */
    suspend fun getAllDiaryNotes(ascending: Boolean): List<NoteEntity> {
        return if (ascending) dao.getAllDiaryAsc() else dao.getAllDiaryDesc()
    }

    suspend fun getAllDiaryCreatedTimes(): List<Long> = dao.getAllDiaryCreatedTimes()

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun update(note: NoteEntity) = dao.update(note)

    suspend fun delete(note: NoteEntity) = dao.delete(note)

    fun getDeletedNotes(): Flow<List<NoteEntity>> {
        val threshold = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        return dao.getDeletedNotes(threshold)
    }

    suspend fun moveToRecycleBin(note: NoteEntity) {
        val updated = note.copy(
            isDeleted = true,
            deletedTime = System.currentTimeMillis()
        )
        dao.update(updated)
    }

    suspend fun restoreFromRecycleBin(note: NoteEntity) {
        val orderIndex = if (note.type == NoteType.KNOWLEDGE) {
            dao.getMaxKnowledgeOrderIndex() + 1
        } else {
            note.orderIndex
        }
        val updated = note.copy(
            isDeleted = false,
            deletedTime = 0,
            orderIndex = orderIndex
        )
        dao.update(updated)
    }

    suspend fun permanentlyDelete(note: NoteEntity) {
        dao.delete(note)
    }

    suspend fun pruneExpiredNotes() {
        val threshold = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        dao.pruneDeletedNotes(threshold)
    }

    suspend fun clearRecycleBin() {
        dao.clearRecycleBin()
    }

    /** 新建一条知识卡片，自动排在当前最后一位 */
    suspend fun addNewKnowledge(): NoteEntity {
        val maxIndex = dao.getMaxKnowledgeOrderIndex()
        val note = NoteEntity(type = NoteType.KNOWLEDGE, orderIndex = maxIndex + 1)
        val id = dao.insert(note)
        return note.copy(id = id)
    }

    /** 新建一条日记卡片 */
    suspend fun addNewDiary(): NoteEntity {
        val note = NoteEntity(type = NoteType.DIARY)
        val id = dao.insert(note)
        return note.copy(id = id)
    }

    /** 拖拽结束后持久化新的顺序 */
    suspend fun reorderKnowledge(notes: List<NoteEntity>) {
        val updated = notes.mapIndexed { index, note -> note.copy(orderIndex = index) }
        dao.updateAll(updated)
    }
}
