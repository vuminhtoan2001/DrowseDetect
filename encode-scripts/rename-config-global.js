module.exports = {
  // chung — SALT + NAMESPACE RIÊNG vga52a (khác các app khác để hash khác nhau)
  // BUG ĐÃ GẶP: file này copy nguyên từ vga34b-kotlin, BASE_NAMESPACE để sót "com.camera.geostamp"
  // (namespace app CŨ) thay vì namespace thật của vga52a. Hậu quả: PROTECTED_PACKAGE_ROOT trong
  // rename-file.js không khớp package gốc thật ("com.camera.studio.photoeditor") → cơ chế bảo vệ
  // import R/BuildConfig + không đổi tên package gốc hoàn toàn KHÔNG có tác dụng (so sánh với
  // "geostamp" không bao giờ đúng) → file nằm ở package gốc (vd MyApplication.kt) bị đổi tên +
  // dời package, R/BuildConfig thành "Unresolved reference" vì AGP vẫn sinh R ở namespace thật
  // trong build.gradle.kts, không phải "com.camera.geostamp".
  RANDOM_SALT: "com-camera-studio-photoeditor-slt",
  HASH_LENGTH: 6,
  BASE_NAMESPACE: "com.camera.studio.photoeditor",
  srcCodePath: "app/src/main",

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
