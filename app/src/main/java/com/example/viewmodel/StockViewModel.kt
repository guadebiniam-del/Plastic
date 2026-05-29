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
    val totalFabricatedBags: Int,
    val totalSoldBags: Int,
    val netStockBags: Int,
    val totalFabricatedPieces: Int,
    val totalSoldPieces: Int,
    val netStockPieces: Int,
    val latestCounter: Int
)

data class OverallSummary(
    val totalFabricatedKg: Double,
    val totalSoldKg: Double,
    val netStockKg: Double,
    val totalFabricatedBags: Int,
    val totalSoldBags: Int,
    val netStockBags: Int,
    val totalFabricatedPieces: Int,
    val totalSoldPieces: Int,
    val netStockPieces: Int
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    
    // Predefined plastic bag item categories for quick selection
    val predefinedItems = listOf(
        "DPunch Bag",
        "HDPE T-Shirt Bag",
        "LDPE Flat Bag",
        "PP High-Clarity Bag",
        "Drawstring Garbage Bag",
        "Heavy Duty Poly Sack"
    )

    // Predefined bag sizes for quick Selection
    val predefinedSizes = listOf(
        "35×48",
        "44×79",
        "20 x 30 cm",
        "25 x 40 cm",
        "30 x 45 cm",
        "40 x 60 cm",
        "12 x 18 inches"
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

            val totalFabricatedBags = groupEntries
                .filter { it.type == StockEntry.TYPE_FABRICATED }
                .sumOf { it.counter }
            
            val totalSoldBags = groupEntries
                .filter { it.type == StockEntry.TYPE_SOLD }
                .sumOf { it.counter }

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
                totalFabricatedBags = totalFabricatedBags,
                totalSoldBags = totalSoldBags,
                netStockBags = totalFabricatedBags - totalSoldBags,
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
        val totalFabricatedBags = summaries.sumOf { it.totalFabricatedBags }
        val totalSoldBags = summaries.sumOf { it.totalSoldBags }
        val totalFabricatedPcs = summaries.sumOf { it.totalFabricatedPieces }
        val totalSoldPcs = summaries.sumOf { it.totalSoldPieces }
        OverallSummary(
            totalFabricatedKg = totalFabricated,
            totalSoldKg = totalSold,
            netStockKg = totalFabricated - totalSold,
            totalFabricatedBags = totalFabricatedBags,
            totalSoldBags = totalSoldBags,
            netStockBags = totalFabricatedBags - totalSoldBags,
            totalFabricatedPieces = totalFabricatedPcs,
            totalSoldPieces = totalSoldPcs,
            netStockPieces = totalFabricatedPcs - totalSoldPcs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallSummary(0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0))

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

    fun addEntry(
        name: String,
        size: String,
        bagsCount: Int,
        piecesPerBag: Int,
        kgPerBag: Double,
        type: String
    ) {
        viewModelScope.launch {
            if (name.isNotBlank() && size.isNotBlank() && bagsCount > 0) {
                val totalWeight = bagsCount * kgPerBag
                val totalPieces = bagsCount * piecesPerBag
                repository.insert(
                    StockEntry(
                        itemName = name.trim(),
                        size = size.trim(),
                        weightKg = totalWeight,
                        pieces = totalPieces,
                        counter = bagsCount,
                        piecesPerBag = piecesPerBag,
                        kgPerBag = kgPerBag,
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
            
            // Seed sample plastic bag fabrications and sales with exact pieces & bag counters!
            val samples = listOf(
                // DPunch size 35×48, Fabricated: 50 bags, 30 pieces/bag, 15.0 kg/bag
                StockEntry(
                    itemName = "DPunch Bag",
                    size = "35×48",
                    weightKg = 50 * 15.0,
                    pieces = 50 * 30,
                    counter = 50,
                    piecesPerBag = 30,
                    kgPerBag = 15.0,
                    type = StockEntry.TYPE_FABRICATED,
                    timestamp = System.currentTimeMillis() - 72000000
                ),
                // DPunch size 35×48, Sold: 12 bags, 30 pieces/bag, 15.0 kg/bag
                StockEntry(
                    itemName = "DPunch Bag",
                    size = "35×48",
                    weightKg = 12 * 15.0,
                    pieces = 12 * 30,
                    counter = 12,
                    piecesPerBag = 30,
                    kgPerBag = 15.0,
                    type = StockEntry.TYPE_SOLD,
                    timestamp = System.currentTimeMillis() - 60000000
                ),
                // DPunch size 44×79, Fabricated: 20 bags, 50 pieces/bag, 17.5 kg/bag
                StockEntry(
                    itemName = "DPunch Bag",
                    size = "44×79",
                    weightKg = 20 * 17.5,
                    pieces = 20 * 50,
                    counter = 20,
                    piecesPerBag = 50,
                    kgPerBag = 17.5,
                    type = StockEntry.TYPE_FABRICATED,
                    timestamp = System.currentTimeMillis() - 54000000
                ),
                // DPunch size 44×79, Sold: 5 bags, 50 pieces/bag, 17.5 kg/bag
                StockEntry(
                    itemName = "DPunch Bag",
                    size = "44×79",
                    weightKg = 5 * 17.5,
                    pieces = 5 * 50,
                    counter = 5,
                    piecesPerBag = 50,
                    kgPerBag = 17.5,
                    type = StockEntry.TYPE_SOLD,
                    timestamp = System.currentTimeMillis() - 48000000
                ),
                // T-Shirt Bag, size 30 x 45 cm, Fabricated: 100 bags, 100 pcs/bag, 8.5 kg/bag
                StockEntry(
                    itemName = "HDPE T-Shirt Bag",
                    size = "30 x 45 cm",
                    weightKg = 100 * 8.5,
                    pieces = 100 * 100,
                    counter = 100,
                    piecesPerBag = 100,
                    kgPerBag = 8.5,
                    type = StockEntry.TYPE_FABRICATED,
                    timestamp = System.currentTimeMillis() - 42000000
                ),
                // T-Shirt Bag, size 30 x 45 cm, Sold: 40 bags, 100 pcs/bag, 8.5 kg/bag
                StockEntry(
                    itemName = "HDPE T-Shirt Bag",
                    size = "30 x 45 cm",
                    weightKg = 40 * 8.5,
                    pieces = 40 * 100,
                    counter = 40,
                    piecesPerBag = 100,
                    kgPerBag = 8.5,
                    type = StockEntry.TYPE_SOLD,
                    timestamp = System.currentTimeMillis() - 36000000
                )
            )
            
            samples.forEach { repository.insert(it) }
        }
    }
}
