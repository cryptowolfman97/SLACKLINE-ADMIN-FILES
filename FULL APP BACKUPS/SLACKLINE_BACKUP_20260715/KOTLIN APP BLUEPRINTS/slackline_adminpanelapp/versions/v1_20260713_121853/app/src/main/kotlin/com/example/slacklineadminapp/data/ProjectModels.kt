package com.example.slacklineadminapp.data

import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.UUID

enum class TaskStatus(val label: String, val identifier: String) {
    TODO("To Do", "todo"),
    IN_PROGRESS("In Progress", "in_progress"),
    DONE("Done", "done")
}

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val timeline: String = "",      // Human-readable e.g. "Jan 05, 2025 → Jan 12, 2025"
    val startDate: String = "",     // ISO format: "yyyy-MM-dd"
    val endDate: String = "",       // ISO format: "yyyy-MM-dd"
    val statusIdentifier: String = TaskStatus.TODO.identifier
)

data class SubProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tasks: List<TaskItem> = emptyList()
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tasks: List<TaskItem> = emptyList(),        // ← Direct project-level tasks
    val subProjects: List<SubProject> = emptyList()
)

object ProjectStorageEngine {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun saveProjectTree(projects: List<Project>, targetDir: File? = null): Boolean {
        return try {
            val dir = targetDir ?: getDefaultDirectory()
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "projects_database.json")
            file.writeText(gson.toJson(projects))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadProjectTree(sourceDir: File? = null): List<Project> {
        return try {
            val dir = sourceDir ?: getDefaultDirectory()
            val file = File(dir, "projects_database.json")
            if (!file.exists()) return emptyList()
            val array = gson.fromJson(file.readText(), Array<Project>::class.java)
            array.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Export full tree to a named backup file in an arbitrary directory
    fun exportToFile(projects: List<Project>, outputFile: File): Boolean {
        return try {
            if (!outputFile.parentFile?.exists()!!) outputFile.parentFile?.mkdirs()
            outputFile.writeText(gson.toJson(projects))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Import from any json file — returns parsed list or null on error
    fun importFromFile(sourceFile: File): List<Project>? {
        return try {
            val array = gson.fromJson(sourceFile.readText(), Array<Project>::class.java)
            array.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDefaultDirectory(): File {
        return AppStorage.projectManagementDir()
    }
}
