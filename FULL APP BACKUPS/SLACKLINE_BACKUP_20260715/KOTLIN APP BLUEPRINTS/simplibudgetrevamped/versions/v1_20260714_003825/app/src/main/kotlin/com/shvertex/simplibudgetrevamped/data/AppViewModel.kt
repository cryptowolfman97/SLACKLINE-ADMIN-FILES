package com.shvertex.simplibudgetrevamped.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {

    var data by mutableStateOf(AppData())
        private set

    var prefs by mutableStateOf(AppPrefs())
        private set

    fun load() {
        data = loadData()
        prefs = loadPrefs()
    }

    fun save() {
        saveData(data)
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    fun upsertTransaction(tx: Transaction) {
        val list = data.transactions.toMutableList()
        val idx = list.indexOfFirst { it.id == tx.id }
        if (idx >= 0) list[idx] = tx else list.add(tx)
        data = data.copy(transactions = list)
        save()
    }

    fun deleteTransaction(id: String) {
        data = data.copy(transactions = data.transactions.filter { it.id != id })
        save()
    }

    // ── Income ────────────────────────────────────────────────────────────────

    fun upsertIncome(inc: IncomeEntry) {
        val list = data.income.toMutableList()
        val idx = list.indexOfFirst { it.id == inc.id }
        if (idx >= 0) list[idx] = inc else list.add(inc)
        data = data.copy(income = list)
        save()
    }

    fun deleteIncome(id: String) {
        data = data.copy(income = data.income.filter { it.id != id })
        save()
    }

    // ── Budget categories ─────────────────────────────────────────────────────

    fun updateCategory(updated: BudgetCategory) {
        val list = data.budgetCategories.toMutableList()
        val idx = list.indexOfFirst { it.name == updated.name }
        if (idx >= 0) list[idx] = updated else list.add(updated)
        data = data.copy(budgetCategories = list)
        save()
    }

    fun addCategory(cat: BudgetCategory) {
        data = data.copy(budgetCategories = data.budgetCategories + cat)
        save()
    }

    fun deleteCategory(name: String) {
        data = data.copy(budgetCategories = data.budgetCategories.filter { it.name != name })
        save()
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    fun upsertGoal(goal: SavingsGoal) {
        val list = data.savingsGoals.toMutableList()
        val idx = list.indexOfFirst { it.id == goal.id }
        if (idx >= 0) list[idx] = goal else list.add(goal)
        data = data.copy(savingsGoals = list)
        save()
    }

    fun deleteGoal(id: String) {
        data = data.copy(savingsGoals = data.savingsGoals.filter { it.id != id })
        save()
    }

    fun upsertFund(fund: SinkingFund) {
        val list = data.sinkingFunds.toMutableList()
        val idx = list.indexOfFirst { it.id == fund.id }
        if (idx >= 0) list[idx] = fund else list.add(fund)
        data = data.copy(sinkingFunds = list)
        save()
    }

    fun deleteFund(id: String) {
        data = data.copy(sinkingFunds = data.sinkingFunds.filter { it.id != id })
        save()
    }

    // ── Bills ─────────────────────────────────────────────────────────────────

    fun upsertBill(bill: Bill) {
        val list = data.bills.toMutableList()
        val idx = list.indexOfFirst { it.id == bill.id }
        if (idx >= 0) list[idx] = bill else list.add(bill)
        data = data.copy(bills = list)
        save()
    }

    fun deleteBill(id: String) {
        val txList = data.transactions.filter { !(it.autoBillPayment && it.billId == id) }
        data = data.copy(bills = data.bills.filter { it.id != id }, transactions = txList)
        save()
    }

    fun toggleBillPaid(billId: String, ym: String) {
        val bills = data.bills.toMutableList()
        var txList = data.transactions.toMutableList()
        val idx = bills.indexOfFirst { it.id == billId }
        if (idx < 0) return
        val bill = bills[idx]
        val paid = ym in bill.paidMonths
        if (paid) {
            bills[idx] = bill.copy(paidMonths = bill.paidMonths.filter { it != ym })
            txList = txList.filter { !(it.autoBillPayment && it.billId == billId && inMonth(it.date, ym)) }.toMutableList()
        } else {
            bills[idx] = bill.copy(paidMonths = bill.paidMonths + ym)
            val existingTx = txList.any { it.autoBillPayment && it.billId == billId && inMonth(it.date, ym) }
            if (!existingTx) {
                txList.add(Transaction(
                    id = uid(), name = bill.name, amount = bill.amount,
                    category = bill.category, type = "expense",
                    date = "$ym-${bill.dueDay.toString().padStart(2, '0')}",
                    note = "Bill payment: ${bill.name}",
                    accountId = bill.accountId, autoBillPayment = true, billId = billId
                ))
            }
        }
        data = data.copy(bills = bills, transactions = txList)
        save()
    }

    // ── Debts ─────────────────────────────────────────────────────────────────

    fun upsertDebt(debt: Debt) {
        val list = data.debts.toMutableList()
        val idx = list.indexOfFirst { it.id == debt.id }
        if (idx >= 0) list[idx] = debt else list.add(debt)
        data = data.copy(debts = list)
        save()
    }

    fun deleteDebt(id: String) {
        data = data.copy(debts = data.debts.filter { it.id != id })
        save()
    }

    fun recordDebtPayment(debtId: String, amount: Double) {
        val list = data.debts.toMutableList()
        val idx = list.indexOfFirst { it.id == debtId }
        if (idx < 0) return
        val d = list[idx]
        list[idx] = d.copy(balance = maxOf(0.0, d.balance - amount))
        data = data.copy(debts = list)
        save()
    }

    // ── Accounts ──────────────────────────────────────────────────────────────

    fun upsertAccount(acc: Account) {
        val list = data.accounts.toMutableList()
        val idx = list.indexOfFirst { it.id == acc.id }
        if (idx >= 0) list[idx] = acc else list.add(acc)
        data = data.copy(accounts = list)
        save()
    }

    fun deleteAccount(id: String) {
        val txList = data.transactions.map { if (it.accountId == id) it.copy(accountId = "") else it }
        val incList = data.income.map { if (it.accountId == id) it.copy(accountId = "") else it }
        val billList = data.bills.map { if (it.accountId == id) it.copy(accountId = "") else it }
        val trList = data.accountTransfers.filter { it.fromAccountId != id && it.toAccountId != id }
        data = data.copy(
            accounts = data.accounts.filter { it.id != id },
            transactions = txList, income = incList,
            bills = billList, accountTransfers = trList
        )
        save()
    }

    fun addTransfer(tr: AccountTransfer) {
        data = data.copy(accountTransfers = data.accountTransfers + tr)
        save()
    }

    // ── Net Worth ─────────────────────────────────────────────────────────────

    fun addNetWorthEntry(entry: NetWorthEntry) {
        data = data.copy(netWorthEntries = data.netWorthEntries + entry)
        save()
    }

    // ── Recurring ─────────────────────────────────────────────────────────────

    fun upsertRecurring(rec: RecurringTransaction) {
        val list = data.recurring.toMutableList()
        val idx = list.indexOfFirst { it.id == rec.id }
        if (idx >= 0) list[idx] = rec else list.add(rec)
        data = data.copy(recurring = list)
        save()
    }

    fun deleteRecurring(id: String) {
        data = data.copy(recurring = data.recurring.filter { it.id != id })
        save()
    }

    fun processRecurring() {
        val ym = currentMonth()
        val txList = data.transactions.toMutableList()
        var changed = false
        data.recurring.forEach { rec ->
            val already = txList.any { it.recurringId == rec.id && inMonth(it.date, ym) }
            if (!already) {
                txList.add(Transaction(
                    id = uid(), name = rec.name, amount = rec.amount,
                    category = rec.category, type = rec.type,
                    date = "$ym-${rec.day.toString().padStart(2, '0')}",
                    note = "Auto: ${rec.name}", recurringId = rec.id
                ))
                changed = true
            }
        }
        if (changed) { data = data.copy(transactions = txList); save() }
    }

    // ── Settings & prefs ──────────────────────────────────────────────────────

    fun updateCurrency(symbol: String) {
        data = data.copy(settings = data.settings.copy(currency = symbol))
        save()
    }

    fun updatePrefs(updated: AppPrefs) {
        prefs = updated
        savePrefs(updated)
    }

    // ── Paychecks ─────────────────────────────────────────────────────────────

    fun upsertPaycheck(pc: Paycheck) {
        val list = data.paychecks.toMutableList()
        val idx = list.indexOfFirst { it.id == pc.id }
        if (idx >= 0) list[idx] = pc else list.add(pc)
        data = data.copy(paychecks = list)
        save()
    }

    fun deletePaycheck(id: String) {
        data = data.copy(paychecks = data.paychecks.filter { it.id != id })
        save()
    }

    // ── Backup / restore ──────────────────────────────────────────────────────

    fun restoreFromJson(json: String) {
        val (restoredData, restoredPrefs) = restoreBackupJson(json)
        data = restoredData
        if (restoredPrefs != null) { prefs = restoredPrefs; savePrefs(prefs) }
        save()
    }
}
