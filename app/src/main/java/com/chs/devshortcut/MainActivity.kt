package com.chs.devshortcut

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.chs.devshortcut.ui.theme.DevShortcutTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevShortcutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipData = ClipData.newPlainText("label", "text")

    var hasPermission by remember { mutableStateOf(Util.hasPermission(context)) }
    var adbEnabled by remember { mutableStateOf(Util.isEnabled(context)) }

    LifecycleResumeEffect(Unit) {
        hasPermission = Util.hasPermission(context)
        adbEnabled = Util.isEnabled(context)
        DevWidgetProvider.updateAll(context)
        onPauseOrDispose { }
    }

    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                adbEnabled = Util.isEnabled(context)
                DevWidgetProvider.updateAll(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    Scaffold { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("USB 디버깅 토글", style = MaterialTheme.typography.headlineSmall)

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("USB 디버깅", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (adbEnabled) "켜짐" else "꺼짐",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = adbEnabled,
                        enabled = hasPermission,
                        onCheckedChange = { checked ->
                            if (Util.setEnabled(context, checked)) {
                                adbEnabled = Util.isEnabled(context)
                                DevWidgetProvider.updateAll(context)
                            } else {
                                Toast.makeText(context, R.string.toast_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            PermissionCard(
                hasPermission = hasPermission,
                command = Util.grantCommand(context),
                onCopy = {
                        scope.launch{
                            clipboard.setClipEntry(clipData.toClipEntry())
                        }
                    Toast.makeText(context, "복사됨", Toast.LENGTH_SHORT).show()
                }
            )

            Button(
                onClick = { requestPinWidget(context) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("홈 화면에 위젯 추가") }
        }
    }
}

@Composable
private fun PermissionCard(hasPermission: Boolean, command: String, onCopy: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (hasPermission) "WRITE_SECURE_SETTINGS 권한: 허용됨 ✓"
                else "WRITE_SECURE_SETTINGS 권한: 필요",
                style = MaterialTheme.typography.titleMedium
            )
            if (!hasPermission) {
                Text(
                    "이 권한은 앱에서 요청할 수 없습니다. USB 디버깅이 켜진 상태에서 PC에 연결한 뒤 " +
                            "아래 명령을 1회 실행하세요. 이후에는 디버깅을 꺼도 권한이 유지됩니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SelectionContainer {
                Text(command, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onCopy) { Text("명령어 복사") }
        }
    }
}

fun requestPinWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(
            ComponentName(context, DevWidgetProvider::class.java), null, null
        )
    } else {
        Toast.makeText(
            context, "런처가 지원하지 않습니다. 홈 화면을 길게 눌러 위젯을 직접 추가하세요.",
            Toast.LENGTH_LONG
        ).show()
    }
}
