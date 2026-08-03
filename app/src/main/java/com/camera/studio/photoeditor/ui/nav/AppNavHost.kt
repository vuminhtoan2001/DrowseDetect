package com.camera.studio.photoeditor.ui.nav

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camera.studio.photoeditor.advertisement.AdManager
import com.camera.studio.photoeditor.ui.components.ComingSoon
import com.camera.studio.photoeditor.ui.home.HomeScreen
import com.camera.studio.photoeditor.ui.onboarding.IntroScreen
import com.camera.studio.photoeditor.ui.onboarding.PermissionScreen
import com.camera.studio.photoeditor.ui.settings.SettingScreen

@Composable
fun AppNavHost(
    startRoute: String,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    fun goBack() { navController.popBackStack() }
    fun toHome() {
        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
    }
    /** Tap tile Home → inter_home → (native-inter fallback) → điều hướng thật (đúng 1 lần). */
    fun withInterHome(navigate: () -> Unit) {
        val activity = context as? Activity
        if (activity == null) { navigate(); return }
        AdManager.showInter(activity, "inter_home") { navigate() }
    }

    NavHost(navController = navController, startDestination = startRoute) {

        // ---------- Onboarding ----------
        composable(Screen.Intro.route) { IntroScreen(onFinish = { navController.navigate(Screen.Permission.route) { popUpTo(Screen.Intro.route) { inclusive = true } } }) }
        composable(Screen.Permission.route) { PermissionScreen(onDone = ::toHome) }

        // ---------- Hub ----------
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenCamera = { withInterHome { navController.navigate(Screen.Camera.route) } },
                onOpenSettings = { navController.navigate(Screen.Setting.route) },
                onOpenGallery = { withInterHome { navController.navigate(Screen.Gallery.route) } },
                onOpenItem = { _, _ -> },
                onOpenEditPhoto = { withInterHome { navController.navigate(Screen.EditPhotoPicker.route) } },
                onOpenCollageMaker = { withInterHome { navController.navigate(Screen.CollagePicker.route) } },
                onOpenWallpaper = { withInterHome { navController.navigate(Screen.WallpaperHome.route) } },
            )
        }

        // ---------- Màn tính năng — chưa dựng, placeholder ----------
        composable(Screen.Camera.route) { ComingSoon("Camera", onBack = ::goBack) }
        composable(Screen.Gallery.route) { ComingSoon("Gallery", onBack = ::goBack) }
        composable(Screen.EditPhotoPicker.route) { ComingSoon("Edit Photo", onBack = ::goBack) }
        composable(Screen.CollagePicker.route) { ComingSoon("Collage Maker", onBack = ::goBack) }
        composable(Screen.WallpaperHome.route) { ComingSoon("Wallpaper", onBack = ::goBack) }

        // ---------- Cài đặt ----------
        composable(Screen.Setting.route) {
            SettingScreen(onBack = ::goBack)
        }
    }
}
