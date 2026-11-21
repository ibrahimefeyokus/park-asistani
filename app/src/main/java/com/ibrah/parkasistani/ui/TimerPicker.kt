package com.ibrah.parkasistani.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val PRESETS = listOf(15, 30, 60, 120)

@Composable
fun TimerPicker(onDismiss: () -> Unit, onStart: (minutes: Int) -> Unit) {
    var selected by remember { mutableStateOf<Int?>(60) }
    var custom by remember { mutableStateOf("") }
    val minutes = custom.toIntOrNull()?.takeIf { it in 1..24 * 60 } ?: selected

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("Park süresi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESETS.forEach { m ->
                        FilterChip(
                            selected = custom.isEmpty() && selected == m,
                            onClick = { selected = m; custom = "" },
                            label = { Text(if (m < 60) "$m dk" else "${m / 60} sa") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { v -> custom = v.filter(Char::isDigit).take(4) },
                    label = { Text("Özel (dakika)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text("Bitimden 10 dk önce ve süre dolunca bildirim gelir.")
            }
        },
        confirmButton = {
            TextButton(enabled = minutes != null, onClick = { minutes?.let(onStart) }) { Text("Başlat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
