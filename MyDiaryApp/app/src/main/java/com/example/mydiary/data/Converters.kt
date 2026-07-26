package com.example.mydiary.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = NoteType.valueOf(value)
}
