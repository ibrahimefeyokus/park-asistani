package com.ibrah.parkasistani.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ibrah.parkasistani.data.ParkRecord
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.nav.MapsIntents
import com.ibrah.parkasistani.park.ParkDeleter
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch

private val NOTE_HINTS = listOf("Kat ", "Sıra ", "Giriş yanı", "Sokak park")

@Composable
fun DetailScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ParkRepository.get(ctx) }
    var record by remember { mutableStateOf<ParkRecord?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    // Realme kamera açıkken etkinliği öldürebilir; yol kaydedilebilir durumda tutulur.
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var photoVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(id) {
        record = repo.get(id)
        if (note.isEmpty()) note = record?.note.orEmpty()
        loaded = true
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPhotoPath ?: return@rememberLauncherForActivityResult
        pendingPhotoPath = null
        scope.launch {
            val r = repo.get(id)
            if (!ok || r == null) { File(path).delete(); return@launch }
            r.photoPath?.let { old -> if (old != path) File(old).delete() }
            val updated = r.copy(photoPath = path)
            repo.update(updated); record = updated; photoVersion++
        }
    }

    fun takePhoto(r: ParkRecord) {
        val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "park_${r.id}_${System.currentTimeMillis()}.jpg")
        pendingPhotoPath = file.path
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        camera.launch(uri)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        val r = record
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Üst çubuk: geri ve sil
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                Text("Park detayı", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 12.dp))
                if (r != null) FilledTonalIconButton(
                    onClick = { confirmDelete = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Icon(Icons.Filled.DeleteOutline, "Sil") }
            }

            if (r == null) {
                if (loaded) Text("Bu kayıt artık yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            HeroCard(r, onNavigate = { MapsIntents.openWalking(ctx, r.lat!!, r.lng!!) })
            PhotoCard(r.photoPath, photoVersion, onTake = { takePhoto(r) })
            NoteCard(
                note = note,
                saved = r.note.orEmpty(),
                onChange = { note = it },
                onSave = {
                    scope.launch { val u = r.copy(note = note.trim().ifBlank { null }); repo.update(u); record = u }
                },
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        icon = { Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Kayıt silinsin mi?") },
        text = { Text("Bu park kaydı, notu ve fotoğrafı silinecek. Geri alınamaz.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch { ParkDeleter.get(ctx).delete(id); onBack() }
            }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } },
    )
}

@Composable
private fun HeroCard(r: ParkRecord, onNavigate: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ParkBadge(56.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(formatDay(r.savedAt).uppercase(Locale.forLanguageTag("tr-TR")),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(formatClock(r.savedAt), style = MaterialTheme.typography.displaySmall)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(if (r.source == ParkSource.AUTO) Icons.Filled.Bluetooth else Icons.Filled.TouchApp,
                    if (r.source == ParkSource.AUTO) "Otomatik kayıt" else "Elle kayıt")
                InfoChip(if (r.hasLocation) Icons.Filled.GpsFixed else Icons.Filled.GpsOff, r.locationLabel())
                InfoChip(Icons.Filled.Schedule, formatAgo(r.savedAt))
            }
            Button(
                onClick = onNavigate, enabled = r.hasLocation, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null); Spacer(Modifier.width(8.dp))
                Text(if (r.hasLocation) "Arabama götür" else "Konum kaydedilemedi", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun PhotoCard(path: String?, version: Int, onTake: () -> Unit) {
    val bmp = remember(path, version) {
        path?.let { BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = 4 }) }
    }
    if (bmp == null) {
        // Boş durum: büyük, davetkâr bir alan
        Surface(
            onClick = onTake,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(vertical = 28.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconBadge(Icons.Filled.AddAPhoto, MaterialTheme.colorScheme.surfaceContainerLowest,
                    MaterialTheme.colorScheme.primary, size = 56.dp)
                Text("Otopark fotoğrafı ekle", style = MaterialTheme.typography.titleMedium)
                Text("Kat tabelası ya da sütun numarası işini kolaylaştırır.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)) {
        Image(bmp.asImageBitmap(), "Park fotoğrafı",
            Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentScale = ContentScale.Crop)
        FilledTonalButton(
            onClick = onTake,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) { Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Değiştir") }
    }
}

@Composable
private fun NoteCard(note: String, saved: String, onChange: (String) -> Unit, onSave: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.StickyNote2, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Not", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = note, onValueChange = onChange,
                placeholder = { Text("ör. B2 katı, 14 numaralı sütun") },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (note.isBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NOTE_HINTS.forEach { h -> SuggestionChip(onClick = { onChange(h) }, label = { Text(h.trim()) }) }
                }
            }
            AnimatedVisibility(note.trim() != saved) {
                Button(onClick = onSave, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Text("Notu kaydet")
                }
            }
        }
    }
}
