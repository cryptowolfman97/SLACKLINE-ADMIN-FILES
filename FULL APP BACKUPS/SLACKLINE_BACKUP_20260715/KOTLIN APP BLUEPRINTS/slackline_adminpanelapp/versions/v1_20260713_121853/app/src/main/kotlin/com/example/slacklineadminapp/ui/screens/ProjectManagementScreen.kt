package com.example.slacklineadminapp.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.data.TaskStatus as DataTaskStatus
import com.example.slacklineadminapp.ui.theme.*
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ─── Dialog type constants ────────────────────────────────────────────────────
private const val DLG_NONE          = ""
private const val DLG_PROJECT       = "PROJECT"
private const val DLG_SUBPROJECT    = "SUBPROJECT"
private const val DLG_TASK          = "TASK"
private const val DLG_SETTINGS      = "SETTINGS"
private const val DLG_IMPORT_CHOICE = "IMPORT_CHOICE"

// ─── Target location for nested operations ────────────────────────────────────
data class TargetLocation(
    val projectId: String,
    val subProjectId: String = "",  // empty = project-level task
    val taskId: String = ""
)

// ─── SharedPrefs keys ─────────────────────────────────────────────────────────
private const val PREFS_NAME       = "pm_settings"
private const val PREF_EXPORT_DIR  = "export_dir_path"
private const val PREF_NOTIFS      = "notifications_enabled"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagementScreen(onNavigateBack: () -> Unit) {
    val vm: ProjectViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val ctx = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val projects by vm.projects.collectAsState()
    val toastMessage by vm.toastMessage.collectAsState()
    val appColors = LocalAppColors.current
    val prefs = remember { ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // ─── Expansion state ──────────────────────────────────────────────────────
    var expandedProjects    by remember { mutableStateOf(setOf<String>()) }
    var expandedSubProjects by remember { mutableStateOf(setOf<String>()) }

    // ─── Dialog routing ───────────────────────────────────────────────────────
    var activeDialog    by remember { mutableStateOf(DLG_NONE) }
    var targetLoc       by remember { mutableStateOf<TargetLocation?>(null) }
    var isEditingMode   by remember { mutableStateOf(false) }

    // ─── Task dialog form state ───────────────────────────────────────────────
    var textInputState    by remember { mutableStateOf("") }
    var taskNotesState    by remember { mutableStateOf("") }
    var taskTimelineState by remember { mutableStateOf("") }
    var taskStartDate     by remember { mutableStateOf("") }   // ISO yyyy-MM-dd
    var taskEndDate       by remember { mutableStateOf("") }   // ISO yyyy-MM-dd
    var taskStatusState   by remember { mutableStateOf(DataTaskStatus.TODO.identifier) }

    // ─── Date picker visibility ───────────────────────────────────────────────
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    // ─── Settings state ───────────────────────────────────────────────────────
    var exportDirPath  by remember { mutableStateOf(prefs.getString(PREF_EXPORT_DIR, null)) }
    var pendingImportFile by remember { mutableStateOf<File?>(null) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean(PREF_NOTIFS, true)) }

    // ─── Display formatter ────────────────────────────────────────────────────
    val displayFmt = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val isoFmt     = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    fun buildTimelineLabel(start: String, end: String): String {
        if (start.isBlank() || end.isBlank()) return ""
        return try {
            val s = LocalDate.parse(start, isoFmt).format(displayFmt)
            val e = LocalDate.parse(end, isoFmt).format(displayFmt)
            "$s → $e"
        } catch (ex: Exception) { "" }
    }

    // ─── File/folder pickers ──────────────────────────────────────────────────
    val exportDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = getRealPathFromUri(ctx, it)
            exportDirPath = path
            prefs.edit().putString(PREF_EXPORT_DIR, path).apply()
            if (path != null) vm.setExportDirectory(path)
            Toast.makeText(ctx, "Export directory set", Toast.LENGTH_SHORT).show()
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val file = getFileFromUri(ctx, it)
            if (file != null) {
                pendingImportFile = file
                activeDialog = DLG_IMPORT_CHOICE
            } else {
                Toast.makeText(ctx, "Could not read selected file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // Set path FIRST, then load data to ensure we read from correct directory
        exportDirPath?.let { vm.setExportDirectory(it) }
        vm.loadData()
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(ctx, toastMessage, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    BackHandler { onNavigateBack() }

    fun toggleProject(id: String) {
        expandedProjects = if (id in expandedProjects) expandedProjects - id else expandedProjects + id
    }

    fun toggleSubProject(id: String) {
        expandedSubProjects = if (id in expandedSubProjects) expandedSubProjects - id else expandedSubProjects + id
    }

    fun openTaskDialog(loc: TargetLocation, editing: Boolean = false, task: TaskItem? = null) {
        targetLoc       = loc
        isEditingMode   = editing
        textInputState  = task?.title ?: ""
        taskNotesState  = task?.notes ?: ""
        taskStartDate   = task?.startDate ?: ""
        taskEndDate     = task?.endDate ?: ""
        taskTimelineState = task?.timeline ?: ""
        taskStatusState = task?.statusIdentifier ?: DataTaskStatus.TODO.identifier
        activeDialog    = DLG_TASK
    }

    // ─── Date pickers (shown outside AlertDialog to avoid nesting) ────────────
    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (taskStartDate.isNotBlank())
                LocalDate.parse(taskStartDate, isoFmt)
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            else null
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        taskStartDate = date.format(isoFmt)
                        taskTimelineState = buildTimelineLabel(taskStartDate, taskEndDate)
                    }
                    showStartPicker = false
                }) { Text("OK", color = TealCol) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Cancel", color = SubText)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = appColors.card2,
                titleContentColor = TealCol,
                headlineContentColor = TextCol,
                weekdayContentColor = SubText,
                navigationContentColor = TealCol,
                yearContentColor = TextCol,
                currentYearContentColor = TealCol,
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = TealCol,
                dayContentColor = TextCol,
                todayContentColor = TealCol,
                todayDateBorderColor = TealCol,
                selectedDayContentColor = Color.Black,
                selectedDayContainerColor = TealCol,
                disabledDayContentColor = SubText.copy(alpha = 0.3f)
            )
        ) {
            DatePicker(
                state = pickerState,
                title = { Text("  Select Start Date", color = TealCol, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, start = 16.dp)) },
                headline = null,
                showModeToggle = false
            )
        }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (taskEndDate.isNotBlank())
                LocalDate.parse(taskEndDate, isoFmt)
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            else null
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        taskEndDate = date.format(isoFmt)
                        taskTimelineState = buildTimelineLabel(taskStartDate, taskEndDate)
                    }
                    showEndPicker = false
                }) { Text("OK", color = TealCol) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancel", color = SubText)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = appColors.card2,
                titleContentColor = TealCol,
                headlineContentColor = TextCol,
                weekdayContentColor = SubText,
                navigationContentColor = TealCol,
                yearContentColor = TextCol,
                currentYearContentColor = TealCol,
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = TealCol,
                dayContentColor = TextCol,
                todayContentColor = TealCol,
                todayDateBorderColor = TealCol,
                selectedDayContentColor = Color.Black,
                selectedDayContainerColor = TealCol,
                disabledDayContentColor = SubText.copy(alpha = 0.3f)
            )
        ) {
            DatePicker(
                state = pickerState,
                title = { Text("  Select End Date", color = TealCol, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, start = 16.dp)) },
                headline = null,
                showModeToggle = false
            )
        }
    }

    // ─── Main scaffold ────────────────────────────────────────────────────────
    Scaffold(
        containerColor = appColors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PROJECT MANAGEMENT",
                        color = TealCol,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TealCol)
                    }
                },
                actions = {
                    // Settings
                    IconButton(onClick = { activeDialog = DLG_SETTINGS }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SubText)
                    }
                    // Add Project
                    IconButton(onClick = {
                        textInputState = ""
                        isEditingMode  = false
                        activeDialog   = DLG_PROJECT
                    }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Project", tint = TealCol)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.card)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assignment, null, tint = SubText, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active workspace items", color = SubText, fontSize = 14.sp)
                        Text("Tap + to build your first project", color = SubText.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        val isProjExpanded = project.id in expandedProjects

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, TealCol.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.animateContentSize()) {

                                // ── Project header row ────────────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isProjExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = TealCol,
                                        modifier = Modifier.clickable { toggleProject(project.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = project.title,
                                        color = TextCol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f).clickable { toggleProject(project.id) }
                                    )
                                    // Add task to project directly
                                    IconButton(onClick = {
                                        openTaskDialog(TargetLocation(projectId = project.id))
                                    }) {
                                        Icon(Icons.Default.PlaylistAdd, "Add Task to Project", tint = TealCol)
                                    }
                                    // Add subproject
                                    IconButton(onClick = {
                                        targetLoc      = TargetLocation(projectId = project.id)
                                        textInputState = ""
                                        activeDialog   = DLG_SUBPROJECT
                                    }) {
                                        Icon(Icons.Default.Add, "Add SubProject", tint = BlueCol)
                                    }
                                    // Edit project name
                                    IconButton(onClick = {
                                        targetLoc      = TargetLocation(projectId = project.id)
                                        textInputState = project.title
                                        isEditingMode  = true
                                        activeDialog   = DLG_PROJECT
                                    }) {
                                        Icon(Icons.Default.Edit, "Edit Project", tint = SubText)
                                    }
                                    // Delete project
                                    IconButton(onClick = { vm.deleteProject(project.id) }) {
                                        Icon(Icons.Default.Delete, "Delete Project", tint = RedCol)
                                    }
                                }

                                AnimatedVisibility(visible = isProjExpanded) {
                                    Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 12.dp)) {

                                        // ── Project-level tasks ───────────────
                                        if (project.tasks.isNotEmpty()) {
                                            Text(
                                                "Project Tasks",
                                                color = TealCol.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            project.tasks.forEach { task ->
                                                TaskNodeView(
                                                    task = task,
                                                    statusColor = statusColor(task.statusIdentifier),
                                                    onToggleCheckbox = { newNotes ->
                                                        vm.updateProjectTask(
                                                            project.id, task.id,
                                                            task.copy(notes = newNotes)
                                                        )
                                                    },
                                                    onEdit = {
                                                        openTaskDialog(
                                                            TargetLocation(project.id, taskId = task.id),
                                                            editing = true,
                                                            task = task
                                                        )
                                                    },
                                                    onDelete = { 
                                                        vm.deleteProjectTask(project.id, task.id) 
                                                        NotificationHelper.cancelDeadline(ctx, task.id)
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        // ── SubProjects ───────────────────────
                                        project.subProjects.forEach { subProject ->
                                            val isSubExpanded = subProject.id in expandedSubProjects

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .background(appColors.card2, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isSubExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = BlueCol,
                                                    modifier = Modifier.clickable { toggleSubProject(subProject.id) }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = subProject.title,
                                                    color = TextCol,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f).clickable { toggleSubProject(subProject.id) }
                                                )
                                                IconButton(onClick = {
                                                    openTaskDialog(TargetLocation(project.id, subProject.id))
                                                }) {
                                                    Icon(Icons.Default.PlaylistAdd, "Add Task", tint = BlueCol)
                                                }
                                                IconButton(onClick = {
                                                    targetLoc      = TargetLocation(project.id, subProject.id)
                                                    textInputState = subProject.title
                                                    isEditingMode  = true
                                                    activeDialog   = DLG_SUBPROJECT
                                                }) {
                                                    Icon(Icons.Default.Edit, "Edit Sub", tint = SubText)
                                                }
                                                IconButton(onClick = { vm.deleteSubProject(project.id, subProject.id) }) {
                                                    Icon(Icons.Default.Delete, "Delete Sub", tint = RedCol)
                                                }
                                            }

                                            AnimatedVisibility(visible = isSubExpanded) {
                                                Column(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                                                    subProject.tasks.forEach { task ->
                                                        TaskNodeView(
                                                            task = task,
                                                            statusColor = statusColor(task.statusIdentifier),
                                                            onToggleCheckbox = { newNotes ->
                                                                vm.updateTaskItem(
                                                                    project.id, subProject.id, task.id,
                                                                    task.copy(notes = newNotes)
                                                                )
                                                            },
                                                            onEdit = {
                                                                openTaskDialog(
                                                                    TargetLocation(project.id, subProject.id, task.id),
                                                                    editing = true,
                                                                    task = task
                                                                )
                                                            },
                                                            onDelete = { 
                                                                vm.deleteTaskItem(project.id, subProject.id, task.id) 
                                                                NotificationHelper.cancelDeadline(ctx, task.id)
                                                            }
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
                }
            }
        }
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────────
    when (activeDialog) {

        // ── Add/Edit Project name ─────────────────────────────────────────────
        DLG_PROJECT -> AlertDialog(
            onDismissRequest = { activeDialog = DLG_NONE },
            containerColor = appColors.card2,
            title = {
                Text(
                    if (isEditingMode) "Modify Core Project" else "Generate Root Project",
                    color = TealCol, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = textInputState,
                    onValueChange = { textInputState = it },
                    label = { Text("Project Title", color = SubText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealCol,
                        focusedTextColor = TextCol,
                        unfocusedTextColor = TextCol
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isEditingMode) {
                        targetLoc?.let { vm.editProjectName(it.projectId, textInputState) }
                    } else {
                        vm.addProject(textInputState)
                    }
                    activeDialog = DLG_NONE
                }) { Text("COMMIT", color = TealCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = DLG_NONE }) {
                    Text("DISCARD", color = SubText)
                }
            }
        )

        // ── Add/Edit SubProject name ──────────────────────────────────────────
        DLG_SUBPROJECT -> AlertDialog(
            onDismissRequest = { activeDialog = DLG_NONE },
            containerColor = appColors.card2,
            title = {
                Text(
                    if (isEditingMode) "Modify Layer Block" else "Generate Segment Block",
                    color = TealCol, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = textInputState,
                    onValueChange = { textInputState = it },
                    label = { Text("SubProject Title", color = SubText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealCol,
                        focusedTextColor = TextCol,
                        unfocusedTextColor = TextCol
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    targetLoc?.let {
                        if (isEditingMode)
                            vm.editSubProjectName(it.projectId, it.subProjectId, textInputState)
                        else
                            vm.addSubProject(it.projectId, textInputState)
                    }
                    activeDialog = DLG_NONE
                }) { Text("COMMIT", color = TealCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = DLG_NONE }) {
                    Text("DISCARD", color = SubText)
                }
            }
        )

        // ── Add/Edit Task ─────────────────────────────────────────────────────
        DLG_TASK -> AlertDialog(
            onDismissRequest = { activeDialog = DLG_NONE },
            containerColor = appColors.card2,
            title = {
                Text(
                    if (isEditingMode) "Configure Objective Node" else "Generate Task Objective",
                    color = TealCol, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Title
                    OutlinedTextField(
                        value = textInputState,
                        onValueChange = { textInputState = it },
                        label = { Text("Title Reference", color = SubText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealCol,
                            focusedTextColor = TextCol,
                            unfocusedTextColor = TextCol
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Date pickers ──────────────────────────────────────────
                    Text("Timeline", color = TealCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Start date
                        Button(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, TealCol.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = TealCol, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (taskStartDate.isNotBlank())
                                    LocalDate.parse(taskStartDate, isoFmt).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                else "Start Date",
                                color = if (taskStartDate.isNotBlank()) TextCol else SubText,
                                fontSize = 11.sp
                            )
                        }
                        // End date
                        Button(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, TealCol.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = TealCol, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (taskEndDate.isNotBlank())
                                    LocalDate.parse(taskEndDate, isoFmt).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                else "End Date",
                                color = if (taskEndDate.isNotBlank()) TextCol else SubText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Timeline read-only label
                    if (taskTimelineState.isNotBlank()) {
                        Text(
                            text = "📅 $taskTimelineState",
                            color = TealCol.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // ── Notes macros ──────────────────────────────────────────
                    Text("Operational Logs Macros", color = TealCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { taskNotesState += if (taskNotesState.isEmpty() || taskNotesState.endsWith("\n")) "• " else "\n• " },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, BlueCol.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.List, null, tint = BlueCol, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("• Bullet", color = TextCol, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { taskNotesState += if (taskNotesState.isEmpty() || taskNotesState.endsWith("\n")) "[ ] " else "\n[ ] " },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, GreenCol.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = GreenCol, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("☑ Task", color = TextCol, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                if (taskNotesState.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(taskNotesState))
                                    Toast.makeText(ctx, "Notes copied", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, OrangeCol.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.weight(1.1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = OrangeCol, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Copy All", color = TextCol, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.isNotEmpty()) taskNotesState += clip
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.card),
                            border = BorderStroke(1.dp, SubText.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, null, tint = SubText, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Paste", color = TextCol, fontSize = 10.sp)
                        }
                    }

                    // ── Raw notes editor ──────────────────────────────────────
                    OutlinedTextField(
                        value = taskNotesState,
                        onValueChange = { taskNotesState = it },
                        label = { Text("Raw Notes / Code Input", color = SubText) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealCol,
                            focusedTextColor = TextCol,
                            unfocusedTextColor = TextCol
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)
                    )

                    // ── Live preview ──────────────────────────────────────────
                    if (taskNotesState.isNotBlank()) {
                        NotesPreview(
                            notesText = taskNotesState,
                            onNotesChanged = { taskNotesState = it },
                            appColors = appColors
                        )
                    }

                    // ── Status selector ───────────────────────────────────────
                    Text("Status", color = TealCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DataTaskStatus.values().forEach { s ->
                            val active = taskStatusState == s.identifier
                            val c = statusColor(s.identifier)
                            Button(
                                onClick = { taskStatusState = s.identifier },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) c else c.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(s.label, color = if (active) Color.White else c, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val loc = targetLoc ?: return@TextButton
                    val packageTask = TaskItem(
                        id = if (isEditingMode) loc.taskId else UUID.randomUUID().toString(),
                        title = textInputState,
                        notes = taskNotesState,
                        timeline = taskTimelineState,
                        startDate = taskStartDate,
                        endDate = taskEndDate,
                        statusIdentifier = taskStatusState
                    )
                    
                    // Trigger notification logic if enabled and task is not "Done"
                    if (notificationsEnabled && packageTask.statusIdentifier != DataTaskStatus.DONE.identifier) {
                        NotificationHelper.scheduleDeadline(ctx, packageTask.id, packageTask.title, packageTask.endDate)
                    } else if (packageTask.statusIdentifier == DataTaskStatus.DONE.identifier) {
                        NotificationHelper.cancelDeadline(ctx, packageTask.id) // Cancel if marked complete
                    }

                    if (loc.subProjectId.isBlank()) {
                        // Project-level task
                        if (isEditingMode) vm.updateProjectTask(loc.projectId, loc.taskId, packageTask)
                        else vm.addTaskToProject(loc.projectId, packageTask)
                    } else {
                        // SubProject-level task
                        if (isEditingMode) vm.updateTaskItem(loc.projectId, loc.subProjectId, loc.taskId, packageTask)
                        else vm.addTaskToSubProject(loc.projectId, loc.subProjectId, packageTask)
                    }
                    activeDialog = DLG_NONE
                }) { Text("COMMIT Changes", color = TealCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = DLG_NONE }) {
                    Text("DISCARD Changes", color = SubText)
                }
            }
        )

        // ── Settings ──────────────────────────────────────────────────────────
        DLG_SETTINGS -> AlertDialog(
            onDismissRequest = { activeDialog = DLG_NONE },
            containerColor = appColors.card2,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = TealCol, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Management Settings", color = TealCol, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Notification Toggle
                    Text("Notifications", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(appColors.card, RoundedCornerShape(8.dp)).border(1.dp, TealCol.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Task Deadlines", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Alerts at 9:00 AM on end date", color = SubText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { checked ->
                                notificationsEnabled = checked
                                prefs.edit().putBoolean(PREF_NOTIFS, checked).apply()
                                if (!checked) {
                                    WorkManager.getInstance(ctx).cancelAllWork()
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = SubText.copy(alpha = 0.2f))

                    // Export directory
                    Text("Export / Auto-Save Directory", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(appColors.card, RoundedCornerShape(8.dp))
                            .border(1.dp, TealCol.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, tint = TealCol, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = exportDirPath ?: "Default: Download/SLACKLINE ADMIN FILES",
                            color = if (exportDirPath != null) TextCol else SubText,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = { exportDirLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealCol.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, TealCol.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = TealCol, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Change Directory", color = TealCol, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = SubText.copy(alpha = 0.2f))

                    // Export / Import
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.exportData(); activeDialog = DLG_NONE },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenCol.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, GreenCol.copy(alpha = 0.5f)), modifier = Modifier.weight(1f)
                        ) { Text("Export All", color = GreenCol, fontSize = 11.sp) }
                        
                        Button(
                            onClick = { activeDialog = DLG_NONE; importFileLauncher.launch(arrayOf("application/json", "*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueCol.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, BlueCol.copy(alpha = 0.5f)), modifier = Modifier.weight(1f)
                        ) { Text("Import JSON", color = BlueCol, fontSize = 11.sp) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDialog = DLG_NONE }) {
                    Text("Done", color = TealCol, fontWeight = FontWeight.Bold)
                }
            }
        )

        // ── Import choice: Merge or Replace ──────────────────────────────────
        DLG_IMPORT_CHOICE -> AlertDialog(
            onDismissRequest = { activeDialog = DLG_NONE; pendingImportFile = null },
            containerColor = appColors.card2,
            title = {
                Text("Import Mode", color = TealCol, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "How should the imported data be handled?",
                        color = TextCol, fontSize = 13.sp
                    )
                    // Merge option
                    Button(
                        onClick = {
                            pendingImportFile?.let { vm.importAndMerge(it) }
                            pendingImportFile = null
                            activeDialog = DLG_NONE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenCol.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, GreenCol.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Merge", color = GreenCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Replace option
                    Button(
                        onClick = {
                            pendingImportFile?.let { vm.importAndReplace(it) }
                            pendingImportFile = null
                            activeDialog = DLG_NONE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedCol.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, RedCol.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Replace All", color = RedCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = DLG_NONE; pendingImportFile = null }) {
                    Text("Cancel", color = SubText)
                }
            },
            confirmButton = {}
        )
    }
}

// ─── Task node view (shared by project-level and subproject-level) ─────────────
@Composable
private fun TaskNodeView(
    task: TaskItem,
    statusColor: Color,
    onToggleCheckbox: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 4.dp).size(8.dp).background(statusColor, RoundedCornerShape(50)))
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable { onEdit() }
        ) {
            Text(task.title, color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (task.timeline.isNotBlank()) {
                Text(task.timeline, color = SubText, fontSize = 10.sp)
            }
            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                NotesReadView(
                    notesText = task.notes,
                    onToggle = onToggleCheckbox
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, "Remove Task", tint = RedCol, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Inline notes renderer (read/toggle mode) ─────────────────────────────────
@Composable
private fun NotesReadView(
    notesText: String,
    onToggle: (String) -> Unit
) {
    val lines = notesText.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEachIndexed { index, line ->
            val isUnchecked = line.startsWith("[ ] ")
            val isChecked   = line.startsWith("[x] ", ignoreCase = true)
            val isBullet    = line.startsWith("• ")
            when {
                isUnchecked || isChecked -> {
                    val content = line.substring(4)
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.clickable {
                            val newMarker = if (isUnchecked) "[x] " else "[ ] "
                            val newNotes = lines.toMutableList()
                                .apply { this[index] = newMarker + content }
                                .joinToString("\n")
                            onToggle(newNotes)
                        }
                    ) {
                        if (isChecked)
                            Icon(Icons.Default.CheckCircle, null, tint = GreenCol, modifier = Modifier.size(14.dp))
                        else
                            Box(modifier = Modifier.size(14.dp).border(1.5.dp, SubText.copy(alpha = 0.6f), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = content,
                            color = if (isChecked) SubText.copy(alpha = 0.5f) else SubText,
                            fontSize = 11.sp,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
                isBullet -> Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = TealCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(line.substring(2), color = SubText, fontSize = 11.sp)
                }
                line.isNotBlank() -> Text(line, color = SubText, fontSize = 11.sp)
            }
        }
    }
}

// ─── Inline notes preview in edit mode ───────────────────────────────────────
@Composable
private fun NotesPreview(
    notesText: String,
    onNotesChanged: (String) -> Unit,
    appColors: AppColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.bg, RoundedCornerShape(8.dp))
            .border(1.dp, TealCol.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        val lines = notesText.split("\n")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.forEachIndexed { index, line ->
                val isUnchecked = line.startsWith("[ ] ")
                val isChecked   = line.startsWith("[x] ", ignoreCase = true)
                val isBullet    = line.startsWith("• ")
                when {
                    isUnchecked || isChecked -> {
                        val content = line.substring(4)
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.clickable {
                                val newMarker = if (isUnchecked) "[x] " else "[ ] "
                                onNotesChanged(
                                    lines.toMutableList()
                                        .apply { this[index] = newMarker + content }
                                        .joinToString("\n")
                                )
                            }
                        ) {
                            if (isChecked)
                                Icon(Icons.Default.CheckCircle, null, tint = GreenCol, modifier = Modifier.size(14.dp))
                            else
                                Box(modifier = Modifier.size(14.dp).border(1.5.dp, SubText.copy(alpha = 0.6f), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = content,
                                color = if (isChecked) SubText.copy(alpha = 0.5f) else SubText,
                                fontSize = 11.sp,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                    isBullet -> Row(verticalAlignment = Alignment.Top) {
                        Text("•", color = TealCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text(line.substring(2), color = SubText, fontSize = 11.sp)
                    }
                    line.isNotBlank() -> Text(line, color = SubText, fontSize = 11.sp)
                }
            }
        }
    }
}

// ─── Status color helper ─────────────────────────────────────────────────────
private fun statusColor(identifier: String): Color = when (identifier) {
    DataTaskStatus.IN_PROGRESS.identifier -> GreenCol
    DataTaskStatus.DONE.identifier        -> BlueCol
    else                                  -> OrangeCol
}

// ─── URI → real file path helpers ────────────────────────────────────────────
private fun getRealPathFromUri(context: Context, uri: Uri): String? {
    return try {
        val docUri = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
        docUri?.uri?.path?.let { raw ->
            val colonIdx = raw.lastIndexOf(':')
            if (colonIdx != -1) {
                "/storage/emulated/0/${raw.substring(colonIdx + 1)}"
            } else raw
        }
    } catch (e: Exception) { null }
}

private fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "pm_import_temp.json")
        tempFile.outputStream().use { out -> inputStream.copyTo(out) }
        tempFile
    } catch (e: Exception) { null }
}
