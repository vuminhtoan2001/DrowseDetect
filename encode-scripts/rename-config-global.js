module.exports = {
  // chung — SALT + NAMESPACE RIÊNG vga34b (khác các app khác để hash khác nhau)
  RANDOM_SALT: "com-camera-geostamp-slt",
  HASH_LENGTH: 6,
  BASE_NAMESPACE: "com.camera.geostamp",
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
