package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val pageCount: Int = 1,
    val thumbnailPath: String = "",
    val categoryTag: String = "Document",
    val isFavorite: Boolean = false,
    val combinedOcrText: String = ""
)
