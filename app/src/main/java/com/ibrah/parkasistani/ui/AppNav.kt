package com.ibrah.parkasistani.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ibrah.parkasistani.setup.SetupBanner
import com.ibrah.parkasistani.setup.SetupScreen

@Composable
fun AppNav(openTimerFor: Long?, onTimerHandled: () -> Unit) {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    // İlk açılışta kurulum ekranını bir kez göster.
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("app", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("setupShown", false)) {
            prefs.edit().putBoolean("setupShown", true).apply()
            nav.navigate("setup")
        }
    }

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                openTimerFor = openTimerFor,
                onTimerHandled = onTimerHandled,
                banner = { SetupBanner(onOpenSetup = { nav.navigate("setup") }) },
                onOpenDetail = { nav.navigate("detail/$it") },
                onOpenHistory = { nav.navigate("history") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            DetailScreen(it.arguments!!.getLong("id"), onBack = { nav.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onOpen = { nav.navigate("detail/$it") }, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onOpenSetup = { nav.navigate("setup") }, onBack = { nav.popBackStack() })
        }
        composable("setup") {
            SetupScreen(onOpenCarSelection = { nav.navigate("settings") }, onBack = { nav.popBackStack() })
        }
    }
}
