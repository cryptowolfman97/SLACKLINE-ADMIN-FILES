package com.example.slacklineadminapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.ProjectViewModel
import com.example.slacklineadminapp.data.SyncedCalendarEntry
import com.example.slacklineadminapp.data.TaskStatus
import com.example.slacklineadminapp.ui.theme.LocalAppColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Lightweight filter-only ViewModel (no data, just UI filter state)
// ─────────────────────────────────────────────────────────────────────────────

class BusinessCalendarViewModel : ViewModel() {
    var currentMonth by mutableStateOf(YearMonth.now())
        private set

    var selectedProjectFilter by mutableStateOf<String?>(null)
    var selectedStatusFilter  by mutableStateOf<String?>(null)  // TaskStatus.identifier or null

    fun nextMonth() { currentMonth = currentMonth.plusMonths(1) }
    fun prevMonth() { currentMonth = currentMonth.minusMonths(1) }

    fun getFilteredEntriesForDate(
        date: LocalDate,
        allEntries: List<SyncedCalendarEntry>
    ): List<SyncedCalendarEntry> {
        return allEntries.filter { entry ->
            val matchesDate = date == entry.startDate || date == entry.endDate ||
                    (date.isAfter(entry.startDate) && date.isBefore(entry.endDate))
            val matchesProject = selectedProjectFilter == null || entry.projectId == selectedProjectFilter
            val matchesStatus  = selectedStatusFilter == null || entry.statusIdentifier == selectedStatusFilter
            matchesDate && matchesProject && matchesStatus
        }
    }
}

// ─── Status color mapping (mirrors ProjectManagementScreen logic) ─────────────
private fun calendarStatusColor(identifier: String) = when (identifier) {
    TaskStatus.IN_PROGRESS.identifier -> Color(0xFF2ECC71)
    TaskStatus.DONE.identifier        -> Color(0xFF3498DB)
    else                              -> Color(0xFFE67E22)
}

