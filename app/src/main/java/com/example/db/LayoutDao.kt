package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutDao {
    @Query("SELECT * FROM layouts ORDER BY lastModified DESC")
    fun getAllLayouts(): Flow<List<LayoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: LayoutEntity)

    @Query("DELETE FROM layouts WHERE id = :id AND isPreset = 0")
    suspend fun deleteLayoutById(id: String)

    @Query("SELECT * FROM layouts WHERE id = :id")
    suspend fun getLayoutById(id: String): LayoutEntity?
}
