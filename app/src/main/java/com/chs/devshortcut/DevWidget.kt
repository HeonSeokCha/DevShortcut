package com.chs.devshortcut

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DevWidget : GlanceAppWidget() {

    companion object {
        private val updateMutex = Mutex()
        suspend fun updateAll(context: Context) = withContext(Dispatchers.Main) {
            updateMutex.withLock { updateAllLocked(context) }
        }

        private suspend fun updateAllLocked(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val glanceManager = GlanceAppWidgetManager(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DevWidgetProvider::class.java))
            ids.forEach { id ->
                val views = DevWidget().compose(context, glanceManager.getGlanceIdBy(id))
                manager.updateAppWidget(id, views)
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            DevWidgetContent(
                hasPermission = Util.hasPermission(context),
                enabled = Util.isEnabled(context)
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun DevWidgetContent(hasPermission: Boolean, enabled: Boolean) {
    val context = LocalContext.current
    val (bg, label) = when {
        !hasPermission -> Color(0xFFC62828) to R.string.state_no_permission
        enabled -> Color(0xFF2E7D32) to R.string.state_on
        else -> Color(0xFF616161) to R.string.state_off
    }

    val action = if (hasPermission) {
        actionSendBroadcast(
            Intent(context, DevWidgetProvider::class.java).setAction(DevWidgetProvider.ACTION_TOGGLE)
        )
    } else {
        actionStartActivity<MainActivity>()
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .cornerRadius(20.dp)
            .padding(8.dp)
            .clickable(action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_usb),
            contentDescription = null,
            modifier = GlanceModifier.size(28.dp)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = context.getString(label),
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class DevWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.chs.devshortcut.ACTION_TOGGLE"
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle
    ) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        val msg = when (Util.toggle(context)) {
            true -> R.string.toast_on
            false -> R.string.toast_off
            null -> R.string.toast_failed
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        refresh(context)
    }

    private fun refresh(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                DevWidget.updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
