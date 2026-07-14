package com.example.interstellarcalc.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SimbadRepository {

    private const val TAP_URL = "https://simbad.cds.unistra.fr/simbad/sim-tap/sync"

    suspend fun search(query: String, category: UniverseCategory, limit: Int = 30): List<UniverseObject> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        // 1. Sanitize user input and create variations for case-insensitivity
        val safeOriginal = query.replace("'", "''").trim()
        val safeUpper = safeOriginal.uppercase()
        val safeLower = safeOriginal.lowercase()
        
        // FIX 1: Force lowercase first so "TON 618" correctly becomes "Ton 618"
        val safeTitle = safeLower.replaceFirstChar { it.uppercase() }
        
        // FIX 2: Replace spaces with '%' to catch SIMBAD's inconsistent spacing
        val searchTerms = setOf(safeOriginal, safeUpper, safeLower, safeTitle).map { it.replace(" ", "%") }
        
        // The LIKE query will now look like: i.id LIKE 'Ton%618%'
        val likeClauses = searchTerms.joinToString(" OR ") { "i.id LIKE '$it%'" }


        // 2. Build the category filtering clause
        val typeClause = if (category != UniverseCategory.ALL && category != UniverseCategory.OTHER && category.simbadTypes.isNotEmpty())
            "AND (" + category.simbadTypes.joinToString(" OR ") { "b.otype='$it'" } + ")"
        else ""

        // 3. Assemble the corrected ADQL string
        val adql = "SELECT TOP $limit b.main_id, b.otype, b.ra, b.dec, b.plx_value, b.rvz_redshift AS z_value, f.B AS flux_b, f.V AS flux_v, b.sp_type FROM basic AS b JOIN ident AS i ON b.oid=i.oidref LEFT JOIN allfluxes AS f ON b.oid=f.oidref WHERE ($likeClauses) $typeClause ORDER BY flux_v ASC"
        
        val fullUrl = "$TAP_URL?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=${URLEncoder.encode(adql, "UTF-8")}"

        return withContext(Dispatchers.IO) {
            val conn = URL(fullUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 20000
            conn.readTimeout    = 30000
            conn.requestMethod  = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "InterstellarCalc/1.0")
            try {
                val code = conn.responseCode
                if (code != 200) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: "no error body"
                    throw Exception("HTTP $code: $err")
                }
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                parseJson(body)
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun parseJson(json: String): List<UniverseObject> {
        val results = mutableListOf<UniverseObject>()
        try {
            val dataIdx  = json.indexOf("\"data\":")
            if (dataIdx < 0) return emptyList()
            val arrStart = json.indexOf('[', dataIdx) + 1
            if (arrStart <= 0) return emptyList()

            for (row in splitRows(json, arrStart)) {
                val cols = parseRow(row)
                if (cols.size < 9) continue

                val mainId = cols[0].unquote()
                val otype  = cols[1].unquote()
                if (mainId.isBlank() || mainId == "null") continue

                val ra     = cols[2].unquote()
                val dec    = cols[3].unquote()
                val plx    = cols[4].toDoubleOrNull()
                val z      = cols[5].toDoubleOrNull()
                val fluxB  = cols[6].toDoubleOrNull()
                val fluxV  = cols[7].toDoubleOrNull()
                val spType = cols[8].unquote().ifBlank { null }

                val distLy = when {
                    plx != null && plx > 0.0 -> (1000.0 / plx) * 3.2616
                    z   != null && z   > 0.0 -> (z * 3e8 / 70.0) * 3.26156e6
                    else -> null
                }

                results.add(UniverseObject(
                    id           = mainId,
                    name         = mainId,
                    objectType   = mapSimbadType(otype),
                    rawType      = otype,
                    ra           = ra,
                    dec          = dec,
                    redshift     = z,
                    parallaxMas  = plx,
                    distanceLy   = distLy,
                    bMagnitude   = fluxB,
                    vMagnitude   = fluxV,
                    spectralType = spType,
                    nasaImageUrl = null
                ))
            }
        } catch (e: Exception) {
            throw Exception("Parse error: ${e.message}")
        }
        return results
    }

    private fun splitRows(json: String, fromIdx: Int): List<String> {
        val rows  = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inStr = false
        var i     = fromIdx
        while (i < json.length) {
            val c = json[i]
            when {
                c == '"' && (i == 0 || json[i - 1] != '\\') -> inStr = !inStr
                !inStr && c == '[' -> { if (depth == 0) start = i; depth++ }
                !inStr && c == ']' -> {
                    depth--
                    if (depth == 0 && start >= 0) { rows.add(json.substring(start, i + 1)); start = -1 }
                    else if (depth < 0) break
                }
            }
            i++
        }
        return rows
    }

    private fun parseRow(row: String): List<String> {
        val inner = row.trim().removePrefix("[").removeSuffix("]")
        val cols  = mutableListOf<String>()
        var inStr = false
        var buf   = StringBuilder()
        var k     = 0
        while (k < inner.length) {
            val c = inner[k]
            when {
                c == '"' && (k == 0 || inner[k - 1] != '\\') -> { inStr = !inStr; buf.append(c) }
                c == ',' && !inStr -> { cols.add(buf.toString().trim()); buf = StringBuilder() }
                else -> buf.append(c)
            }
            k++
        }
        if (buf.isNotEmpty()) cols.add(buf.toString().trim())
        return cols
    }

    private fun String.unquote() = this.trim().trim('"')

    fun mapSimbadType(otype: String): UniverseObjectType {
        val o = otype.trim()
        return when {
            o == "PSR" || o.contains("Pulsar", true)                     -> UniverseObjectType.PULSAR
            o == "BH"  || o == "XB*" || o == "LMXB" || o == "HMXB"     -> UniverseObjectType.BLACK_HOLE
            o == "GlCl"                                                   -> UniverseObjectType.CLUSTER_GLOBULAR
            o == "OpCl" || o == "Cl*" || o == "Association*"            -> UniverseObjectType.CLUSTER_OPEN
            o == "PN"                                                     -> UniverseObjectType.NEBULA_PLANETARY
            o == "SNR"                                                    -> UniverseObjectType.NEBULA_SUPERNOVA
            o == "HII" || o == "Neb" || o == "EmObj" || o == "ISM"
                       || o.contains("Neb", true)                        -> UniverseObjectType.NEBULA_EMISSION
            o == "QSO" || o.contains("Quasar", true)
                       || o.contains("Blazar", true) || o == "AGN"      -> UniverseObjectType.QUASAR
            o.startsWith("G") && !o.endsWith("*")                        -> UniverseObjectType.GALAXY_SPIRAL
            o.endsWith("*") || o == "Star" || o.contains("Star", true)
                       || o.contains("WD", true)                         -> UniverseObjectType.STAR
            else                                                          -> UniverseObjectType.UNKNOWN
        }
    }
}
