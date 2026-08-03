package com.camera.studio.photoeditor.screenshot

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.camera.studio.photoeditor.ui.shared.filter.FilterPicker
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
 * docs/UI_FIDELITY_WORKFLOW.md. Ảnh xuất ra đối chiếu bằng mắt với
 * docs/example/screenshot/edit_image_filter.jpg (panel Filter, category "Popular").
 *
 * [FilterPicker] được chọn làm ví dụ đầu tiên vì là composable THUẦN (chỉ nhận state + callback qua
 * tham số, không tự gọi hiltViewModel()/Firebase/ads) — render được ngay trên JVM, không cần
 * MainActivity/Hilt/BaseApplication. Các màn hình thật (HomeScreen, CameraScreen...) đa số có
 * hiltViewModel()/ads nên KHÔNG render trực tiếp được theo cách này — xem giới hạn ở tài liệu quy trình.
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
class FilterPickerScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun filterPicker_popularCategory_noSelection() {
        composeTestRule.setContent {
            AppTheme {
                Box(Modifier.fillMaxWidth().background(Color.Black).padding(vertical = 16.dp)) {
                    FilterPicker(
                        selected = null,
                        onSelect = {},
                        previewBitmap = null,
                    )
                }
            }
        }

        composeTestRule.onRoot()
            .captureRoboImage("build/outputs/roborazzi/filter_picker_popular_no_selection.png")
    }
}
