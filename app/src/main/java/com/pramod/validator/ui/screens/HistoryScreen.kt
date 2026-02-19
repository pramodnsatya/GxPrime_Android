package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.Placeable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Report
import com.pramod.validator.viewmodel.HistoryViewModel
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onReportClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    onSignOut: () -> Unit,
    historyViewModel: HistoryViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    
    LaunchedEffect(Unit) {
        if (historyViewModel.shouldLoadReports()) {
        historyViewModel.loadReports()
        }
    }
    
    val reports by historyViewModel.filteredReports.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val searchQuery by historyViewModel.searchQuery.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
            TopAppBar(
                title = { 
                        Text(
                            text = "Assessment History",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                                painter = painterResource(id = com.pramod.validator.R.drawable.ic_filter),
                            contentDescription = "Filter",
                                tint = Color(0xFF0F172A) // slate-900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A),
                        actionIconContentColor = Color(0xFF0F172A)
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "history",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> { /* Already on history */ }
                        "fda483" -> onNavigateToFda483()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
                    }
                },
                user = currentUser,
                permissions = currentUserPermissions
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
            ) {
                // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { historyViewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                placeholder = { 
                    Text(
                        "Search assessments...",
                        color = Color(0xFF94A3B8) // slate-400
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8) // slate-400
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { historyViewModel.setSearchQuery("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE2E8F0), // slate-200
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )
            
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                    CircularProgressIndicator()
                    }
                } else if (reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                                    Text(
                        text = "No assessments found",
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 16.sp
                    )
                }
            } else {
                // Group reports by time period - cached with remember
                val groupedReports = remember(reports) { groupReportsByTimePeriod(reports) }
                val isLoadingMore by historyViewModel.isLoadingMore.collectAsState()
                
                // Create LazyListState to detect scroll position
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                
                // Detect when user scrolls near the end
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null) {
                                val totalItems = listState.layoutInfo.totalItemsCount
                                // Load more when user is within 5 items of the end
                                if (lastVisibleIndex >= totalItems - 5 && !isLoadingMore) {
                                    android.util.Log.d("HistoryScreen", "Near end of list, loading more reports...")
                                    historyViewModel.loadMoreReports()
                                }
                            }
                        }
                }
                
                    LazyColumn(
                    state = listState,
                        modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedReports.forEach { (period, periodReports) ->
                        item {
                            Text(
                                text = period,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A), // slate-900
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(periodReports) { report ->
                            ModernHistoryCard(
                                    report = report,
                                timePeriod = period,
                                    onClick = { onReportClick(report.id) }
                                )
                            }
                        }
                    
                    // Loading indicator at the bottom
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                    }
                    }
                }
            }
        }
        }
    }

    if (showFilterDialog) {
        SimpleFilterDialog(
            onDismiss = { showFilterDialog = false },
            historyViewModel = historyViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleFilterDialog(
    onDismiss: () -> Unit,
    historyViewModel: HistoryViewModel
) {
    val currentDomainFilters by historyViewModel.selectedDomainFilters.collectAsState()
    val currentStatusFilters by historyViewModel.selectedStatusFilters.collectAsState()
    val currentDateRange by historyViewModel.selectedDateRange.collectAsState()
    val currentSortBy by historyViewModel.selectedSortBy.collectAsState()
    
    var selectedDomains by remember(currentDomainFilters) { mutableStateOf(currentDomainFilters.toMutableSet()) }
    var selectedStatuses by remember(currentStatusFilters) { mutableStateOf(currentStatusFilters.toMutableSet()) }
    var selectedDateRange by remember(currentDateRange) { mutableStateOf(currentDateRange) }
    var selectedSortBy by remember(currentSortBy) { mutableStateOf(currentSortBy) }
    
    ModalBottomSheet(
        onDismissRequest = {
            // Apply filters before dismissing
            historyViewModel.setDomainFilters(selectedDomains)
            historyViewModel.setStatusFilters(selectedStatuses)
            historyViewModel.setDateRange(selectedDateRange)
            historyViewModel.setSortBy(selectedSortBy)
            onDismiss()
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                
                IconButton(
                    onClick = {
                        historyViewModel.setDomainFilters(selectedDomains)
                        historyViewModel.setStatusFilters(selectedStatuses)
                        historyViewModel.setDateRange(selectedDateRange)
                        historyViewModel.setSortBy(selectedSortBy)
                        onDismiss()
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF0F172A)
                    )
                }
            }
            
            // Domain Section
                        Text(
                text = "Domain",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val domains = listOf(
                "Quality Unit",
                "Packaging & Labeling",
                "Production",
                "Materials",
                "Laboratory Systems",
                "Facilities & Equipment",
                "Custom"
            )
            
            // Domain chips in rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                domains.chunked(3).forEach { rowDomains ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDomains.forEach { domain ->
                            val isSelected = selectedDomains.contains(domain)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDomains = if (isSelected) {
                                            selectedDomains - domain
                                        } else {
                                            selectedDomains + domain
                                        }.toMutableSet()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = domain,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else Color(0xFF64748B),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        // Fill remaining space if row has less than 3 items
                        repeat(3 - rowDomains.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Section
            Text(
                text = "Status",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                    modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                listOf("Completed", "In Progress").forEach { status ->
                    val isSelected = selectedStatuses.contains(status)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedStatuses = if (isSelected) {
                                    selectedStatuses - status
                                } else {
                                    selectedStatuses + status
                                }.toMutableSet()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = status,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date Range Section
                            Text(
                text = "Date Range",
                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val dateRanges = listOf("All Time", "Today", "This Week", "This Month")
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dateRanges.chunked(2).forEach { rowRanges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowRanges.forEach { range ->
                            val isSelected = selectedDateRange == range
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDateRange = range },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF1F5F9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected && range == "All Time") {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                        Text(
                                        text = range,
                                            fontSize = 13.sp,
                                        color = if (isSelected) Color.White else Color(0xFF64748B),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sort By Section
            Text(
                text = "Sort By",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val sortOptions = listOf(
                "Date (Newest)",
                "Date (Oldest)",
                "Name (A-Z)",
                "Name (Z-A)",
                "Domain (A-Z)",
                "Domain (Z-A)",
                "Status"
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortOptions.forEach { option ->
                    val isSelected = selectedSortBy == option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSortBy = option },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Text(
                                text = option,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF0F172A)
                            )
                            
                            if (isSelected) {
                Icon(
                                    Icons.Default.Check,
                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernHistoryCard(
    report: Report,
    timePeriod: String,
    onClick: () -> Unit
) {
    // Determine color based on time period
    val (gradientStart, gradientEnd) = when (timePeriod) {
        "Today" -> Pair(Color(0xFF3B82F6), Color(0xFF2563EB)) // blue
        "Yesterday" -> Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)) // violet
        "This Week" -> Pair(Color(0xFF10B981), Color(0xFF059669)) // emerald
        "This Month" -> Pair(Color(0xFFF59E0B), Color(0xFFD97706)) // amber
        else -> Pair(Color(0xFF06B6D4), Color(0xFF0891B2)) // cyan for previous months
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored gradient top bar - 6px height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(gradientStart, gradientEnd)
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Domain badge - using same colors as SubDomainScreen
    Surface(
        shape = RoundedCornerShape(12.dp),
                    color = getSubDomainColor(report.domainId)
        ) {
            Text(
                        text = getDomainDisplayName(report.domainId),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // Assessment name
                Text(
                    text = report.assessmentName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A) // slate-900
                )
                
                // Facility name
                if (report.facilityName.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
        Icon(
                            Icons.Default.LocationOn,
            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF64748B) // slate-500
                        )
        Text(
                            text = report.facilityName,
            fontSize = 14.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }
                
                // Date and time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF64748B) // slate-500
                    )
            Text(
                        text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(Date(report.completedAt)),
                        fontSize = 14.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
                
                // Divider line
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color(0xFFE2E8F0) // slate-200
                )
                
                // Status and View Report button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Completed status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color(0xFF3B82F6), // blue-500
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Text(
                            text = "Completed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3B82F6) // blue-500
                        )
                    }
                    
                    // View Report button
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6) // blue-500
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                            Icons.Default.Visibility,
                contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                Text(
                            text = "View Report",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

fun groupReportsByTimePeriod(reports: List<Report>): Map<String, List<Report>> {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    
    // Calculate time boundaries
    calendar.timeInMillis = now
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis
    
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStart = calendar.timeInMillis
    
    calendar.timeInMillis = now
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val thisWeekStart = calendar.timeInMillis
    
    calendar.timeInMillis = now
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val thisMonthStart = calendar.timeInMillis
    
    val grouped = mutableMapOf<String, MutableList<Report>>()
    
    reports.forEach { report ->
        val reportDate = Date(report.completedAt)
        android.util.Log.d("HistoryGrouping", "Report timestamp: ${report.completedAt}, Date: $reportDate")
        
        val period = when {
            report.completedAt >= todayStart -> "Today"
            report.completedAt >= yesterdayStart -> "Yesterday"
            report.completedAt >= thisWeekStart -> "This Week"
            report.completedAt >= thisMonthStart -> "This Month"
            else -> {
                // For older reports, format with their actual month and year
                val formattedPeriod = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(reportDate)
                android.util.Log.d("HistoryGrouping", "Older report - formatted as: $formattedPeriod")
                formattedPeriod
            }
        }
        
        android.util.Log.d("HistoryGrouping", "Report grouped into: $period")
        grouped.getOrPut(period) { mutableListOf() }.add(report)
    }
    
    // Filter out empty sections and sort by time period priority
    val periodOrder = listOf("Today", "Yesterday", "This Week", "This Month")
    return grouped
        .filter { it.value.isNotEmpty() } // Only include sections with reports
        .toSortedMap(compareBy { period ->
            val index = periodOrder.indexOf(period)
            if (index >= 0) index else Int.MAX_VALUE
        })
}

fun getDomainColor(domainId: String): Color {
    return when {
        domainId.contains("qu_", ignoreCase = true) || 
        domainId.contains("quality", ignoreCase = true) -> Color(0xFF3B82F6) // blue
        domainId.contains("pr_", ignoreCase = true) || 
        domainId.contains("production", ignoreCase = true) ||
        domainId.contains("manufacturing", ignoreCase = true) -> Color(0xFF8B5CF6) // violet
        domainId.contains("mt_", ignoreCase = true) || 
        domainId.contains("material", ignoreCase = true) -> Color(0xFF8B5CF6) // violet (Materials)
        domainId.contains("lab_", ignoreCase = true) || 
        domainId.contains("laboratory", ignoreCase = true) -> Color(0xFF8B5CF6) // violet (Laboratory)
        domainId.contains("pl_", ignoreCase = true) || 
        domainId.contains("packaging", ignoreCase = true) ||
        domainId.contains("labeling", ignoreCase = true) -> Color(0xFF8B5CF6) // violet (Packaging)
        domainId.contains("fe_", ignoreCase = true) || 
        domainId.contains("facilities", ignoreCase = true) ||
        domainId.contains("equipment", ignoreCase = true) -> Color(0xFF06B6D4) // cyan
        else -> Color(0xFF64748B) // slate
    }
}

// Get subdomain colors matching SubDomainScreen.kt
fun getSubDomainColor(domainId: String): Color {
    return when {
        domainId.contains("qu_", ignoreCase = true) || 
        domainId.contains("quality", ignoreCase = true) -> Color(0xFF6366F1) // Quality Unit - indigo
        domainId.contains("pr_", ignoreCase = true) || 
        domainId.contains("production", ignoreCase = true) || 
        domainId.contains("manufacturing", ignoreCase = true) -> Color(0xFF8B5CF6) // Production - violet
        domainId.contains("mt_", ignoreCase = true) || 
        domainId.contains("material", ignoreCase = true) -> Color(0xFFEC4899) // Materials - pink
        domainId.contains("lab_", ignoreCase = true) || 
        domainId.contains("laboratory", ignoreCase = true) -> Color(0xFF10B981) // Laboratory - emerald
        domainId.contains("fe_", ignoreCase = true) || 
        domainId.contains("facilities", ignoreCase = true) || 
        domainId.contains("equipment", ignoreCase = true) -> Color(0xFFF59E0B) // Facilities - amber
        domainId.contains("pl_", ignoreCase = true) || 
        domainId.contains("packaging", ignoreCase = true) || 
        domainId.contains("labeling", ignoreCase = true) -> Color(0xFFF43F5E) // Packaging/Labeling - rose
        else -> Color(0xFF64748B) // Default - slate
    }
}

fun getDomainDisplayName(domainId: String): String {
    return when {
        domainId.contains("qu_", ignoreCase = true) || 
        domainId.contains("quality", ignoreCase = true) -> "Quality Unit"
        domainId.contains("pr_", ignoreCase = true) || 
        domainId.contains("production", ignoreCase = true) ||
        domainId.contains("manufacturing", ignoreCase = true) -> "Production"
        domainId.contains("mt_", ignoreCase = true) || 
        domainId.contains("material", ignoreCase = true) -> "Materials"
        domainId.contains("lab_", ignoreCase = true) || 
        domainId.contains("laboratory", ignoreCase = true) -> "Laboratory Systems"
        domainId.contains("pl_", ignoreCase = true) || 
        domainId.contains("packaging", ignoreCase = true) ||
        domainId.contains("labeling", ignoreCase = true) -> "Packaging & Labeling"
        domainId.contains("fe_", ignoreCase = true) || 
        domainId.contains("facilities", ignoreCase = true) ||
        domainId.contains("equipment", ignoreCase = true) -> "Facilities & Equipment"
        else -> domainId
    }
}

// Helper functions for compatibility with other screens
fun getDomainIcon(domainId: String): String {
    return when {
        domainId.contains("qu_", ignoreCase = true) || 
        domainId.contains("quality", ignoreCase = true) -> "🎯"
        domainId.contains("pr_", ignoreCase = true) || 
        domainId.contains("production", ignoreCase = true) ||
        domainId.contains("manufacturing", ignoreCase = true) -> "🏭"
        domainId.contains("mt_", ignoreCase = true) || 
        domainId.contains("material", ignoreCase = true) -> "📋"
        domainId.contains("lab_", ignoreCase = true) || 
        domainId.contains("laboratory", ignoreCase = true) -> "🔬"
        domainId.contains("pl_", ignoreCase = true) || 
        domainId.contains("packaging", ignoreCase = true) ||
        domainId.contains("labeling", ignoreCase = true) -> "📦"
        domainId.contains("fe_", ignoreCase = true) || 
        domainId.contains("facilities", ignoreCase = true) ||
        domainId.contains("equipment", ignoreCase = true) -> "🏢"
        else -> "📄"
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
