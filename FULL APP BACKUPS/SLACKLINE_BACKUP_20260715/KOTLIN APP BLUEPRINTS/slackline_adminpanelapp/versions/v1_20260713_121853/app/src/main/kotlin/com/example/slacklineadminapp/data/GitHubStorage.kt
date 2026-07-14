package com.example.slacklineadminapp.data

import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Persists GitHub account credentials (alias → username + PAT) to disk
 * in the same directory as the rest of the app's admin data.
 */
object GitHubStorage {

    private fun accountsFile(): File =
        AppStorage.githubAccountsFile()

    // ── Account CRUD ──────────────────────────────────────────────────────

    fun loadAccounts(): Map<String, GitHubAccount> =
        AppStorage.loadJson(
            accountsFile(),
            emptyMap<String, GitHubAccount>()
        )

    fun saveAccount(alias: String, account: GitHubAccount) {
        val all = loadAccounts().toMutableMap()
        all[alias] = account
        AppStorage.saveJson(accountsFile(), all)
    }

    fun deleteAccount(alias: String) {
        val all = loadAccounts().toMutableMap()
        all.remove(alias)
        AppStorage.saveJson(accountsFile(), all)
    }
}

data class GitHubAccount(
    val username: String = "",
    val token: String    = ""
)
