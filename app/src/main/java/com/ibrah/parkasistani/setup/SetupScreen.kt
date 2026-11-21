package com.ibrah.parkasistani.setup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private data class ItemText(val title: String, val why: String, val action: String)

private fun text(item: SetupItem) = when (item) {
    SetupItem.NOTIFICATIONS -> ItemText("Bildirimler", "Park kaydı ve süre uyarıları için.", "İzin ver")
    SetupItem.FINE_LOCATION -> ItemText("Konum", "Park yerini kaydetmek için.", "İzin ver")
    SetupItem.BACKGROUND_LOCATION -> ItemText(
        "Konum: her zaman", "Uygulama kapalıyken otomatik kayıt için. Açılan ekranda \"Her zaman izin ver\"i seç.", "Ayarı aç")
    SetupItem.BLUETOOTH -> ItemText("Yakındaki cihazlar", "Araç Bluetooth'unun koptuğunu algılamak için.", "İzin ver")
    SetupItem.EXACT_ALARM -> ItemText("Alarm ve hatırlatıcılar", "Süre uyarısının tam zamanında gelmesi için.", "Ayarı aç")
    SetupItem.BATTERY_UNRESTRICTED -> ItemText(
        "Pil kısıtlaması yok",
        "Realme arka plandaki uygulamaları kapatır. Açılan ekranda izin ver; ayrıca Uygulama bilgisi → Pil kullanımı → \"Arka planda çalışmaya izin ver\"i aç.",
        "Ayarı aç")
    SetupItem.CAR_SELECTED -> ItemText("Araç cihazı", "Hangi Bluetooth cihazının araban olduğunu seç.", "Seç")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onOpenCarSelection: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<SetupState?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(tick) { state = SetupStatus.read(ctx) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val pkg = Uri.parse("package:${ctx.packageName}")

    fun act(item: SetupItem) = when (item) {
        SetupItem.NOTIFICATIONS -> permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        SetupItem.FINE_LOCATION -> permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        SetupItem.BACKGROUND_LOCATION -> permLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        SetupItem.BLUETOOTH -> permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        SetupItem.EXACT_ALARM -> ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
        SetupItem.BATTERY_UNRESTRICTED -> BatterySettings.open(ctx)
        SetupItem.CAR_SELECTED -> onOpenCarSelection()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Kurulum") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
        )
    }) { pad ->
        val s = state ?: return@Scaffold
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            val ready = SetupStatus.autoParkReady(s)
            Text(
                if (ready) "Otomatik park kaydı hazır." else "Otomatik kaydın çalışması için eksikleri tamamla.",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp),
            )
            SetupItem.entries.forEach { item ->
                val ok = item in s.granted
                val t = text(item)
                // Arka plan konumu ancak ön plan konumu verildikten sonra istenebilir.
                val blocked = item == SetupItem.BACKGROUND_LOCATION && SetupItem.FINE_LOCATION !in s.granted
                ListItem(
                    leadingContent = {
                        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null,
                            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    },
                    headlineContent = { Text(t.title) },
                    supportingContent = { Text(t.why) },
                    trailingContent = {
                        if (!ok) OutlinedButton(onClick = { act(item) }, enabled = !blocked) { Text(t.action) }
                    },
                )
            }
            OutlinedButton(
                onClick = { ctx.startActivity(BatterySettings.appDetails(ctx)) },
                modifier = Modifier.padding(16.dp),
            ) { Text("Uygulama bilgisi (pil ayarı)") }
        }
    }
}

/** Ana ekranın üstündeki uyarı bandı. */
@Composable
fun SetupBanner(onOpenSetup: () -> Unit) {
    val ctx = LocalContext.current
    var ready by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(tick) { ready = SetupStatus.autoParkReady(SetupStatus.read(ctx)) }
    if (ready) return
    Card(
        onClick = onOpenSetup,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BluetoothDisabled, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Otomatik kayıt kapalı", style = MaterialTheme.typography.titleSmall)
                Text("Birkaç izin eksik, tamamlamak 1 dakika sürer.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}
