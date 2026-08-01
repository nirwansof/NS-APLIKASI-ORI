package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayers ORDER BY id ASC")
    fun getAllPrayers(): Flow<List<PrayerItem>>

    @Query("SELECT * FROM prayers WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavoritePrayers(): Flow<List<PrayerItem>>

    @Query("UPDATE prayers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("SELECT * FROM prayers WHERE category = :category ORDER BY id ASC")
    fun getPrayersByCategory(category: String): Flow<List<PrayerItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayer(prayer: PrayerItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayers(prayers: List<PrayerItem>)

    @Query("DELETE FROM prayers WHERE id = :id")
    suspend fun deletePrayerById(id: Int)

    @Query("DELETE FROM prayers WHERE category = :category AND isCustom = 0")
    suspend fun deleteDefaultPrayersByCategory(category: String)

    @Query("DELETE FROM prayers WHERE isCustom = 1")
    suspend fun deleteAllCustomPrayers()

    // Sync Queue Methods
    @Query("SELECT * FROM sync_queue ORDER BY id ASC")
    fun getSyncQueue(): Flow<List<SyncQueueItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncQueueItem(id: Int)

    @Query("DELETE FROM sync_queue")
    suspend fun clearSyncQueue()
}
