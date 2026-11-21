package com.ibrah.parkasistani.setup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object BatterySettings {
    /** Sırayla denenecek ekranlar; çözülebilen ilki açılır. */
    @SuppressLint("BatteryLife")
    fun intents(context: Context): List<Intent> {
        val pkg = Uri.parse("package:${context.packageName}")
        return listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg),
        )
    }

    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun open(context: Context) {
        val intent = intents(context).firstOrNull { it.resolveActivity(context.packageManager) != null } ?: return
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
