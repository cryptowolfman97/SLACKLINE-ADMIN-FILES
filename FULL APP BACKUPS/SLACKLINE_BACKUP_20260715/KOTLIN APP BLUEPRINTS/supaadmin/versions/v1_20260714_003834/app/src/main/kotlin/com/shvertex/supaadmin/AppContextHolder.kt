package com.shvertex.supaadmin

import android.content.Context

object AppContextHolder {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context = appContext ?: throw IllegalStateException("AppContextHolder not initialized")
}
