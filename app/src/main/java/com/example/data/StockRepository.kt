package com.example.data

import kotlinx.coroutines.flow.Flow

class StockRepository(private val stockDao: StockDao) {
    val allEntries: Flow<List<StockEntry>> = stockDao.getAllEntries()

    suspend fun insert(entry: StockEntry) {
        stockDao.insertEntry(entry)
    }

    suspend fun delete(entry: StockEntry) {
        stockDao.deleteEntry(entry)
    }

    suspend fun deleteById(id: Int) {
        stockDao.deleteEntryById(id)
    }

    suspend fun clear() {
        stockDao.clearAll()
    }
}
