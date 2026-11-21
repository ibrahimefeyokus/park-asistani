package com.ibrah.parkasistani.nav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object MapsIntents {
    private fun coord(lat: Double, lng: Double) = String.format(Locale.US, "%.6f,%.6f", lat, lng)
        .replace(Regex("0+(?=,|$)"), "").replace(".,", ",").removeSuffix(".")

    fun walkingUri(lat: Double, lng: Double) = "google.navigation:q=${coord(lat, lng)}&mode=w"

    fun webUri(lat: Double, lng: Double) =
        "https://www.google.com/maps/dir/?api=1&destination=${coord(lat, lng)}&travelmode=walking"

    fun openWalking(context: Context, lat: Double, lng: Double) {
        val nav = Intent(Intent.ACTION_VIEW, Uri.parse(walkingUri(lat, lng)))
            .setPackage("com.google.android.apps.maps")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(nav)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri(lat, lng))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
