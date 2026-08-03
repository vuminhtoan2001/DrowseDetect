package com.camera.studio.photoeditor.screenshot

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.camera.studio.photoeditor.ui.components.ComingSoon
import com.camera.studio.photoeditor.ui.theme.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Bằng chứng pipeline Roborazzi chạy được, dùng làm mẫu quy trình bám sát screenshot gốc — xem
 * docs/UI_FIDELITY_WORKFLOW.md. App này hiện là bản "example lõi" (chỉ Intro/Permission/Home/
 * Setting/Language), nên dùng [ComingSoon] làm ví dụ vì là composable THUẦN (chỉ nhận tham số,
 * không tự gọi hiltViewModel()/Firebase/ads) — render được ngay trên JVM. Khi bắt đầu 1 tính năng
 * thật, viết test tương tự cho composable của tính năng đó và đối chiếu với ảnh gốc trong
 * docs-sample/screenshot/ (nếu có) hoặc docs/screenshot/ (ảnh mới của app đang làm).
 */
/**
 * [Config.application] = [Application] trần (KHÔNG phải `.MyApplication` thật của app) — bắt buộc
 * cho mọi test loại "chỉ render UI thuần": `.MyApplication` kế thừa BaseApplication của lib, tự init
 * Firebase/Ads/Hilt trong onCreate(), sẽ crash (`IllegalStateException` từ FirebaseApp) khi chạy trên
 * JVM/Robolectric vì thiếu bootstrap thật của Firebase/Play Services. Không liên quan gì tới đúng/sai
 * của composable đang test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-420dpi", application = Application::class)
class ComingSoonScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun comingSoon_rendersTitleAndPlaceholderText() {
        composeTestRule.setContent {
            AppTheme {
                ComingSoon("Camera")
            }
        }

        composeTestRule.onRoot()
            .captureRoboImage("build/outputs/roborazzi/coming_soon.png")
    }
}
