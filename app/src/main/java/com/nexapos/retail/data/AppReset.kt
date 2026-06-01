package com.nexapos.retail.data

import android.app.ActivityManager
import android.content.Context

/**
 * Wipes ALL application data (encrypted database, image files, PIN, backup
 * settings, business profile) and restarts the app from a clean slate. Used by
 * Settings → Danger zone → Delete business data.
 *
 * Implemented via [ActivityManager.clearApplicationUserData] which is the
 * Android-supported way to nuke the package's private storage in one shot.
 */
object AppReset {
    fun wipeAndRestart(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.clearApplicationUserData()
    }
}
