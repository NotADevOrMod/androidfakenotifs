package com.example.fakenotifs

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifs")
data class NotifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val title: String,
    val message: String,
    val iconPath: String? = null,
    val imagePath: String? = null,
    val accent: Int = 0xFF4F46E5.toInt()
)

@Entity(tableName = "scheduled")
data class ScheduledEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notifId: Long,
    val triggerAt: Long
)

@Dao
interface AppDao {
    @Query("SELECT * FROM notifs ORDER BY id DESC")
    fun notifs(): Flow<List<NotifEntity>>

    @Query("SELECT * FROM notifs WHERE id = :id")
    suspend fun notif(id: Long): NotifEntity?

    @Query("SELECT COUNT(*) FROM notifs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(n: NotifEntity): Long

    @Delete
    suspend fun delete(n: NotifEntity)

    @Query("SELECT * FROM scheduled ORDER BY triggerAt")
    fun scheduledFlow(): Flow<List<ScheduledEntity>>

    @Query("SELECT * FROM scheduled")
    suspend fun scheduledAll(): List<ScheduledEntity>

    @Insert
    suspend fun addScheduled(s: ScheduledEntity): Long

    @Query("DELETE FROM scheduled WHERE id = :id")
    suspend fun removeScheduled(id: Long)

    @Query("DELETE FROM scheduled WHERE notifId = :nid")
    suspend fun removeScheduledFor(nid: Long)
}

@Database(entities = [NotifEntity::class, ScheduledEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "fakenotifs.db")
                .build().also { inst = it }
        }
    }
}
