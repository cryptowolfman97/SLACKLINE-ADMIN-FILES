package com.example.slacklineadminapp.data

import java.util.UUID

enum class ProductStatus { ACTIVE, WARNING, REVOKED }

data class KotlinProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val prefix: String,
    val appCode: String, // e.g., "my_app_pro"
    val publicKeyPem: String,
    val privateKeyPem: String,
    val createdAt: Long = System.currentTimeMillis(),
    var status: ProductStatus = ProductStatus.ACTIVE
)

data class KotlinLicense(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val licenseKey: String, // The final compressed Base64 block
    val deviceCode: String,
    val customerName: String,
    val customerEmail: String,
    val paymentMethod: String,
    val licenseType: String,
    val issuedAt: Long = System.currentTimeMillis(),
    var isRevoked: Boolean = false
)
