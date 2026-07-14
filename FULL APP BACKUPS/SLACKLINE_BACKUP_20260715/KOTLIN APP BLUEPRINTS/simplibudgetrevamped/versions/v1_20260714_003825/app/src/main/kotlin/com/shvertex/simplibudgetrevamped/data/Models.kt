package com.shvertex.simplibudgetrevamped.data

import java.util.UUID

// ── Helpers ──────────────────────────────────────────────────────────────────

fun uid(): String = UUID.randomUUID().toString()

fun catColor(index: Int): String = listOf(
    "#00C896","#007AFF","#FF9F0A","#FF453A","#BF5AF2",
    "#30D158","#5E5CE6","#FF6B35","#00B4D8","#F72585",
    "#90E0EF","#B5E48C"
)[index % 12]

// ── Core models ───────────────────────────────────────────────────────────────

data class Transaction(
    val id: String = uid(),
    val name: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val type: String = "expense",     // "expense" | "income"
    val date: String = "",            // YYYY-MM-DD
    val note: String = "",
    val accountId: String = "",
    val recurringId: String = "",
    val autoBillPayment: Boolean = false,
    val billId: String = ""
)

data class IncomeEntry(
    val id: String = uid(),
    val name: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = "",
    val accountId: String = ""
)

data class BudgetCategory(
    val name: String = "",
    val planned: Double = 0.0,
    val color: String = "#00C896",
    val alertPct: Int = 80
)

data class SavingsGoal(
    val id: String = uid(),
    val name: String = "",
    val target: Double = 0.0,
    val saved: Double = 0.0,
    val deadline: String = "No deadline",
    val color: String = "#00C896"
)

data class SinkingFund(
    val id: String = uid(),
    val name: String = "",
    val target: Double = 0.0,
    val saved: Double = 0.0,
    val deadline: String = "No deadline",
    val monthlyNeeded: Double = 0.0
)

data class Bill(
    val id: String = uid(),
    val name: String = "",
    val amount: Double = 0.0,
    val dueDay: Int = 1,
    val category: String = "Subscriptions",
    val accountId: String = "",
    val paidMonths: List<String> = emptyList()
)

data class Debt(
    val id: String = uid(),
    val name: String = "",
    val balance: Double = 0.0,
    val originalBalance: Double = 0.0,
    val minPayment: Double = 0.0,
    val interestRate: Double = 0.0
)

data class Account(
    val id: String = uid(),
    val name: String = "",
    val type: String = "Bank",
    val openingBalance: Double = 0.0,
    val includeInNetworth: Boolean = true,
    val isActive: Boolean = true
)

data class AccountTransfer(
    val id: String = uid(),
    val fromAccountId: String = "",
    val toAccountId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = ""
)

data class RecurringTransaction(
    val id: String = uid(),
    val name: String = "",
    val amount: Double = 0.0,
    val day: Int = 1,
    val category: String = "Other",
    val type: String = "expense"
)

data class NetWorthEntry(
    val date: String = "",
    val value: Double = 0.0
)

data class Paycheck(
    val id: String = uid(),
    val day: Int = 1,
    val amount: Double = 0.0
)

data class AppSettings(
    val currency: String = "$",
    val budgetMonth: String = ""
)

// ── Root data container ───────────────────────────────────────────────────────

data class AppData(
    val income: List<IncomeEntry> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val recurring: List<RecurringTransaction> = emptyList(),
    val budgetCategories: List<BudgetCategory> = defaultCategories(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val sinkingFunds: List<SinkingFund> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val netWorthEntries: List<NetWorthEntry> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val accountTransfers: List<AccountTransfer> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val settings: AppSettings = AppSettings()
)

fun defaultCategories(): List<BudgetCategory> = listOf(
    "Housing","Food & Groceries","Transport","Utilities",
    "Health","Entertainment","Clothing","Education",
    "Savings","Debt Repayment","Subscriptions","Other"
).mapIndexed { i, n -> BudgetCategory(name = n, planned = 0.0, color = catColor(i)) }

val ACCOUNT_TYPES = listOf("Bank","Savings","Cash","E-wallet","Credit Card","Loan")
