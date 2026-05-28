package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.StockEntry
import com.example.viewmodel.ItemStockSummary
import com.example.viewmodel.OverallSummary
import com.example.viewmodel.StockViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockApp(
    viewModel: StockViewModel = viewModel()
) {
    // Collecting states reactively
    val allEntries by viewModel.allEntries.collectAsState()
    val filteredEntries by viewModel.filteredEntries.collectAsState()
    val stockSummaries by viewModel.stockSummaries.collectAsState()
    val overallSummary by viewModel.overallSummary.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterItem by viewModel.filterItem.collectAsState()

    // Dialog state for logging activity
    var showLogDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    // Screen dimensions to support adaptive viewports
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    // Tab state (re-routed dynamically or shown side-by-side)
    var selectedTabsIndex by remember { mutableIntStateOf(0) } // 0 = Stock Summary, 1 = History Log

    // Format utility for timestamps
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_anwar_logo_1780000838875),
                            contentDescription = "Anwar Recycling Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            "Anwar Recycling",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Plastics recycling & fabrication tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.seedSampleData() },
                        modifier = Modifier.testTag("seed_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Seed Sample Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Data",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Log Entry") },
                text = { Text("Log Activity") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_log_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Overall Summary Metric Cards
                OverallMetricsSection(overallSummary = overallSummary)

                Spacer(modifier = Modifier.height(16.dp))

                // If database is completely empty, provide an attractive onboarding view
                if (allEntries.isEmpty()) {
                    EmptyOnboardingView(
                        onSeedData = { viewModel.seedSampleData() },
                        onAddManual = { showLogDialog = true }
                    )
                } else {
                    // Search & Filters bar
                    SearchBarAndFilters(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        filterType = filterType,
                        onTypeFilterChange = { viewModel.setFilterType(it) },
                        filterItem = filterItem,
                        onItemFilterChange = { viewModel.setFilterItem(it) },
                        itemsList = viewModel.predefinedItems
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isWideScreen) {
                        // Wide Screen Canonical Layout: Show stocks summary and transaction logs side by side!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    "Stock Remaining by Item / Size",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                StockSummaryList(
                                    summaries = stockSummaries,
                                    searchQuery = searchQuery
                                )
                            }
                            Column(modifier = Modifier.weight(0.8f)) {
                                Text(
                                    "Recent Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                TransactionHistoryList(
                                    entries = filteredEntries,
                                    dateFormatter = dateFormatter,
                                    onDeleteEntry = { viewModel.deleteEntry(it) }
                                )
                            }
                        }
                    } else {
                        // Phone Layout: Tabbed structure with segmented controllers
                        Column(modifier = Modifier.weight(1f)) {
                            TabRow(
                                selectedTabIndex = selectedTabsIndex,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("phone_tabs_row"),
                                indicator = @Composable { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabsIndex]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                divider = {}
                            ) {
                                Tab(
                                    selected = selectedTabsIndex == 0,
                                    onClick = { selectedTabsIndex = 0 },
                                    text = { Text("Stock Summaries") },
                                    modifier = Modifier.testTag("summary_tab")
                                )
                                Tab(
                                    selected = selectedTabsIndex == 1,
                                    onClick = { selectedTabsIndex = 1 },
                                    text = { Text("All Logs (${filteredEntries.size})") },
                                    modifier = Modifier.testTag("history_tab")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            AnimatedContent(
                                targetState = selectedTabsIndex,
                                label = "TabTransition"
                            ) { targetIndex ->
                                when (targetIndex) {
                                    0 -> StockSummaryList(
                                        summaries = stockSummaries,
                                        searchQuery = searchQuery
                                    )
                                    1 -> TransactionHistoryList(
                                        entries = filteredEntries,
                                        dateFormatter = dateFormatter,
                                        onDeleteEntry = { viewModel.deleteEntry(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to write new entry
    if (showLogDialog) {
        LogEntryDialog(
            predefinedItems = viewModel.predefinedItems,
            predefinedSizes = viewModel.predefinedSizes,
            onDismiss = { showLogDialog = false },
            onSave = { name, size, weight, pieces, counter, type ->
                viewModel.addEntry(name, size, weight, pieces, counter, type)
                showLogDialog = false
            }
        )
    }

    // Modal dialogue to confirm database destruction/wipe
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Reset All Data") },
            text = { Text("Are you absolutely sure you want to completely erase all fabricated and sold logs? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun OverallMetricsSection(overallSummary: OverallSummary) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Card for Weight
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("overall_metrics_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "TOTAL WEIGHT IN STOCK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${String.format("%,.1f", overallSummary.netStockKg)} kg",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (overallSummary.netStockKg >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("metric_net_stock")
                        )
                        Text(
                            "Net Weight",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        MetricMiniBadge(
                            label = "Fabricated",
                            value = "${String.format("%,.0f", overallSummary.totalFabricatedKg)} kg",
                            color = Color(0xFF2E7D32),
                            icon = Icons.Default.AddCircle,
                            tag = "metric_fabricated"
                        )
                        MetricMiniBadge(
                            label = "Sold",
                            value = "${String.format("%,.0f", overallSummary.totalSoldKg)} kg",
                            color = Color(0xFFC62828),
                            icon = Icons.Default.ShoppingCart,
                            tag = "metric_sold"
                        )
                    }
                }
            }
        }

        // Card for Piece Count
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("overall_pieces_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "TOTAL BAG PIECES IN STOCK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${String.format("%,d", overallSummary.netStockPieces)} pcs",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (overallSummary.netStockPieces >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("metric_net_pieces")
                        )
                        Text(
                            "Net Pieces",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        MetricMiniBadge(
                            label = "Fabricated",
                            value = "${String.format("%,d", overallSummary.totalFabricatedPieces)} pcs",
                            color = Color(0xFF2E7D32),
                            icon = Icons.Default.CheckCircle,
                            tag = "metric_fabricated_pieces"
                        )
                        MetricMiniBadge(
                            label = "Sold",
                            value = "${String.format("%,d", overallSummary.totalSoldPieces)} pcs",
                            color = Color(0xFFC62828),
                            icon = Icons.Default.Send,
                            tag = "metric_sold_pieces"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricMiniBadge(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.testTag(tag)
                )
            }
        }
    }
}

@Composable
fun SearchBarAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterType: String?,
    onTypeFilterChange: (String?) -> Unit,
    filterItem: String?,
    onItemFilterChange: (String?) -> Unit,
    itemsList: List<String>
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            placeholder = { Text("Search item, size or type...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Type filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Filter Type:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilterChip(
                selected = filterType == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("chip_all")
            )

            FilterChip(
                selected = filterType == StockEntry.TYPE_FABRICATED,
                onClick = { onTypeFilterChange(StockEntry.TYPE_FABRICATED) },
                label = { Text("Fabricated") },
                modifier = Modifier.testTag("chip_fabricated")
            )

            FilterChip(
                selected = filterType == StockEntry.TYPE_SOLD,
                onClick = { onTypeFilterChange(StockEntry.TYPE_SOLD) },
                label = { Text("Sold") },
                modifier = Modifier.testTag("chip_sold")
            )
        }
    }
}

@Composable
fun StockSummaryList(
    summaries: List<ItemStockSummary>,
    searchQuery: String
) {
    if (summaries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No summaries match current filters.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("stock_summary_list")
        ) {
            items(summaries, key = { "${it.itemName}_${it.size}" }) { summary ->
                Card(
                     modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_card_${summary.itemName}_${summary.size}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    summary.itemName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            summary.size,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (summary.latestCounter > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    "Counter: ${String.format("%,d", summary.latestCounter)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Net Status column
                            val isLowStock = summary.netStockKg < 100.0 && summary.netStockKg >= 0
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${String.format("%,.1f", summary.netStockKg)} kg",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.netStockKg >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "${String.format("%,d", summary.netStockPieces)} pcs",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isLowStock) {
                                    Text(
                                        "LOW STOCK",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fabricated vs Sold details row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Fabricated breakdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Fabricated (Made)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "${String.format("%,.1f", summary.totalFabricatedKg)} kg",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        "(${String.format("%,d", summary.totalFabricatedPieces)} pcs)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Sold breakdown
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "Sold (Dispatched)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "${String.format("%,.1f", summary.totalSoldKg)} kg",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                    Text(
                                        "(${String.format("%,d", summary.totalSoldPieces)} pcs)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC62828).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryList(
    entries: List<StockEntry>,
    dateFormatter: SimpleDateFormat,
    onDeleteEntry: (StockEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No previous transactions found for selection.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("transaction_history_list")
        ) {
            items(entries, key = { it.id }) { entry ->
                var showRowDeleteDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { /* Could expand or show detail */ },
                            onLongClick = { showRowDeleteDialog = true }
                        )
                        .testTag("history_card_${entry.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored indicator based on fabrication vs sales
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (entry.type == StockEntry.TYPE_FABRICATED)
                                        Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    else
                                        Color(0xFFC62828).copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (entry.type == StockEntry.TYPE_FABRICATED) Icons.Default.Add else Icons.Default.ShoppingCart,
                                contentDescription = entry.type,
                                tint = if (entry.type == StockEntry.TYPE_FABRICATED) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.itemName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        if (entry.type == StockEntry.TYPE_FABRICATED)
                                            "+${String.format("%,.1f", entry.weightKg)} kg"
                                        else
                                            "-${String.format("%,.1f", entry.weightKg)} kg",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (entry.type == StockEntry.TYPE_FABRICATED) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    if (entry.pieces > 0) {
                                        Text(
                                            "${String.format("%,d", entry.pieces)} pcs",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (entry.type == StockEntry.TYPE_FABRICATED) Color(0xFF2E7D32).copy(alpha = 0.8f) else Color(0xFFC62828).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Size: ${entry.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (entry.type == StockEntry.TYPE_FABRICATED && entry.counter > 0) {
                                        Text(
                                            "• Counter: ${String.format("%,d", entry.counter)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    dateFormatter.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Delete button
                        IconButton(
                            onClick = { showRowDeleteDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_entry_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete log",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (showRowDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showRowDeleteDialog = false },
                        title = { Text("Delete This Log?") },
                        text = { Text("Are you sure you want to delete this ${entry.type.lowercase(Locale.getDefault())} record of ${entry.itemName} (${entry.size}) of ${entry.weightKg} kg?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDeleteEntry(entry)
                                    showRowDeleteDialog = false
                                },
                                modifier = Modifier.testTag("confirm_delete_${entry.id}_button")
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRowDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyOnboardingView(
    onSeedData: () -> Unit,
    onAddManual: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("empty_onboarding_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 350.dp)
                .padding(24.dp)
        ) {
            // Visual element representing industrial inventory
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Welcome to Stock Tracker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "No transactions logged yet. To view stock balance (Fabricated vs Sold), you can populate standard test logs or add your first manual entry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSeedData,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seed_onboarding_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seed Sample Inventory")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddManual,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_onboarding_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Manual Activity")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogEntryDialog(
    predefinedItems: List<String>,
    predefinedSizes: List<String>,
    onDismiss: () -> Unit,
    onSave: (itemName: String, size: String, weightKg: Double, pieces: Int, counter: Int, type: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(StockEntry.TYPE_FABRICATED) }
    var itemNameInput by remember { mutableStateOf("") }
    var sizeInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var piecesInput by remember { mutableStateOf("") }
    var counterInput by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("log_entry_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Log Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Selection FABRICATED vs SOLD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                        .testTag("toggle_type_row")
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedType == StockEntry.TYPE_FABRICATED) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable { selectedType = StockEntry.TYPE_FABRICATED }
                            .padding(vertical = 10.dp)
                            .testTag("toggle_type_fabricated"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Fabricated (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == StockEntry.TYPE_FABRICATED) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedType == StockEntry.TYPE_SOLD) Color(0xFFC62828) else Color.Transparent)
                            .clickable { selectedType = StockEntry.TYPE_SOLD }
                            .padding(vertical = 10.dp)
                            .testTag("toggle_type_sold"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sold (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == StockEntry.TYPE_SOLD) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item Select
                Text(
                    "Item Name *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    predefinedItems.forEach { predefinedItem ->
                        SuggestionChip(
                            onClick = { itemNameInput = predefinedItem },
                            label = { Text(predefinedItem) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (itemNameInput == predefinedItem) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = itemNameInput,
                    onValueChange = { itemNameInput = it },
                    placeholder = { Text("Enter or select item name...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_item_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Size Select
                Text(
                    "Size *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    predefinedSizes.forEach { predefinedSize ->
                        SuggestionChip(
                            onClick = { sizeInput = predefinedSize },
                            label = { Text(predefinedSize) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (sizeInput == predefinedSize) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = sizeInput,
                    onValueChange = { sizeInput = it },
                    placeholder = { Text("Enter or select size...") },
                     modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_size_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pieces Input
                Text(
                    "Pieces * (Bags Count)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = piecesInput,
                    onValueChange = { piecesInput = it },
                    placeholder = { Text("Enter number of pieces (e.g. 5000)...") },
                    leadingIcon = { Badge { Text("pcs") } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_pieces_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Weight (kg) Input
                Text(
                    "Weight (kg) *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    placeholder = { Text("Enter weight in kg...") },
                    leadingIcon = { Badge { Text("kg") } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_weight_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Counter Input (only really relevant for fabrication machines, but let's label it correctly)
                Text(
                    if (selectedType == StockEntry.TYPE_FABRICATED) "Machine Counter Reading" else "Machine Counter Reading (Optional)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = counterInput,
                    onValueChange = { counterInput = it },
                    placeholder = { Text("Enter machine counter value...") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_counter_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (hasError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please fill in all starred (*) fields correctly (weight > 0, pieces > 0).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val weight = weightInput.toDoubleOrNull()
                            val pieces = piecesInput.toIntOrNull()
                            val counter = counterInput.toIntOrNull() ?: 0
                            
                            if (itemNameInput.isNotBlank() && sizeInput.isNotBlank() && 
                                weight != null && weight > 0 && 
                                pieces != null && pieces > 0) {
                                onSave(itemNameInput, sizeInput, weight, pieces, counter, selectedType)
                            } else {
                                hasError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == StockEntry.TYPE_FABRICATED) Color(0xFF2E7D32) else Color(0xFFC62828)
                        ),
                        modifier = Modifier.testTag("dialog_submit_button")
                    ) {
                        Text("Log Activity")
                    }
                }
            }
        }
    }
}


