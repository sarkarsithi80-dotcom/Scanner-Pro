package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannerDao {

    @Query("SELECT * FROM documents WHERE isEncrypted = 0 ORDER BY updatedAt DESC")
    fun getAllPublicDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isEncrypted = 1 ORDER BY updatedAt DESC")
    fun getAllEncryptedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun observeDocumentById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE (title LIKE '%' || :query || '%' OR combinedOcrText LIKE '%' || :query || '%') AND isEncrypted = 0 ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    // Pages
    @Query("SELECT * FROM document_pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    fun getPagesForDocument(docId: Long): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    suspend fun getPagesListForDocument(docId: Long): List<DocumentPageEntity>

    @Query("SELECT * FROM document_pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: Long): DocumentPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: DocumentPageEntity)

    @Query("DELETE FROM document_pages WHERE id = :pageId")
    suspend fun deletePage(pageId: Long)
}
