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
  RANDOM_SALT: "com-camera-studio-photoeditor-slt",
  HASH_LENGTH: 6,
  BASE_NAMESPACE: "com.camera.studio.photoeditor",
  srcCodePath: "app/src/main",

  // ── Tên riêng của app này cần bảo vệ THÊM (ngoài safe-skip-base.js dùng chung) ─────────────────
  // Thêm vào đây khi build R8 THẬT phát hiện 1 tên bị rename nhầm phá vỡ tham chiếu tới thư viện
  // ngoài/API hệ thống — không đoán trước được hết bằng regex/blacklist tĩnh, chỉ lộ ra khi build
  // release thật. KHÔNG xoá/sửa safe-skip-base.js, chỉ bổ sung ở đây. Ghi rõ lý do trong comment
  // (tên trùng gì, phát hiện lúc nào) để người sau không lỡ tay xoá.
  SAFE_SKIP_APP_SPECIFIC: [
    // (hiện chưa có mục nào riêng của vga52a-kotlin ngoài safe-skip-base.js — mọi tên từng gây bug
    // thật của app này đã đủ phổ biến để đưa thẳng vào safe-skip-base.js dùng chung, xem file đó.)
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
