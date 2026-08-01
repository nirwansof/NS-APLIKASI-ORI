package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Ayat(
    val no: String,
    val arab: String,
    val latin: String,
    val terjemahan: String
)

@Entity(tableName = "prayers")
data class PrayerItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "doa", "sholat", "sunnah", "qiyamul"
    val title: String,
    val ayatListJson: String, // JSON serialized list of Ayat
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val subCategory: String = "Umum"
)

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val title: String,
    val ayatListJson: String
)
