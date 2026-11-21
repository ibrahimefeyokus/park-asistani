package com.ibrah.parkasistani.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ibrah.parkasistani.data.ParkRecord
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.nav.MapsIntents
import com.ibrah.parkasistani.park.ParkSaver
import com.ibrah.parkasistani.park.SaveResult
import com.ibrah.parkasistani.timer.ActiveTimer
import com.ibrah.parkasistani.timer.ActiveTimerStore
import com.ibrah.parkasistani.timer.TimerScheduler
import com.ibrah.parkasistani.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    openTimerFor: Long?,
    onTimerHandled: () -> Unit,
    banner: @Composable () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ParkRepository.get(ctx) }
    val latest by repo.observeLatest().collectAsState(initial = null)
    val activeTimer by remember { ActiveTimerStore(ctx).observe() }.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var timerFor by remember { mutableStateOf<Long?>(null) }
    var saving by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(openTimerFor) {
        if (openTimerFor != null) { timerFor = openTimerFor; onTimerHandled() }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }

    fun saveNow() = scope.launch {
        saving = true
        val r = ParkSaver.get(ctx).save(ParkSource.MANUAL, null)
        saving = false
        WidgetUpdater.update(ctx)
        val msg = if (r is SaveResult.Saved && r.hasLocation) "Park yeri kaydedildi" else "Konum alınamadı, not ekleyebilirsin"
        snackbar.showSnackbar(msg)
    }

    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { saveNow() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Button(
                    onClick = {
                        val granted = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) saveNow() else locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    enabled = !saving,
                    shape = MaterialTheme.shapes.large,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text("Konum alınıyor…", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text("Park ettim", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onOpenHistory, onOpenSettings)
            banner()
            val r = latest
            if (r == null) EmptyState() else LastParkCard(
                r, now,
                onOpen = { onOpenDetail(r.id) },
                onNavigate = { MapsIntents.openWalking(ctx, r.lat!!, r.lng!!) },
                onStartTimer = { timerFor = r.id },
                showTimerButton = activeTimer?.takeIf { it.endAt > now } == null,
            )
            AnimatedVisibility(activeTimer?.let { it.endAt > now } == true, enter = fadeIn(), exit = fadeOut()) {
                activeTimer?.let { TimerCard(it, now, onCancel = { TimerScheduler(ctx).cancel() }) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    timerFor?.let { id ->
        TimerPicker(onDismiss = { timerFor = null }, onStart = { m ->
            timerFor = null
            scope.launch {
                val ok = TimerScheduler(ctx).start(id, m)
                snackbar.showSnackbar(if (ok) "Sayaç başladı: $m dk" else "Park kaydı bulunamadı")
            }
        })
    }
}

@Composable
private fun Header(onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Park Asistanı", style = MaterialTheme.typography.headlineMedium)
            Text("Araban nerede, hep bil.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, "Geçmiş") }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Ayarlar") }
    }
}

@Composable
private fun EmptyState() {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 36.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(Icons.Filled.DirectionsCar, MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary, size = 72.dp)
            Text("Henüz park kaydı yok", style = MaterialTheme.typography.titleLarge)
            Text(
                "Arabanın Bluetooth'u kopunca yerini kendim kaydederim. İstersen aşağıdan elle de kaydedebilirsin.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastParkCard(
    r: ParkRecord,
    now: Long,
    onOpen: () -> Unit,
    onNavigate: () -> Unit,
    onStartTimer: () -> Unit,
    showTimerButton: Boolean,
) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ParkBadge()
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("ARABAN BURADA", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(formatClock(r.savedAt), style = MaterialTheme.typography.displaySmall)
                }
                Text(formatAgo(r.savedAt, now), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(if (r.source == ParkSource.AUTO) Icons.Filled.Bluetooth else Icons.Filled.TouchApp, r.sourceLabel())
                InfoChip(if (r.hasLocation) Icons.Filled.GpsFixed else Icons.Filled.GpsOff, r.locationLabel())
                r.note?.takeIf { it.isNotBlank() }?.let { InfoChip(Icons.Filled.StickyNote2, it) }
            }
            Button(
                onClick = onNavigate, enabled = r.hasLocation,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null)
                Spacer(Modifier.width(8.dp))
                Text("Arabama götür", style = MaterialTheme.typography.titleSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val pad = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                if (showTimerButton) {
                    FilledTonalButton(onClick = onStartTimer, shape = MaterialTheme.shapes.medium,
                        contentPadding = pad, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.Timer, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text("Süre", maxLines = 1)
                    }
                }
                OutlinedButton(onClick = onOpen, shape = MaterialTheme.shapes.medium,
                    contentPadding = pad, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Filled.EditNote, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (r.note.isNullOrBlank() && r.photoPath == null) "Not ekle" else "Detay", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TimerCard(t: ActiveTimer, now: Long, onCancel: () -> Unit) {
    val left = ((t.endAt - now + 59_999) / 60_000).coerceAtLeast(0)
    val urgent = t.endAt - now <= 10 * 60_000L
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { t.remainingFraction(now) },
                    modifier = Modifier.size(64.dp), strokeWidth = 6.dp,
                    color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$left", style = MaterialTheme.typography.titleLarge)
                    Text("dk", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Park süresi", style = MaterialTheme.typography.titleMedium)
                Text("${formatClock(t.endAt)}'da bitiyor", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onCancel) { Text("Durdur") }
        }
    }
}
