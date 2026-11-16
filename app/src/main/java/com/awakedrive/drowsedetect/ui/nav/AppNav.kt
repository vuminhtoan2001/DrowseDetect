package com.awakedrive.drowsedetect.ui.nav

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
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.platform.TrackScreen
import com.awakedrive.drowsedetect.ui.home.HomeScreen
import com.awakedrive.drowsedetect.ui.intro.IntroScreen
import com.awakedrive.drowsedetect.ui.permission.PermissionScreen
import com.awakedrive.drowsedetect.ui.setting.SettingScreen

/**
 * Route Navigation dùng LUÔN tên màn Firebase `AD_<PascalCase>` (SCREEN_TRACKING.md §2). Giữ tên hằng
 * Kotlin, chỉ đổi giá trị chuỗi. Hằng ở nhóm "chỉ là tên màn" không phải route.
 */
object Dest {
    const val HOME = "AD_Home"
    const val SETTING = "AD_Setting"

    // Onboarding (first-open): Language custom (MyLanguageActivity) → Intro/Permission.
    const val ONB_INTRO = "AD_Intro"            // pager: log theo slide AD_Intro1..4, route tổng không log
    const val ONB_PERMISSION = "AD_Permission"

    // Chỉ là tên màn Firebase (không phải route)
    const val FIRST_LANGUAGE = "AD_First_Language"
    const val FIRST_LANGUAGE_SELECT = "AD_First_LanguageSelect"
    const val LANGUAGE = "AD_Language"
    const val LANGUAGE_SELECT = "AD_LanguageSelect"

    // Màn của lib (Activity riêng)
    const val SPLASH = "AD_Splash"
    const val FIRST_NOTI_PERMISSION = "AD_First_NotiPermission"
    const val NOTI_PERMISSION = "AD_NotiPermission"
    const val FIRST_IAP = "AD_First_Iap"
    const val IAP = "AD_Iap"
}

private data class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Dest.HOME, R.string.tab_home, Icons.Filled.Home),
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
                // IntroScreen (pager) tự TrackScreen theo slide — route tổng không log
                IntroScreen(
                    onGoPermission = { navController.navigate(Dest.ONB_PERMISSION) },
                    onGoMain = { finishOnboarding(navController, onOnboardingDone) },
                )
            }
            composable(Dest.ONB_PERMISSION) {
                // PermissionScreen tự gọi TrackScreen + LogOnboardingExit
                PermissionScreen(onDone = { finishOnboarding(navController, onOnboardingDone) })
            }
            composable(Dest.HOME) {
                TrackScreen(Dest.HOME)
                HomeScreen()
            }
            composable(Dest.SETTING) {
                TrackScreen(Dest.SETTING)
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
