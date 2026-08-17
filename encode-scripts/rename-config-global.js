/**
 * rename-config-global.js — ⚙️ FILE #1/2 CẦN SỬA KHI CLONE APP MỚI.
 *
 * File này + `rename-config-key.js` là 2 file DUY NHẤT cần sửa khi copy bộ `encode-scripts/` này
 * sang 1 app khác. Mọi file `.js` còn lại (rename-file.js, rename-code-symbols.js,
 * rename-drawables.js, rename-languages.js, reverse.js, firebase-merge.js, safe-skip-base.js,
 * rename-all.js) là LÕI DÙNG CHUNG — không cần và không nên sửa.
 *
 * Xem quy trình đầy đủ + catalog bug đã gặp ở `docs/integration/16_ENCODE_R8_STANDARD.md`.
 */
module.exports = {
  // ── Bắt buộc sửa khi clone app mới ─────────────────────────────────────────────────────────────
  // RANDOM_SALT: đổi khác app cũ để hash ra tên khác nhau giữa các app (không bắt buộc kỹ thuật,
  // nhưng nên đổi để 2 app không vô tình trùng tên sau khi hash).
  // BASE_NAMESPACE: PHẢI khớp CHÍNH XÁC namespace thật trong app/build.gradle.kts (defaultConfig
  //   hoặc android.namespace). BUG ĐÃ GẶP THẬT (≥ 2 lần, xem 16_ENCODE_R8_STANDARD.md): file này bị
  //   copy nguyên từ app trước đó, quên đổi BASE_NAMESPACE → PROTECTED_PACKAGE_ROOT trong
  //   rename-file.js không khớp package gốc thật → cơ chế bảo vệ import R/BuildConfig + không đổi
  //   tên package gốc hoàn toàn MẤT TÁC DỤNG → "Unresolved reference" toàn bộ file dùng R/BuildConfig
  //   khi build release. LUÔN đối chiếu lại giá trị này với `namespace` trong build.gradle.kts trước
  //   khi chạy encode lần đầu cho 1 app.
  RANDOM_SALT: "com-video-smartdownload-downloadervideo-slt",
  HASH_LENGTH: 6,
  BASE_NAMESPACE: "com.video.smartdownload.downloadervideo",
  srcCodePath: "../app/src/main",

  // ── Tên riêng của app này cần bảo vệ THÊM (ngoài safe-skip-base.js dùng chung) ─────────────────
  // Thêm vào đây khi build R8 THẬT phát hiện 1 tên bị rename nhầm phá vỡ tham chiếu tới thư viện
  // ngoài/API hệ thống — không đoán trước được hết bằng regex/blacklist tĩnh, chỉ lộ ra khi build
  // release thật. KHÔNG xoá/sửa safe-skip-base.js, chỉ bổ sung ở đây. Ghi rõ lý do trong comment
  // (tên trùng gì, phát hiện lúc nào) để người sau không lỡ tay xoá.
  SAFE_SKIP_APP_SPECIFIC: [
    // ── Trùng từ khoá CSS/HTML nằm TRONG chuỗi JS chèn vào trang web ──────────────────────────────
    // App này chèn JS vào WebView (MediaOverlay/AppPromptRemover/CollapsedVideoFix). rename-code-symbols
    // thay tên theo TEXT TOÀN CỤC, không phân biệt code với chuỗi, nên tên hàm Kotlin trùng một từ khoá
    // CSS/HTML sẽ bị thay luôn ở BÊN TRONG chuỗi JS → JS/CSS sai âm thầm, build vẫn xanh, chỉ lộ khi
    // chạy bản đã encode trên máy thật.
    //
    // Cả 3 tên dưới đây đều đã gây lỗi THẬT ở vga58a (phát hiện khi test web Facebook):
    "absolute", // fun MediaResolver.absolute() — đập vào 'position:absolute' (MediaOverlay) → CSS không
    //             hợp lệ → nút tải rơi về position:static → HIỆN LỆCH VỊ TRÍ. Đây là triệu chứng gốc.
    "slot", //     fun AdsViewModel.slot() — đập vào '[slot="top-banner"]', '[slot="cta-button"]'
    //             (AppPromptRemover) → selector không khớp gì → dải quảng cáo TikTok không bị xoá.
    "download", // fun BrowserViewModel.download() — đập vào '[class*="download-app" i]' và
    //             '[data-e2e*="download-app" i]' → 2 selector đó chết. Lưu ý encoder coi `-` là RANH
    //             GIỚI TỪ nên "download-app" bị thay, còn "downloadApp" viết liền thì thoát.
    //
    // Cách tự kiểm sau mỗi lần encode (đừng chỉ đọc log — 3 lỗi trên không sinh log nào):
    //   grep -rnE "'[^']*(f|F|C|O)[0-9a-f]{5,7}[^']*'" app/src/main/java --include=*.kt
    // Hash nằm trong dấu nháy = đã đập vào nội dung chuỗi. Bỏ qua chỗ hash nằm trong `${...}`
    // (đó là nội suy, tức code, không phải nội dung chuỗi) và `const val TAG = "<tên class>"`.
  ],
  PROTECTED_FOLDER_NAMES_APP_SPECIFIC: [],
  PROTECTED_FOLDER_AND_FILE_APP_SPECIFIC: [],

  // ── Bật/tắt từng bước trong pipeline (dùng bởi rename-all.js) ───────────────────────────────────
  // rename-config-key: MẶC ĐỊNH TẮT — app này đọc Remote Config key ĐỘNG bằng ghép chuỗi
  // (vd `remote.getBoolean("${placement}_enable")`), rename-config-key.js chỉ tìm được chuỗi
  // LITERAL trong code nên sẽ hash key trong config.xml mà KHÔNG sửa được các chỗ gọi động tương
  // ứng trong code → lệch key → đọc default → tắt hết ads. Chỉ bật lại nếu đã refactor toàn bộ
  // truy cập config sang literal (xem `rename-config-key.js` đầu file để biết cách kiểm tra).
  pipeline: {
    renameDrawables: true,
    renameLanguages: true,
    renameFile: true,
    renameCodeSymbols: true,
    renameConfigKey: false,
  },

  // dùng cho node rename-drawables.js
  // đầu ra các file thuộc drawable (các image,gif,...)
  drawablesOutputFile: "keychangeDrawable.json",

  // dùng cho node rename-languages.js
  // đầu ra các file ngôn ngữ
  languagesOutputFile: "keychangeLanguage.json",

  // dùng cho node rename-config-key.js
  // đường dẫn đến file config firebase
  configFirebaseFile: "res/xml/config.xml",
  // đầu ra file firebase
  firebaseOutputFile: "keychangeConfigFirebase.json",

  // dùng cho node rename-code-symbols.js
  // đầu ra file đổi tên class,hàm và folder
  codeSymbolsOutputFile: "keychangeMap.json",

  // dùng cho node rename-file.js
  // đầu ra file đổi tên class,hàm và folder
  fileChangeOutputFile: "keychangeFile.json",
};
