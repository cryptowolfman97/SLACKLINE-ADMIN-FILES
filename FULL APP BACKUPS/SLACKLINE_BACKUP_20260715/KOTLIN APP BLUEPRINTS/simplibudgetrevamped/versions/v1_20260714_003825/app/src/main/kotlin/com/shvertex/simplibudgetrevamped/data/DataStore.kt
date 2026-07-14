package com.shvertex.simplibudgetrevamped.data

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Storage paths ─────────────────────────────────────────────────────────────

private const val STORAGE_FOLDER = "SimpliBudget SHV"
private const val DATA_FILE = "simplibudget_data.json"
private const val PREFS_FILE = "simplibudget_prefs.json"

fun storageRoot(): File {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    return File(downloads, STORAGE_FOLDER).also { it.mkdirs() }
}

fun dataFile(): File = File(storageRoot(), DATA_FILE)
fun prefsFile(): File = File(storageRoot(), PREFS_FILE)
fun exportsDir(): File = File(storageRoot(), "Exports").also { it.mkdirs() }
fun backupsDir(): File = File(storageRoot(), "Backups").also { it.mkdirs() }

// ── Gson instance ─────────────────────────────────────────────────────────────

val gson: Gson = GsonBuilder().setPrettyPrinting().create()

// ── Prefs ─────────────────────────────────────────────────────────────────────

data class AppPrefs(
    val theme: String = "amoled",
    val pin: String = "",
    val alerts: Boolean = true,
    val rollover: Boolean = false,
    val onboardingDone: Boolean = false
)

fun loadPrefs(): AppPrefs {
    val f = prefsFile()
    return try {
        if (f.exists()) gson.fromJson(f.readText(), AppPrefs::class.java) ?: AppPrefs()
        else AppPrefs()
    } catch (e: Exception) { AppPrefs() }
}

fun savePrefs(prefs: AppPrefs) {
    prefsFile().writeText(gson.toJson(prefs))
}

// ── Data load / save ──────────────────────────────────────────────────────────

fun loadData(): AppData {
    val f = dataFile()
    return try {
        if (f.exists()) gson.fromJson(f.readText(), AppData::class.java) ?: AppData()
        else AppData()
    } catch (e: Exception) { AppData() }
}

fun saveData(data: AppData) {
    dataFile().writeText(gson.toJson(data))
}

// ── Date helpers ──────────────────────────────────────────────────────────────

fun currentMonth(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun inMonth(dateStr: String, ym: String): Boolean =
    dateStr.length >= 7 && dateStr.substring(0, 7) == ym

fun formatAmount(amount: Double, currency: String = "$"): String =
    "$currency${"%,.2f".format(amount)}"

fun monthLabel(ym: String): String = try {
    val d = LocalDate.parse("$ym-01")
    d.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
} catch (e: Exception) { ym }

fun allMonthsInData(data: AppData): List<String> {
    val months = mutableSetOf(currentMonth())
    data.transactions.forEach { t -> if (t.date.length >= 7) months.add(t.date.substring(0, 7)) }
    data.income.forEach { i -> if (i.date.length >= 7) months.add(i.date.substring(0, 7)) }
    return months.sortedDescending()
}

fun lastNMonths(n: Int = 6): List<String> {
    val result = mutableListOf<String>()
    val now = LocalDate.now()
    for (i in n - 1 downTo 0) {
        result.add(now.minusMonths(i.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }
    return result
}

// ── Financial calculations ────────────────────────────────────────────────────

fun monthlyIncomeTotal(data: AppData, ym: String): Double {
    var total = 0.0
    data.income.filter { inMonth(it.date, ym) }.forEach { total += it.amount }
    data.transactions.filter { inMonth(it.date, ym) && it.type == "income" }.forEach { total += it.amount }
    return total
}

fun monthlyExpenseCategoryTotals(data: AppData, ym: String): Map<String, Double> {
    val totals = mutableMapOf<String, Double>()
    data.transactions
        .filter { inMonth(it.date, ym) && it.type == "expense" }
        .forEach { totals[it.category] = (totals[it.category] ?: 0.0) + it.amount }
    return totals
}

fun accountBalances(data: AppData): Map<String, Double> {
    val balances = mutableMapOf<String, Double>()
    data.accounts.forEach { balances[it.id] = it.openingBalance }
    data.income.forEach { inc ->
        if (inc.accountId.isNotEmpty() && balances.containsKey(inc.accountId))
            balances[inc.accountId] = (balances[inc.accountId] ?: 0.0) + inc.amount
    }
    data.transactions.forEach { tx ->
        if (tx.accountId.isNotEmpty() && balances.containsKey(tx.accountId)) {
            when (tx.type) {
                "expense" -> balances[tx.accountId] = (balances[tx.accountId] ?: 0.0) - tx.amount
                "income"  -> balances[tx.accountId] = (balances[tx.accountId] ?: 0.0) + tx.amount
            }
        }
    }
    data.accountTransfers.forEach { tr ->
        if (balances.containsKey(tr.fromAccountId))
            balances[tr.fromAccountId] = (balances[tr.fromAccountId] ?: 0.0) - tr.amount
        if (balances.containsKey(tr.toAccountId))
            balances[tr.toAccountId] = (balances[tr.toAccountId] ?: 0.0) + tr.amount
    }
    return balances
}

fun totalActiveBalance(data: AppData): Double =
    accountBalances(data).entries.sumOf { (id, bal) ->
        val acc = data.accounts.find { it.id == id }
        if (acc != null && acc.isActive) bal else 0.0
    }

fun billPaidInMonth(bill: Bill, ym: String): Boolean = ym in bill.paidMonths

// ── CSV export ────────────────────────────────────────────────────────────────

fun exportTransactionsCsv(data: AppData, ym: String): File {
    val file = File(exportsDir(), "simplibudget_transactions_$ym.csv")
    file.bufferedWriter().use { w ->
        w.write("Date,Name,Category,Type,Amount,Note\n")
        data.transactions
            .filter { inMonth(it.date, ym) }
            .sortedBy { it.date }
            .forEach { w.write("${it.date},${it.name},${it.category},${it.type},${it.amount},${it.note}\n") }
    }
    return file
}

fun exportBudgetCsv(data: AppData, ym: String): File {
    val catSpent = monthlyExpenseCategoryTotals(data, ym)
    val file = File(exportsDir(), "simplibudget_budget_$ym.csv")
    file.bufferedWriter().use { w ->
        w.write("Category,Planned,Spent,Remaining\n")
        data.budgetCategories.forEach { cat ->
            val sp = catSpent[cat.name] ?: 0.0
            w.write("${cat.name},${cat.planned},${sp},${cat.planned - sp}\n")
        }
    }
    return file
}

fun fullBackupJson(data: AppData, prefs: AppPrefs): String {
    val payload = mapOf("app" to "SimpliBudget SHV", "data" to data, "prefs" to prefs)
    return gson.toJson(payload)
}

fun restoreBackupJson(json: String): Pair<AppData, AppPrefs?> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val map: Map<String, Any> = gson.fromJson(json, type)
    val dataJson = gson.toJson(map["data"])
    val prefsJson = gson.toJson(map["prefs"])
    val data = gson.fromJson(dataJson, AppData::class.java) ?: AppData()
    val prefs = try { gson.fromJson(prefsJson, AppPrefs::class.java) } catch (e: Exception) { null }
    return Pair(data, prefs)
}
