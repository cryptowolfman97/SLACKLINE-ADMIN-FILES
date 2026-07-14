package com.example.interstellarcalc.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

data class DeepSkyObject(
    val id             : Int,
    val name           : String,
    val commonName     : String?,
    val objectType     : String,
    val category       : String,
    val constellation  : String?,
    val raDeg          : Double?,
    val decDeg         : Double?,
    val magnitude      : Double?,
    val distanceLy     : Double?,
    val massSolar      : Double?,
    val radiusSolar    : Double?,
    val tempKelvin     : Int?,
    val spectralType   : String?,
    val description    : String?,
    val funFact        : String?
) {
    val displayName: String get() = commonName?.takeIf { it.isNotBlank() } ?: name
}

class DatabaseHelper private constructor(context: Context) {

    private val db: SQLiteDatabase

    init {
        val dbFile = File(context.filesDir, DB_NAME)
        if (!dbFile.exists() || dbFile.length() < 10000L) {
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
            }
        }
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    companion object {
        private const val DB_NAME = "deep_sky.db"

        @Volatile private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun search(query: String, category: String? = null, limit: Int = 50): List<DeepSkyObject> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val like = "%$q%"
        val args = mutableListOf<String>()
        val catClause = if (category != null && category != "ALL") {
            args.add(category)
            "AND category = ?"
        } else ""

        // Search name, common_name, constellation, object_type
        val sql = """
            SELECT * FROM objects
            WHERE (name LIKE ? OR common_name LIKE ? OR constellation LIKE ? OR object_type LIKE ?)
            $catClause
            ORDER BY
                CASE WHEN LOWER(name) = LOWER(?) THEN 0
                     WHEN LOWER(common_name) = LOWER(?) THEN 1
                     WHEN LOWER(name) LIKE LOWER(?) THEN 2
                     ELSE 3 END,
                magnitude ASC
            LIMIT $limit
        """.trimIndent()

        val finalArgs = arrayOf(like, like, like, like, *args.toTypedArray(), q, q, like)
        val cursor = db.rawQuery(sql, finalArgs)
        val results = mutableListOf<DeepSkyObject>()
        cursor.use {
            while (it.moveToNext()) results.add(fromCursor(it))
        }
        return results
    }

    fun getById(id: Int): DeepSkyObject? {
        val cursor = db.rawQuery("SELECT * FROM objects WHERE id = ?", arrayOf(id.toString()))
        return cursor.use { if (it.moveToFirst()) fromCursor(it) else null }
    }

    fun getByCategory(category: String, limit: Int = 100): List<DeepSkyObject> {
        val sql = "SELECT * FROM objects WHERE category = ? ORDER BY magnitude ASC LIMIT ?"
        val cursor = db.rawQuery(sql, arrayOf(category, limit.toString()))
        val results = mutableListOf<DeepSkyObject>()
        cursor.use { while (it.moveToNext()) results.add(fromCursor(it)) }
        return results
    }

    fun getFeatured(limit: Int = 20): List<DeepSkyObject> {
        // Return notable objects across all categories
        val sql = """
            SELECT * FROM objects
            WHERE common_name IS NOT NULL AND common_name != ''
            AND magnitude IS NOT NULL
            ORDER BY magnitude ASC
            LIMIT ?
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(limit.toString()))
        val results = mutableListOf<DeepSkyObject>()
        cursor.use { while (it.moveToNext()) results.add(fromCursor(it)) }
        return results
    }

    private fun fromCursor(c: android.database.Cursor): DeepSkyObject {
        fun str(col: String) = c.getColumnIndex(col).let { if (it >= 0 && !c.isNull(it)) c.getString(it) else null }
        fun dbl(col: String) = c.getColumnIndex(col).let { if (it >= 0 && !c.isNull(it)) c.getDouble(it) else null }
        fun int(col: String) = c.getColumnIndex(col).let { if (it >= 0 && !c.isNull(it)) c.getInt(it) else null }
        return DeepSkyObject(
            id           = c.getInt(c.getColumnIndexOrThrow("id")),
            name         = c.getString(c.getColumnIndexOrThrow("name")),
            commonName   = str("common_name"),
            objectType   = c.getString(c.getColumnIndexOrThrow("object_type")),
            category     = c.getString(c.getColumnIndexOrThrow("category")),
            constellation= str("constellation"),
            raDeg        = dbl("ra_deg"),
            decDeg       = dbl("dec_deg"),
            magnitude    = dbl("magnitude"),
            distanceLy   = dbl("distance_ly"),
            massSolar    = dbl("mass_solar"),
            radiusSolar  = dbl("radius_solar"),
            tempKelvin   = int("temp_kelvin"),
            spectralType = str("spectral_type"),
            description  = str("description"),
            funFact      = str("fun_fact")
        )
    }
}
