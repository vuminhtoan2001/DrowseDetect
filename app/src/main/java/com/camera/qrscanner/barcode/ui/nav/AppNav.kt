package com.camera.qrscanner.barcode.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.camera.qrscanner.barcode.R
import com.camera.qrscanner.barcode.ui.home.HomeScreen
import com.camera.qrscanner.barcode.ui.intro.IntroScreen
import com.camera.qrscanner.barcode.ui.permission.PermissionScreen
import com.camera.qrscanner.barcode.ui.setting.SettingScreen

object Dest {
    const val HOME = "home"
    const val SETTING = "setting"

    // Onboarding (first-open) — Language do SDK lo (Splash→Inter→Language→IAP), app chỉ còn Intro/Permission.
    const val ONB_INTRO = "onb_intro"
    const val ONB_PERMISSION = "onb_permission"
}

private data class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Dest.HOME, R.string.app_name, Icons.Filled.Home),
    Tab(Dest.SETTING, R.string.SETTINGS, Icons.Filled.Settings),
)

@Composable
fun AppNav(
    startDestination: String = Dest.HOME,
    onOnboardingDone: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in TABS.map { it.route }

    Scaffold(
        // Không tự thêm status-bar inset cho content; mỗi màn tự lo qua AppTopBar/statusBarsPadding
        // (tránh double inset). Bottom nav vẫn tự pad navigation-bar.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            // ----- Onboarding (first-open) -----
            composable(Dest.ONB_INTRO) {
                IntroScreen(
                    onGoPermission = { navController.navigate(Dest.ONB_PERMISSION) },
                    onGoMain = { finishOnboarding(navController, onOnboardingDone) },
                )
            }
            composable(Dest.ONB_PERMISSION) {
                PermissionScreen(onDone = { finishOnboarding(navController, onOnboardingDone) })
            }
            composable(Dest.HOME) {
                HomeScreen()
            }
            composable(Dest.SETTING) {
                SettingScreen()
            }
        }
    }
}

private fun finishOnboarding(
    navController: androidx.navigation.NavHostController,
    onDone: () -> Unit,
) {
    onDone()
    navController.navigate(Dest.HOME) {
        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}
