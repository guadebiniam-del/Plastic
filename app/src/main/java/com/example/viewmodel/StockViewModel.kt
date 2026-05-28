package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StockDatabase
import com.example.data.StockEntry
import com.example.data.StockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemStockSummary(
    val itemName: String,
    val size: String,
    val totalFabricatedKg: Double,
    val totalSoldKg: Double,
    val netStockKg: Double,
    val totalFabricatedPieces: Int,
    val totalSoldPieces: Int,
    val netStockPieces: Int,
    val latestCounter: Int
)

data class OverallSummary(
    val totalFabricatedKg: Double,
    val totalSoldKg: Double,
    val netStockKg: Double,
    val totalFabricatedPieces: Int,
    val totalSoldPieces: Int,
    val netStockPieces: Int
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    
    // Predefined plastic bag item categories for quick selection
    val predefinedItems = listOf(
        "HDPE T-Shirt Bag",
        "LDPE Flat Bag",
        "PP High-Clarity Bag",
        "Biodegradable Carrier",
        "Drawstring Garbage Bag",
        "Heavy Duty Poly Sack",
        "Zipper Stand-up Pouch"
    )

    // Predefined bag sizes for quick Selection
    val predefinedSizes = listOf(
        "20 x 30 cm",
        "25 x 40 cm",
        "30 x 45 cm",
        "35 x 50 cm",
        "40 x 60 cm",
        "50 x 75 cm",
        "60 x 90 cm",
        "8 x 12 inches",
        "12 x 18 inches",
        "18 x 24 inches"
    )

    init {
        val stockDao = StockDatabase.getDatabase(application).stockDao()
        repository = StockRepository(stockDao)
    }

    // Direct stream from database
    val allEntries: StateFlow<List<StockEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<String?>(null) // null = ALL, "FABRICATED", "SOLD"
    val filterType = _filterType.asStateFlow()

    private val _filterItem = MutableStateFlow<String?>(null) // null = ALL
    val filterItem = _filterItem.asStateFlow()

    // Filtered transaction history
    val filteredEntries: StateFlow<List<StockEntry>> = combine(
        allEntries,
        searchQuery,
        filterType,
        filterItem
    ) { entries, query, type, item ->
        entries.filter { entry ->
            val matchesQuery = query.isEmpty() || 
                entry.itemName.contains(query, ignoreCase = true) ||
                entry.size.contains(query, ignoreCase = true)
            val matchesType = type == null || entry.type == type
            val matchesItem = item == null || entry.itemName == item
            matchesQuery && matchesType && matchesItem
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock Summaries grouped by Item and Size
    val stockSummaries: StateFlow<List<ItemStockSummary>> = allEntries.map { entries ->
        val groups = entries.groupBy { it.itemName.lowercase().trim() to it.size.lowercase().trim() }
        
        groups.map { (key, groupEntries) ->
            // Use the original capitalized item name and size from the first item in the group
            val originalName = groupEntries.first().itemName
            val originalSize = groupEntries.first().size
            
            val totalFabricated = groupEntries
                .filter { it.type == StockEntry.TYPE_FABRICATED }
                .sumOf { it.weightKg }
            
            val totalSold = groupEntries
                .filter { it.type == StockEntry.TYPE_SOLD }
                .sumOf { it.weightKg }

            val totalFabricatedPcs = groupEntries
                .filter { it.type == StockEntry.TYPE_FABRICATED }
                .sumOf { it.pieces }
            
            val totalSoldPcs = groupEntries
                .filter { it.type == StockEntry.TYPE_SOLD }
                .sumOf { it.pieces }

            val latestCtr = groupEntries
                .filter { it.type == StockEntry.TYPE_FABRICATED }
                .maxOfOrNull { it.counter } ?: 0
            
            ItemStockSummary(
                itemName = originalName,
                size = originalSize,
                totalFabricatedKg = totalFabricated,
                totalSoldKg = totalSold,
                netStockKg = totalFabricated - totalSold,
                totalFabricatedPieces = totalFabricatedPcs,
                totalSoldPieces = totalSoldPcs,
                netStockPieces = totalFabricatedPcs - totalSoldPcs,
                latestCounter = latestCtr
            )
        }.sortedWith(compareBy({ it.itemName }, { it.size }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Total Summary across all items and sizes
    val overallSummary: StateFlow<OverallSummary> = stockSummaries.map { summaries ->
        val totalFabricated = summaries.sumOf { it.totalFabricatedKg }
        val totalSold = summaries.sumOf { it.totalSoldKg }
        val totalFabricatedPcs = summaries.sumOf { it.totalFabricatedPieces }
        val totalSoldPcs = summaries.sumOf { it.totalSoldPieces }
        OverallSummary(
            totalFabricatedKg = totalFabricated,
            totalSoldKg = totalSold,
            netStockKg = totalFabricated - totalSold,
            totalFabricatedPieces = totalFabricatedPcs,
            totalSoldPieces = totalSoldPcs,
            netStockPieces = totalFabricatedPcs - totalSoldPcs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallSummary(0.0, 0.0, 0.0, 0, 0, 0))

    // Operations
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String?) {
        _filterType.value = type
    }

    fun setFilterItem(item: String?) {
        _filterItem.value = item
    }

    fun addEntry(name: String, size: String, weight: Double, pieces: Int, counter: Int, type: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && size.isNotBlank() && weight > 0) {
                repository.insert(
                    StockEntry(
                        itemName = name.trim(),
                        size = size.trim(),
                        weightKg = weight,
                        pieces = pieces,
                        counter = counter,
                        type = type
                    )
                )
            }
        }
    }

    fun deleteEntry(entry: StockEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    fun deleteEntryById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.clear()
            
            // Seed sample plastic bag fabrications and sales with piece count & machine counters!
            val samples = listOf(
                StockEntry(
                    itemName = "HDPE T-Shirt Bag", 
                    size = "30 x 45 cm", 
                    weightKg = 450.0, 
                    pieces = 15000, 
                    counter = 120500, 
                    type = StockEntry.TYPE_FABRICATED, 
                    timestamp = System.currentTimeMillis() - 72000000
                ),
                StockEntry(
                    itemName = "HDPE T-Shirt Bag", 
                    size = "30 x 45 cm", 
                    weightKg = 120.0, 
                    pieces = 4000, 
                    counter = 0, 
                    type = StockEntry.TYPE_SOLD, 
                    timestamp = System.currentTimeMillis() - 60000000
                ),
                StockEntry(
                    itemName = "LDPE Flat Bag", 
                    size = "25 x 40 cm", 
                    weightKg = 850.0, 
                    pieces = 25000, 
                    counter = 84100, 
                    type = StockEntry.TYPE_FABRICATED, 
                    timestamp = System.currentTimeMillis() - 54000000
                ),
                StockEntry(
                    itemName = "LDPE Flat Bag", 
                    size = "25 x 40 cm", 
                    weightKg = 340.0, 
                    pieces = 10000, 
                    counter = 0, 
                    type = StockEntry.TYPE_SOLD, 
                    timestamp = System.currentTimeMillis() - 48000000
                ),
                StockEntry(
                    itemName = "PP High-Clarity Bag", 
                    size = "40 x 60 cm", 
                    weightKg = 1200.0, 
                    pieces = 18000, 
                    counter = 45200, 
                    type = StockEntry.TYPE_FABRICATED, 
                    timestamp = System.currentTimeMillis() - 42000000
                ),
                StockEntry(
                    itemName = "PP High-Clarity Bag", 
                    size = "40 x 60 cm", 
                    weightKg = 400.0, 
                    pieces = 6000, 
                    counter = 0, 
                    type = StockEntry.TYPE_SOLD, 
                    timestamp = System.currentTimeMillis() - 36000000
                )
            )
            
            samples.forEach { repository.insert(it) }
        }
    }
}
