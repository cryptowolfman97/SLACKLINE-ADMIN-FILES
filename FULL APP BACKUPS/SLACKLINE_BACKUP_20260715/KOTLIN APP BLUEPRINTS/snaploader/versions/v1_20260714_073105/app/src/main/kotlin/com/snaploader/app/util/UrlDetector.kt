package com.snaploader.app.util

import com.snaploader.app.model.Platform

object UrlDetector {

    fun detect(url: String): Platform {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> Platform.YOUTUBE
            lower.contains("tiktok.com") || lower.contains("vm.tiktok")  -> Platform.TIKTOK
            lower.contains("instagram.com")                               -> Platform.INSTAGRAM
            lower.contains("facebook.com") || lower.contains("fb.watch") -> Platform.FACEBOOK
            lower.contains("twitter.com") || lower.contains("x.com")     -> Platform.TWITTER
            else                                                           -> Platform.GENERAL
        }
    }

    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }

    fun extractVideoId(url: String, platform: Platform): String {
        return when (platform) {
            Platform.YOUTUBE -> {
                val patterns = listOf(
                    Regex("(?:v=|youtu\\.be/)([A-Za-z0-9_-]{11})"),
                    Regex("embed/([A-Za-z0-9_-]{11})")
                )
                patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.get(1) } ?: ""
            }
            Platform.TIKTOK -> {
                Regex("/video/(\\d+)").find(url)?.groupValues?.get(1) ?: ""
            }
            Platform.INSTAGRAM -> {
                Regex("/(?:p|reel|tv)/([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1) ?: ""
            }
            else -> ""
        }
    }
}