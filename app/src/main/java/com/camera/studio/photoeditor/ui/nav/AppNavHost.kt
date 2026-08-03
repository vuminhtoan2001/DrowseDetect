package com.camera.studio.photoeditor.ui.nav

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.camera.studio.photoeditor.advertisement.AdManager
import com.camera.studio.photoeditor.ui.camera.CameraScreen
import com.camera.studio.photoeditor.ui.collage.CollageMakerScreen
import com.camera.studio.photoeditor.ui.editphoto.EditPhotoScreen
import com.camera.studio.photoeditor.ui.gallery.GalleryScreen
import com.camera.studio.photoeditor.ui.home.HomeScreen
import com.camera.studio.photoeditor.ui.importphoto.ImportPhotoResultScreen
import com.camera.studio.photoeditor.ui.onboarding.IntroScreen
import com.camera.studio.photoeditor.ui.onboarding.PermissionScreen
import com.camera.studio.photoeditor.ui.settings.SettingScreen
import com.camera.studio.photoeditor.ui.shared.editor.MediaPickerScreen
import com.camera.studio.photoeditor.ui.success.TakePhotoSuccessfullyScreen
import com.camera.studio.photoeditor.ui.videoresult.VideoResultScreen
import com.camera.studio.photoeditor.ui.wallpaper.WallpaperEditorScreen
import com.camera.studio.photoeditor.ui.wallpaper.WallpaperTemplatesScreen

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
                onOpenItem = { path, isVideo -> navController.navigate(Screen.importPhotoResult(path, isVideo)) },
                onOpenEditPhoto = { withInterHome { navController.navigate(Screen.EditPhotoPicker.route) } },
                onOpenCollageMaker = { withInterHome { navController.navigate(Screen.CollagePicker.route) } },
                onOpenWallpaper = { withInterHome { navController.navigate(Screen.WallpaperHome.route) } },
            )
        }

        // ---------- Camera & kết quả ----------
        composable(Screen.Camera.route) {
            CameraScreen(
                onBack = ::goBack,
                onPhotoSaved = {
                    navController.navigate(Screen.TakePhotoSuccessfully.route) { popUpTo(Screen.Home.route) }
                },
                onVideoRecorded = { videoUri, previewWidthDp ->
                    navController.navigate(Screen.videoResult(videoUri, previewWidthDp))
                },
                onOpenLatest = { path, isVideo -> navController.navigate(Screen.importPhotoResult(path, isVideo)) },
            )
        }
        composable(
            route = "${Screen.VideoResult.route}/{${Screen.ARG_VIDEO_URI}}/{${Screen.ARG_PREVIEW_WIDTH_DP}}",
            arguments = listOf(
                navArgument(Screen.ARG_VIDEO_URI) { type = NavType.StringType },
                navArgument(Screen.ARG_PREVIEW_WIDTH_DP) { type = NavType.StringType },
            ),
        ) { entry ->
            val videoUri = Uri.decode(entry.arguments?.getString(Screen.ARG_VIDEO_URI) ?: "")
            val previewWidthDp = entry.arguments?.getString(Screen.ARG_PREVIEW_WIDTH_DP)?.toFloatOrNull() ?: 360f
            VideoResultScreen(
                videoUri = videoUri,
                previewWidthDp = previewWidthDp,
                onCancel = ::goBack,
                onSaved = {
                    navController.navigate(Screen.TakePhotoSuccessfully.route) { popUpTo(Screen.Home.route) }
                },
            )
        }
        composable(Screen.TakePhotoSuccessfully.route) { TakePhotoSuccessfullyScreen(onBackHome = ::toHome) }

        composable(
            route = "${Screen.ImportPhotoResult.route}/{${Screen.ARG_URI}}/{${Screen.ARG_IS_VIDEO}}",
            arguments = listOf(
                navArgument(Screen.ARG_URI) { type = NavType.StringType },
                navArgument(Screen.ARG_IS_VIDEO) { type = NavType.BoolType },
            ),
        ) { entry ->
            val path = Uri.decode(entry.arguments?.getString(Screen.ARG_URI) ?: "")
            val isVideo = entry.arguments?.getBoolean(Screen.ARG_IS_VIDEO) ?: false
            ImportPhotoResultScreen(
                path = path,
                isVideo = isVideo,
                onBack = ::goBack,
                onDeleted = ::goBack,
                onEdit = { p -> navController.navigate(Screen.editPhotoEditor(p)) },
            )
        }

        // ---------- Gallery ----------
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onBack = ::goBack,
                onOpenItem = { path, isVideo -> navController.navigate(Screen.importPhotoResult(path, isVideo)) },
            )
        }

        // ---------- Edit Photo (M4) ----------
        composable(Screen.EditPhotoPicker.route) {
            MediaPickerScreen(
                title = "Edit Photo",
                multiSelect = false,
                onBack = ::goBack,
                // KHÔNG popUpTo picker — giữ nguyên trong back stack để bấm "Close" ở Edit Photo
                // quay lại đúng màn chọn ảnh này (thay vì rơi thẳng về Home).
                onConfirm = { picked -> navController.navigate(Screen.editPhotoEditor(picked.first())) },
            )
        }
        composable(
            route = "${Screen.EditPhotoEditor.route}/{${Screen.ARG_SOURCE_PATH}}",
            arguments = listOf(navArgument(Screen.ARG_SOURCE_PATH) { type = NavType.StringType }),
        ) { entry ->
            val sourcePath = Uri.decode(entry.arguments?.getString(Screen.ARG_SOURCE_PATH) ?: "")
            EditPhotoScreen(
                sourcePath = sourcePath,
                onBack = ::goBack,
                onSaved = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
            )
        }

        // ---------- Collage Maker (M5) ----------
        composable(Screen.CollagePicker.route) {
            MediaPickerScreen(
                title = "Collage Maker",
                multiSelect = true,
                onBack = ::goBack,
                // Tương tự Edit Photo — không popUpTo picker, để "Close" ở Collage Maker quay lại
                // đúng màn chọn ảnh.
                onConfirm = { picked -> navController.navigate(Screen.collageEditor(picked)) },
            )
        }
        composable(
            route = "${Screen.CollageEditor.route}/{${Screen.ARG_PATHS}}",
            arguments = listOf(navArgument(Screen.ARG_PATHS) { type = NavType.StringType }),
        ) { entry ->
            val paths = Uri.decode(entry.arguments?.getString(Screen.ARG_PATHS) ?: "").split("|").filter { it.isNotBlank() }
            CollageMakerScreen(
                initialPaths = paths,
                onBack = ::goBack,
                onSaved = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
            )
        }

        // ---------- Wallpaper Creator (M6) ----------
        composable(Screen.WallpaperHome.route) {
            WallpaperTemplatesScreen(
                onBack = ::goBack,
                onOpenTemplate = { id -> navController.navigate(Screen.wallpaperEditor(id)) },
            )
        }
        composable(
            route = "${Screen.WallpaperEditor.route}/{${Screen.ARG_TEMPLATE_ID}}",
            arguments = listOf(navArgument(Screen.ARG_TEMPLATE_ID) { type = NavType.IntType }),
        ) { entry ->
            val templateId = entry.arguments?.getInt(Screen.ARG_TEMPLATE_ID) ?: 0
            WallpaperEditorScreen(
                templateId = templateId,
                onBack = ::goBack,
                onSaved = { },
            )
        }

        // ---------- Cài đặt ----------
        composable(Screen.Setting.route) {
            SettingScreen(onBack = ::goBack)
        }
    }
}
