package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stock_entries")
data class StockEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val size: String,
    val weightKg: Double,
    val pieces: Int = 0,      // Number of physical bag pieces
    val counter: Int = 0,     // Machine or operator batch counter (Bags Count)
    val piecesPerBag: Int = 0,  // Number of pieces (ፍሬ) in one bag
    val kgPerBag: Double = 0.0, // Weight in kg of one bag
    val type: String, // "FABRICATED" or "SOLD"
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_FABRICATED = "FABRICATED"
        const val TYPE_SOLD = "SOLD"
    }
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<StockEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: StockEntry)

    @Delete
    suspend fun deleteEntry(entry: StockEntry)

    @Query("DELETE FROM stock_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)

    @Query("DELETE FROM stock_entries")
    suspend fun clearAll()
}

@Database(entities = [StockEntry::class], version = 3, exportSchema = false)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getDatabase(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
