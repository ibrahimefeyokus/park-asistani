package com.ibrah.parkasistani.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ibrah.parkasistani.car.BondedDevices
import com.ibrah.parkasistani.car.CarDevicePrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenSetup: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { CarDevicePrefs(ctx) }
    var refresh by remember { mutableIntStateOf(0) }
    val bonded = remember(refresh) { BondedDevices.list(ctx) }
    val selected by prefs.selected.collectAsState(initial = emptySet())
    val btPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    LaunchedEffect(refresh) { prefs.selectedOnce() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ayarlar") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            Text("Araç Bluetooth cihazları", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            Text(
                "Seçili cihazın bağlantısı kopunca park yeri otomatik kaydedilir.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (!BondedDevices.hasPermission(ctx)) {
                Button(onClick = { btPerm.launch(Manifest.permission.BLUETOOTH_CONNECT) }, modifier = Modifier.padding(16.dp)) {
                    Text("Bluetooth cihazlarını göster")
                }
            }
            bonded.forEach { d ->
                val checked = selected.any { it.equals(d.address, ignoreCase = true) }
                ListItem(
                    headlineContent = { Text(d.name ?: d.address) },
                    supportingContent = { Text(d.address) },
                    trailingContent = {
                        Checkbox(checked = checked, onCheckedChange = { on ->
                            scope.launch {
                                val next = if (on) selected + d.address else selected.filterNot { it.equals(d.address, true) }.toSet()
                                prefs.setSelected(next)
                            }
                        })
                    },
                )
            }
            OutlinedButton(onClick = onOpenSetup, modifier = Modifier.padding(16.dp)) { Text("İzinler ve pil ayarı") }
        }
    }
}
