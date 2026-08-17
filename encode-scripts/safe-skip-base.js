/**
 * safe-skip-base.js — LÕI DÙNG CHUNG, KHÔNG SỬA FILE NÀY.
 *
 * Danh sách tên hàm/class/segment-package/tên-folder KHÔNG BAO GIỜ được đổi tên (rename/obfuscate)
 * ở BẤT KỲ app nào trong dòng này (dùng chung base-application AAR + Hilt + kiến trúc Kotlin/Compose
 * clone qua nhiều app). Đây là phần "lõi" thật sự ổn định — khi clone sang app mới, KHÔNG cần sửa
 * file này.
 *
 * Khi build R8 thật phát hiện 1 tên bị rename nhầm (Unresolved reference / crash chỉ ở release) mà
 * NGUYÊN NHÂN LÀ ĐẶC THÙ CỦA 1 APP CỤ THỂ (vd app đó tự đặt tên hàm trùng 1 API riêng nào đó) —
 * KHÔNG thêm vào đây. Thêm vào `SAFE_SKIP_APP_SPECIFIC` / `PROTECTED_FOLDER_NAMES_APP_SPECIFIC` /
 * `PROTECTED_FOLDER_AND_FILE_APP_SPECIFIC` trong `rename-config-global.js` CỦA APP ĐÓ.
 *
 * Chỉ thêm vào file này khi tên đó THỰC SỰ chung cho mọi app (API/tên gọi của base-application AAR,
 * keyword ngôn ngữ, kiểu stdlib phổ biến, hoặc 1 lớp bug đã lặp lại ≥ 2 lần ở ≥ 2 app khác nhau).
 */

