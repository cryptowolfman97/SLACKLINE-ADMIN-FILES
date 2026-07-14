package com.example.omnicortex.data.models

import kotlinx.serialization.Serializable

// ── Severity levels ───────────────────────────────────────────────────────────
enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }

// ── Posture finding ───────────────────────────────────────────────────────────
data class PostureFinding(
    val id: String,
    val category: PostureCategory,
    val title: String,
    val detail: String,
    val severity: Severity,
    val passed: Boolean,
    val fixAdvice: String
)

enum class PostureCategory {
    LOCK_ACCESS, DEVELOPER_EXPOSURE, APP_ECOSYSTEM, OS_INTEGRITY, NETWORK_HYGIENE
}

// ── Persisted scan result (JSON serializable, no Room) ────────────────────────
@Serializable
data class ScanResult(
    val id: String,
    val type: String,
    val score: Int,
    val summary: String,
    val detailJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ── Breach record ─────────────────────────────────────────────────────────────
@Serializable
data class BreachRecord(
    val id: String,
    val watchItem: String,
    val breachName: String,
    val breachDate: String,
    val dataClasses: String,
    val isNew: Boolean = true,
    val discoveredAt: Long = System.currentTimeMillis()
)

// ── Network profile ───────────────────────────────────────────────────────────
@Serializable
data class NetworkProfile(
    val bssid: String,
    val ssid: String,
    val security: String,
    val frequency: Int,
    val trusted: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)

// ── App risk entry ────────────────────────────────────────────────────────────
data class AppRiskEntry(
    val packageName: String,
    val appName: String,
    val riskScore: Int,
    val riskFlags: List<RiskFlag>
)

data class RiskFlag(
    val label: String,
    val detail: String,
    val severity: Severity
)

// ── TLS result ────────────────────────────────────────────────────────────────
data class TlsResult(
    val domain: String,
    val isReachable: Boolean,
    val tlsVersion: String,
    val cipherSuite: String,
    val certSubject: String,
    val certIssuer: String,
    val certValidFrom: String,
    val certValidTo: String,
    val daysUntilExpiry: Int,
    val isExpired: Boolean,
    val isSelfSigned: Boolean,
    val hasHsts: Boolean,
    val grade: String,
    val findings: List<TlsFinding>
)

data class TlsFinding(
    val label: String,
    val detail: String,
    val severity: Severity,
    val passed: Boolean
)

// ── Shizuku Mode: per-app firewall rule (JSON serializable, no Room) ──────────
@Serializable
data class FirewallRule(
    val packageName: String,
    val uid: Int,
    val blocked: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
