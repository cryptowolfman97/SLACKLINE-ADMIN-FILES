package com.example.slacklineadminapp.navigation

object Routes {
    const val DASHBOARD           = "dashboard"
    const val LOCK_SCREEN         = "lock_screen"
    const val LEGACY_LICENSE      = "legacy_license_manager"
    const val ADD_PRODUCT         = "add_product"
    const val PRODUCT_MANAGER     = "product_manager/{productId}"
    const val NEW_LICENSE         = "new_license_manager"
    const val NEW_PRODUCT_MANAGER = "new_product_manager/{productId}"
    const val LICENSE_TOOLS       = "license_tools"
    const val SETTINGS            = "settings"
    const val CLOUD_SETTINGS      = "cloud_settings"
    const val UNIVERSAL_BACKUP    = "universal_backup"
    const val MAIN_BACKUP         = "main_backup"
    const val APP_BACKUP          = "app_backup"
    const val RECYCLE_BIN         = "recycle_bin"
    const val GITHUB_MANAGER      = "github_manager"
    const val SUPABASE_ADMIN      = "supabase_admin"
    const val ACTIVITY_LOG        = "activity_log"
    const val CUSTOMERS           = "customers"
    const val WORKFLOW_GUIDE      = "workflow_guide"
    const val WEBSITES            = "websites"
    const val DOCUMENTS           = "documents"
    const val APP_BLUEPRINTS      = "app_blueprints"
    const val KOTLIN_LICENSE_TOOL = "kotlin_license_tool"
    const val INVOICE_MAKER           = "invoice_maker"
    const val COMPANY_WEBSITE_ADMIN   = "company_website_admin"
    const val DOMAIN_MANAGEMENT       = "domain_management_screen"
    const val PROJECT_MANAGEMENT      = "project_management"
    const val BUSINESS_CALENDAR       = "business_calendar"
    const val KOTLIN_APP_BLUEPRINTS = "kotlin_app_blueprints"

    // ── SHV Store Admin ───────────────────────────────────────────────────────
    const val SHV_STORE_ADMIN         = "shv_store_admin"

    fun productManager(id: String)    = "product_manager/$id"
    fun newProductManager(id: String) = "new_product_manager/$id"
}
