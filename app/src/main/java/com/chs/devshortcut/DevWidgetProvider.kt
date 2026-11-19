package com.chs.devshortcut
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class DevWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = buildViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        val result = Util.toggle(context)
        val msg = when (result) {
            true -> R.string.toast_on
            false -> R.string.toast_off
            null -> R.string.toast_failed
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        updateAll(context)
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.usbdebugtoggle.ACTION_TOGGLE"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DevWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val hasPermission = Util.hasPermission(context)
            val enabled = Util.isEnabled(context)

            val (bg, label) = when {
                !hasPermission -> R.drawable.widget_bg_nop to R.string.state_no_permission
                enabled -> R.drawable.widget_bg_on to R.string.state_on
                else -> R.drawable.widget_bg_off to R.string.state_off
            }

            val pendingIntent = if (hasPermission) {
                PendingIntent.getBroadcast(
                    context, 0,
                    Intent(context, DevWidgetProvider::class.java).setAction(ACTION_TOGGLE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context, 1,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            return RemoteViews(context.packageName, R.layout.widget_adb).apply {
                setInt(R.id.widget_root, "setBackgroundResource", bg)
                setTextViewText(R.id.widget_label, context.getString(label))
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
        }
    }
}