module.exports = {
  // Tên hàm/class/segment package không bao giờ rename, dùng ở cả rename-file.js (folder) lẫn
  // rename-code-symbols.js (class/object/fun).
  SAFE_SKIP_BASE: [
    // --- Entry-point / cấu trúc Android-Hilt bắt buộc phải giữ nguyên tên ---
    "main", "Application", "MyApplication", "BuildConfig", "R", "Manifest", "AppModule",

    // --- Segment package của base-application AAR (com.brian.*/com.nlbn.*) — đổi sẽ phá import,
    //     vd `com.brian.base_application.language` → rename nhầm thành `...f9eef09` ---
    "language", "device", "Device", "iap", "start", "base", "util", "utils", "ads", "callback",
    "nativead",

    // --- Segment gốc của mọi thư viện ngoài — nếu 1 extension-function bị parse sai receiver, các
    //     tên này KHÔNG BAO GIỜ được rename vì sẽ phá import trên toàn project ---
    "androidx", "java", "kotlin", "kotlinx",

    // --- Lifecycle/callback Android chuẩn (override từ SDK, đổi tên = "overrides nothing") ---
    "onCreate", "onResume", "onPause", "onStop", "onDestroy", "onNewToken", "onMessageReceived",
    "onReceive",

    // --- API lib ads/IAP/notification dùng chung (base-application AAR) — mọi app đều gọi đúng tên
    //     này, đổi sẽ phá lời gọi vào lib ---
    "loadAd", "showPrivacyOptionsForm", "getInstance", "init", "startForegroundService",
    "startForeground", "startService", "setEnabled", "isEnabled", "isPlaying", "setVolume",
    "startConnection", "retryConnection", "processPurchases", "launchBillingFlow",
    "handlePurchaseUpdates", "acknowledgePurchase", "createNotificationChannel", "cancelNotification",
    "cancelAllNotifications", "scheduleAlarm", "cancelAlarm", "doInBackground", "logAdRevenue",
    "updateLocale", "MainActivity", "AdActivity",

    // --- Kiểu/API Kotlin-Android stdlib phổ biến, dễ bị 1 hàm/biến tự đặt trùng tên (đã gây
    //     "Unresolved reference"/crash thật ở release ở ít nhất 1 app, xem catalog bug trong
    //     docs/integration/16_ENCODE_R8_STANDARD.md) ---
    "View", "ViewParent", "Int", "Currency", "String", "Context", "Type", "Preview", "PreviewView",
    "interface", "for", "findViewById", "isShowing", "show", "dismiss", "remove", "clear", "contains",
    "release", "destroy", "start", "hide", "resume", "stop", "pause", "register", "unregister",
    "fromJson", "insert", "save", "decode", "Product", "Error", "FileData", "BlendMode",

    // --- Tên màn hình lặp lại xuyên suốt các app cùng dòng template (onboarding/splash) ---
    "Splash", "Setting", "Intro", "Permission", "Language", "Uninstall", "Theme",

    // --- Class/hàm dựng sẵn Jetpack Compose — đặt tên trùng (data class/fun tự viết) đã gây bug
    //     thật ≥ 2 lần khác nhau (vd `data class Row` phá luôn mọi `Row(...)` composable hợp lệ,
    //     `data class SolidColor` phá `androidx.compose.ui.graphics.SolidColor`) — chặn trước cả
    //     nhóm tên hay bị đặt trùng nhất thay vì chờ từng app tự vấp lại ---
    "Row", "Column", "Box", "Card", "Icon", "Image", "Button", "Text", "Brush", "Color", "Shape",
    "Modifier", "SolidColor",
    // --- Tên hàm/biến phổ biến trong Compose, dễ bị đặt trùng (đã gây bug thật ≥ 2 lần) ---
    "onClick", "contentDescription", "contentScale", "painterResource", "stringResource",
    "remember", "mutableStateOf", "collectAsState", "observeAsState", "LaunchedEffect",
    "rememberCoroutineScope", "rememberLauncherForActivityResult", "LocalContext", "LocalLifecycleOwner",
    "LocalConfiguration", "LocalDensity", "LocalLayoutDirection", "LocalInspectionMode",
    "LocalView", "LocalFocusManager", "LocalSoftwareKeyboardController", "LocalUriHandler",
    "LocalClipboardManager", "LocalHapticFeedback", "LocalAccessibilityManager", "LocalTextInputService",
    "LocalTextToolbar", "LocalTextInputService", "LocalTextToolbar", "LocalTextInputService",
    "LocalTextToolbar", "LocalTextInputService", "LocalTextToolbar", "LocalTextInputService",
    // --- Tên hàm dùng hay bị biến đổi lỗi 
    "add", "remove", "update", "get", "set", "put", "post", "delete", "fetch", "load", "save",
    "start", "stop", "pause", "resume", "destroy", "create", "init", "build", "run", "execute",
    "process", "handle", "dispatch", "notify", "register", "unregister", "subscribe", "unsubscribe",
    "delete", "insert", "select", "query", "find", "search", "filter", "sort", "map", "reduce",
    "forEach", "onClick", "onChange", "onSubmit", "onError", "onSuccess", "onFailure", "onComplete",
    "onStart", "onStop", "onResume", "onPause", "onDestroy", "onCreate", "onViewCreated", "finish", "cancel", "reset", "clear", "refresh", "reload", "retry", "abort", "exit", "close",
    "open", "read", "write", "copy", "move", "rename", "compress", "decompress", "encrypt",
    "decrypt", "sign", "verify", "hash", "encode", "decode", "serialize", "deserialize",
    "validate", "sanitize", "format", "parse", "convert", "transform", "calculate", "compute",
    "measure", "draw", "render", "paint", "animate", "transition", "interpolate", "blend",
    "mix", "combine", "split", "join", "merge", "separate", "divide", "multiply", "add", "subtract",
    "compare", "equals", "hashCode", "toString", "valueOf", "getClass", "instanceof", "cast",
    "clone", "finalize", "wait", "notifyAll", "yield", "sleep", "join", "interrupt",
    "startThread", "stopThread", "runThread", "pauseThread", "resumeThread", "destroyThread",
    "createThread", "executeThread", "submitThread", "cancelThread", "resetThread", "clearThread",
    "refreshThread", "reloadThread", "retryThread", "abortThread", "exitThread", "closeThread",
    "Toast", "Snackbar", "Dialog", "AlertDialog", "ProgressDialog", "DatePickerDialog",
    "TimePickerDialog", "Notification", "NotificationManager", "NotificationChannel",
    "NotificationCompat", "NotificationBuilder", "NotificationAction", "NotificationStyle",
    "exists", "isEmpty", "isNull", "isNotNull", "isTrue", "isFalse", "isValid", "isInvalid",
    "text", "clearHistory", "saveState", "player", "openInBrowser",
  ],

  // Tên folder luôn được bảo vệ (không đổi tên folder, nhưng vẫn quét/đổi tên file BÊN TRONG) —
  // dùng ở rename-file.js. Đây là tên thư mục Android chuẩn (res/*) + vài tên kiến trúc chung.
  PROTECTED_FOLDER_NAMES_BASE: [
    "com", "res", "values", "layout", "drawable", "mipmap", "menu", "xml", "raw", "assets",
    "keystore", "Type", "Color", "ui", "theme",
  ],

  // Tên folder VÀ mọi file bên trong đều được bảo vệ tuyệt đối (không quét sâu vào bên trong) —
  // dùng ở rename-file.js.
  PROTECTED_FOLDER_AND_FILE_BASE: [
    "dao", "database", "core", "data", "Color", "Type", "Success", "Theme", "MainActivity",
    "AdActivity",
  ],
};
