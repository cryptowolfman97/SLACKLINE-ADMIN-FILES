package com.example.slacklineadminapp.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SyncedCalendarEntry(
    val taskId: String,
    val projectId: String,
    val projectName: String,
    val subProjectName: String,
    val title: String,
    val notes: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val statusIdentifier: String
)

class ProjectViewModel : ViewModel() {

    // ─── State properties must be declared BEFORE the init block ──────────
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _toastMessage = MutableStateFlow("")
    val toastMessage: StateFlow<String> = _toastMessage.asStateFlow()

    private val _exportDirectoryPath = MutableStateFlow<String?>(null)
    val exportDirectoryPath: StateFlow<String?> = _exportDirectoryPath.asStateFlow()

    val calendarEntries: StateFlow<List<SyncedCalendarEntry>> = _projects
        .map { buildCalendarEntries(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // ─── Sanitise helper (removes all Gson nulls) ─────────────────────────
    private fun sanitise(list: List<Project>): List<Project> {
        return list.map { project ->
            project.copy(
                title = project.title.orEmpty(),
                tasks = project.tasks.orEmpty().map { task ->
                    task.copy(
                        title            = task.title.orEmpty(),
                        notes            = task.notes.orEmpty(),
                        timeline         = task.timeline.orEmpty(),
                        startDate        = task.startDate.orEmpty(),
                        endDate          = task.endDate.orEmpty(),
                        statusIdentifier = task.statusIdentifier.orEmpty()
                    )
                },
                subProjects = project.subProjects.orEmpty().map { sub ->
                    sub.copy(
                        title = sub.title.orEmpty(),
                        tasks = sub.tasks.orEmpty().map { task ->
                            task.copy(
                                title            = task.title.orEmpty(),
                                notes            = task.notes.orEmpty(),
                                timeline         = task.timeline.orEmpty(),
                                startDate        = task.startDate.orEmpty(),
                                endDate          = task.endDate.orEmpty(),
                                statusIdentifier = task.statusIdentifier.orEmpty()
                            )
                        }
                    )
                }
            )
        }
    }

    private fun buildCalendarEntries(projectList: List<Project>): List<SyncedCalendarEntry> {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val result = mutableListOf<SyncedCalendarEntry>()

        projectList.forEach { project ->
            project.tasks.orEmpty().forEach { task ->
                parsedEntry(task, project.id, project.title, "", fmt)?.let { result.add(it) }
            }
            project.subProjects.orEmpty().forEach { sub ->
                sub.tasks.orEmpty().forEach { task ->
                    parsedEntry(task, project.id, project.title, sub.title, fmt)?.let { result.add(it) }
                }
            }
        }
        return result
    }

    private fun parsedEntry(
        task: TaskItem,
        projectId: String,
        projectName: String,
        subName: String,
        fmt: DateTimeFormatter
    ): SyncedCalendarEntry? {
        val start = task.startDate.orEmpty()
        val end   = task.endDate.orEmpty()
        if (start.isBlank() || end.isBlank()) return null
        return try {
            SyncedCalendarEntry(
                taskId          = task.id.orEmpty(),
                projectId       = projectId,
                projectName     = projectName,
                subProjectName  = subName,
                title           = task.title.orEmpty(),
                notes           = task.notes.orEmpty(),
                startDate       = LocalDate.parse(start, fmt),
                endDate         = LocalDate.parse(end, fmt),
                statusIdentifier = task.statusIdentifier.orEmpty()
            )
        } catch (e: Exception) { null }
    }

    fun loadData() {
        // Read exactly from where we are targeting saves to avoid mismatch
        val targetPath = _exportDirectoryPath.value ?: ProjectStorageEngine.getDefaultDirectory().absolutePath
        _projects.value = sanitise(ProjectStorageEngine.loadProjectTree(File(targetPath)))
    }

    fun setExportDirectory(path: String) {
        _exportDirectoryPath.value = path
    }

    fun consumeToast() {
        _toastMessage.value = ""
    }

    private fun toast(msg: String) { _toastMessage.value = msg }

    private fun updateAndCommit(updatedList: List<Project>) {
        _projects.value = updatedList
        // Fallback safely to root file engine folder directory if custom export directory isn't defined yet
        val targetPath = _exportDirectoryPath.value ?: ProjectStorageEngine.getDefaultDirectory().absolutePath
        val dir = File(targetPath)
        if (!ProjectStorageEngine.saveProjectTree(updatedList, dir)) {
            toast("Storage write pipeline failed")
        }
    }

    fun exportData(): Boolean {
        val dirPath = _exportDirectoryPath.value
            ?: ProjectStorageEngine.getDefaultDirectory().absolutePath
        val dir = File(dirPath)
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val outputFile = File(dir, "pm_export_$timestamp.json")
        val success = ProjectStorageEngine.exportToFile(_projects.value, outputFile)
        toast(if (success) "Exported to ${outputFile.name}" else "Export failed")
        return success
    }

    fun importAndMerge(sourceFile: File) {
        val imported = ProjectStorageEngine.importFromFile(sourceFile)
        if (imported == null) {
            toast("Import failed — invalid file")
            return
        }
        val cleanImported = sanitise(imported)
        val existingIds = _projects.value.map { it.id }.toSet()
        val newOnes = cleanImported.filterNot { it.id in existingIds }
        updateAndCommit(_projects.value + newOnes)
        toast("Merged ${newOnes.size} new project(s)")
    }

    fun importAndReplace(sourceFile: File) {
        val imported = ProjectStorageEngine.importFromFile(sourceFile)
        if (imported == null) {
            toast("Import failed — invalid file")
            return
        }
        updateAndCommit(sanitise(imported))
        toast("Replaced with ${imported.size} project(s) from file")
    }

    fun addProject(title: String) {
        if (title.isBlank()) return
        updateAndCommit(_projects.value + Project(title = title.trim()))
    }

    fun editProjectName(projectId: String, newName: String) {
        if (newName.isBlank()) return
        updateAndCommit(_projects.value.map {
            if (it.id == projectId) it.copy(title = newName.trim()) else it
        })
    }

    fun deleteProject(projectId: String) {
        updateAndCommit(_projects.value.filterNot { it.id == projectId })
    }

    fun addTaskToProject(projectId: String, task: TaskItem) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId) project.copy(tasks = project.tasks + task) else project
        })
    }

    fun updateProjectTask(projectId: String, taskId: String, updatedTask: TaskItem) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId)
                project.copy(tasks = project.tasks.map { if (it.id == taskId) updatedTask else it })
            else project
        })
    }

    fun deleteProjectTask(projectId: String, taskId: String) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId) project.copy(tasks = project.tasks.filterNot { it.id == taskId }) else project
        })
    }

    fun addSubProject(projectId: String, title: String) {
        if (title.isBlank()) return
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId) project.copy(subProjects = project.subProjects + SubProject(title = title.trim()))
            else project
        })
    }

    fun editSubProjectName(projectId: String, subProjectId: String, newName: String) {
        if (newName.isBlank()) return
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId)
                project.copy(subProjects = project.subProjects.map { sub ->
                    if (sub.id == subProjectId) sub.copy(title = newName.trim()) else sub
                })
            else project
        })
    }

    fun deleteSubProject(projectId: String, subProjectId: String) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId) project.copy(subProjects = project.subProjects.filterNot { it.id == subProjectId }) else project
        })
    }

    fun addTaskToSubProject(projectId: String, subProjectId: String, task: TaskItem) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId)
                project.copy(subProjects = project.subProjects.map { sub ->
                    if (sub.id == subProjectId) sub.copy(tasks = sub.tasks + task) else sub
                })
            else project
        })
    }

    fun updateTaskItem(projectId: String, subProjectId: String, taskId: String, updatedTask: TaskItem) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId)
                project.copy(subProjects = project.subProjects.map { sub ->
                    if (sub.id == subProjectId) sub.copy(tasks = sub.tasks.map { if (it.id == taskId) updatedTask else it }) else sub
                })
            else project
        })
    }

    fun deleteTaskItem(projectId: String, subProjectId: String, taskId: String) {
        updateAndCommit(_projects.value.map { project ->
            if (project.id == projectId)
                project.copy(subProjects = project.subProjects.map { sub ->
                    if (sub.id == subProjectId) sub.copy(tasks = sub.tasks.filterNot { it.id == taskId }) else sub
                })
            else project
        })
    }
}

private fun String?.orEmpty(): String = this ?: ""
