package com.chs.devshortcut

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object Util {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    fun isDeveloperOptionsEnabled(context: Context): Boolean =
        Settings.Global.getInt(
            context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) == 1

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (!hasPermission(context)) return false
        val cr = context.contentResolver
        return try {
            if (enabled && !isDeveloperOptionsEnabled(context)) {
                Settings.Global.putInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
            }
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, if (enabled) 1 else 0)
        } catch (e: SecurityException) {
            false
        }
    }

    fun toggle(context: Context): Boolean? {
        val target = !isEnabled(context)
        return if (setEnabled(context, target)) target else null
    }

    fun grantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"
}