private fun calendarStatusLabel(identifier: String) = when (identifier) {
    TaskStatus.IN_PROGRESS.identifier -> "In Progress"
    TaskStatus.DONE.identifier        -> "Done"
    else                              -> "To Do"
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCalendarScreen(
    onNavigateBack: () -> Unit,
    onEditTaskRequested: (taskId: String) -> Unit
) {
    val calVm: BusinessCalendarViewModel = viewModel()
    val projectVm: ProjectViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val appColors = LocalAppColors.current

    // ─── FRESH LAUNCH FIX ────────────────────────────────────────────────────
    // Guarantee data is loaded if we hit this screen before the Project screen
    LaunchedEffect(Unit) {
        projectVm.loadData()
    }

    // Live-synced entries from Project Management
    val allEntries by projectVm.calendarEntries.collectAsState()

    // Derive unique projects from entries for filter chips
    val uniqueProjects = remember(allEntries) {
        allEntries.map { it.projectId to it.projectName }.distinct()
    }

    var selectedDateForPopup by remember { mutableStateOf<LocalDate?>(null) }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BUSINESS CALENDAR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.bg)
            )
        },
        containerColor = appColors.bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            MonthSelectorHeader(
                currentMonth = calVm.currentMonth,
                onPrevMonth  = { calVm.prevMonth() },
                onNextMonth  = { calVm.nextMonth() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CalendarGridFrame(
                currentMonth    = calVm.currentMonth,
                onDateSelected  = { date -> selectedDateForPopup = date },
                getEntriesForDate = { date -> calVm.getFilteredEntriesForDate(date, allEntries) }
            )

            Spacer(modifier = Modifier.weight(1f))

            FilterManagementDeck(
                uniqueProjects         = uniqueProjects,
                selectedProjectFilter  = calVm.selectedProjectFilter,
                onProjectFilterSelected = { calVm.selectedProjectFilter = it },
                selectedStatusFilter   = calVm.selectedStatusFilter,
                onStatusFilterSelected  = { calVm.selectedStatusFilter = it }
            )
        }

        selectedDateForPopup?.let { date ->
            val entriesForDay = calVm.getFilteredEntriesForDate(date, allEntries)
            DetailedSchedulePopup(
                date      = date,
                entries   = entriesForDay,
                onDismiss = { selectedDateForPopup = null },
                onEditTask = { taskId ->
                    selectedDateForPopup = null
                    onEditTaskRequested(taskId)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Month header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthSelectorHeader(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.card, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFF1ABC9C))
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.US).uppercase()} ${currentMonth.year}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF1ABC9C))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calendar grid (Updated with Pulsing Dots)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CalendarGridFrame(
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    getEntriesForDate: (LocalDate) -> List<SyncedCalendarEntry>
) {
    val daysInGrid      = remember(currentMonth) { calculateGridDays(currentMonth) }
    val weekdayLabels   = remember { listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val rowsCount = daysInGrid.size / 7
        for (row in 0 until rowsCount) {
            Row(
                modifier = Modifier.fillMaxWidth().height(82.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val date = daysInGrid[row * 7 + col]
                    val isCurrentMonth = date.month == currentMonth.month && date.year == currentMonth.year
                    val dayEntries = getEntriesForDate(date)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.4.dp, Color.White.copy(alpha = 0.08f))
                            .background(
                                if (date == LocalDate.now()) Color(0xFF1ABC9C).copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { onDateSelected(date) }
                            .padding(2.dp)
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 11.sp,
                            fontWeight = if (date == LocalDate.now()) FontWeight.Black else FontWeight.SemiBold,
                            color = when {
                                date == LocalDate.now() -> Color(0xFF1ABC9C)
                                isCurrentMonth          -> Color.White
                                else                    -> Color.White.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 2.dp)
                        )

                        // ─── PULSING DOTS UI ─────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val maxDots = 4
                            val displayEntries = dayEntries.take(maxDots)
                            
                            displayEntries.forEachIndexed { index, entry ->
                                if (index > 0) Spacer(modifier = Modifier.width(3.dp))
                                PulsingDot(color = calendarStatusColor(entry.statusIdentifier))
                            }
                            
                            if (dayEntries.size > maxDots) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "+",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1ABC9C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter deck — project filters derived live from actual data
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterManagementDeck(
    uniqueProjects: List<Pair<String, String>>,
    selectedProjectFilter: String?,
    onProjectFilterSelected: (String?) -> Unit,
    selectedStatusFilter: String?,
    onStatusFilterSelected: (String?) -> Unit
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.card, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FilterList, null, tint = Color(0xFF1ABC9C), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("WORKSPACE & STATUS FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.5f))
        }

        // Project filter row (scrollable if many projects)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipItem(
                label = "ALL WORK",
                isSelected = selectedProjectFilter == null,
                activeColor = Color(0xFF1ABC9C),
                onClick = { onProjectFilterSelected(null) },
                modifier = Modifier.weight(1f)
            )
            uniqueProjects.take(2).forEach { (id, name) ->
                FilterChipItem(
                    label = name,
                    isSelected = selectedProjectFilter == id,
                    activeColor = Color(0xFF1ABC9C),
                    onClick = { onProjectFilterSelected(id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Status filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipItem(
                label = "ALL",
                isSelected = selectedStatusFilter == null,
                activeColor = Color(0xFF1ABC9C),
                onClick = { onStatusFilterSelected(null) },
                modifier = Modifier.weight(1f)
            )
            listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE).forEach { status ->
                FilterChipItem(
                    label = status.label,
                    isSelected = selectedStatusFilter == status.identifier,
                    activeColor = calendarStatusColor(status.identifier),
                    onClick = { onStatusFilterSelected(status.identifier) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (isSelected) activeColor else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) activeColor else Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Day detail popup
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailedSchedulePopup(
    date: LocalDate,
    entries: List<SyncedCalendarEntry>,
    onDismiss: () -> Unit,
    onEditTask: (String) -> Unit
) {
    val appColors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .background(appColors.card, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE")),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1ABC9C)
                        )
                        Text(
                            text = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Close Panel", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No active schedules pinned to this day.",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries) { entry ->
                            val sColor = calendarStatusColor(entry.statusIdentifier)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp).height(40.dp)
                                        .background(sColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    // Project name + optional subproject
                                    val context = if (entry.subProjectName.isNotBlank())
                                        "${entry.projectName} › ${entry.subProjectName}"
                                    else entry.projectName
                                    Text(
                                        text = context.uppercase(),
                                        fontSize = 8.sp, fontWeight = FontWeight.Black, color = sColor
                                    )
                                    Text(
                                        text = entry.title,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = Color.White, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = calendarStatusLabel(entry.statusIdentifier),
                                        fontSize = 10.sp, color = sColor.copy(alpha = 0.8f)
                                    )
                                    if (entry.notes.isNotBlank()) {
                                        Text(
                                            text = entry.notes.replace("\n", " "),
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onEditTask(entry.taskId) },
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                ) {
                                    Icon(
                                        Icons.Default.Edit, "Edit Task Source",
                                        tint = Color(0xFF1ABC9C), modifier = Modifier.size(16.dp)
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

// ─────────────────────────────────────────────────────────────────────────────
// Pulsing Dot Composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_anim"
    )

    Box(
        modifier = modifier
            .size(7.dp)
            .background(color = color.copy(alpha = alpha), shape = CircleShape)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid math
// ─────────────────────────────────────────────────────────────────────────────

private fun calculateGridDays(currentMonth: YearMonth): List<LocalDate> {
    val firstDay = currentMonth.atDay(1)
    val missingPrefix = firstDay.dayOfWeek.value - 1
    var running = firstDay.minusDays(missingPrefix.toLong())
    val grid = mutableListOf<LocalDate>()
    repeat(42) { grid.add(running); running = running.plusDays(1) }
    return grid
}
