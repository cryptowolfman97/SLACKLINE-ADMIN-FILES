package com.shvertex.universalconv

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.shvertex.universalconv.shvgate.LicenseGateScreen

class SHVApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(SHVGateCallbacks())
    }

    inner class SHVGateCallbacks : ActivityLifecycleCallbacks {

        private val gatedActivities = mutableSetOf<String>()

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            val activityName = activity.javaClass.simpleName
            if (activityName !in gatedActivities) {
                gatedActivities.add(activityName)
                injectGate(activity)
            }
        }

        private fun injectGate(activity: Activity) {
            val accessGranted = mutableStateOf(false)
            val gateView = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    if (!accessGranted.value) {
                        LicenseGateScreen(
                            context = activity,
                            onAccessGranted = {
                                accessGranted.value = true
                                (parent as? android.view.ViewGroup)?.removeView(this@apply)
                            }
                        )
                    }
                }
            }
            // Set solid black immediately so no app content flashes before gate draws
            activity.window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
            (activity.window.decorView as android.view.ViewGroup).addView(
                gateView,
                android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            gatedActivities.remove(activity.javaClass.simpleName)
        }
    }
}