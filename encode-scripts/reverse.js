const fs = require("fs");
const path = require("path");
const config = require("./rename-config-global");
const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);

/** Đọc JSON an toàn */
function readJsonSafe(filePath) {
  if (!fs.existsSync(filePath)) return {};
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf-8"));
  } catch (e) {
    console.error("Lỗi đọc JSON:", filePath, e.message);
    return {};
  }
}

/** Đảo key-value */
function reverseMap(map) {
  const out = {};
  for (const [k, v] of Object.entries(map || {})) {
    out[v] = k;
  }
  return out;
}

/** Gộp nhiều map */
function mergeAll(...maps) {
  return Object.assign({}, ...maps);
}

/** Revert text trong file */
function revertInFile(filePath, replacements) {
  let text = fs.readFileSync(filePath, "utf-8");
  for (const [from, to] of Object.entries(replacements)) {
    const re = new RegExp(`\\b${from}\\b`, "g");
    text = text.replace(re, to);
  }
  fs.writeFileSync(filePath, text, "utf-8");
}

/** Duyệt thư mục đệ quy */
function walk(dir, filterExt = [".kt", ".java", ".xml"]) {
  let results = [];
  for (const file of fs.readdirSync(dir)) {
    const full = path.join(dir, file);
    const stat = fs.statSync(full);
    if (stat.isDirectory()) {
      results = results.concat(walk(full, filterExt));
    } else if (filterExt.some((ext) => file.endsWith(ext))) {
      results.push(full);
    }
  }
  return results;
}

function renameFilesByMapping(rootDir, replacements) {
  const entries = fs.readdirSync(rootDir, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(rootDir, entry.name);

    if (entry.isDirectory()) {
      // Đệ quy nếu là folder
      renameFilesByMapping(fullPath, replacements);
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name);
      const base = path.basename(entry.name, ext);

      // Nếu base name nằm trong mapping
      if (replacements[base]) {
        const newName = replacements[base] + ext;
        const newPath = path.join(rootDir, newName);

        // Tránh rename trùng tên chính nó
        if (newName !== entry.name) {
          try {
            fs.renameSync(fullPath, newPath);
            console.log(`🔄 Đổi tên file: ${entry.name} → ${newName}`);
          } catch (err) {
            console.error(`❌ Lỗi khi đổi tên ${entry.name}:`, err.message);
          }
        }
      }
    }
  }
}

function renameFoldersByMapping(rootDir, replacements) {
  const entries = fs.readdirSync(rootDir, { withFileTypes: true });

  // Đầu tiên, duyệt các thư mục con (đệ quy trước)
  for (const entry of entries) {
    const fullPath = path.join(rootDir, entry.name);
    if (entry.isDirectory()) {
      renameFoldersByMapping(fullPath, replacements);
    }
  }

  // Sau đó, rename các folder con (bottom-up)
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const oldFolder = entry.name;
      const newFolder = replacements[oldFolder];
      if (newFolder && newFolder !== oldFolder) {
        const oldPath = path.join(rootDir, oldFolder);
        const newPath = path.join(rootDir, newFolder);
        try {
          fs.renameSync(oldPath, newPath);
          console.log(`📁 Đổi tên folder: ${oldFolder} → ${newFolder}`);
        } catch (err) {
          console.error(`❌ Lỗi khi đổi tên folder ${oldFolder}:`, err.message);
        }
      }
    }
  }
}

/** Main */
function revertAll({
  rootDir,
  keychangeFile,
  iconReplaceFile,
  stringReplaceFile,
  keyReplaceFile,
  codeReplaceFile,
}) {
  const mainMap = readJsonSafe(keychangeFile);
  const iconMap = readJsonSafe(iconReplaceFile);
  const stringMap = readJsonSafe(stringReplaceFile);
  const keyReplaceMap = readJsonSafe(keyReplaceFile);
  const codeReplaceMap = readJsonSafe(codeReplaceFile);

  // Đảo toàn bộ
  const folderMappingsReverse = reverseMap(mainMap.folders);
  const fileMappingsReverse = reverseMap(mainMap.files);
  const packageMappingsReverse = reverseMap(mainMap.packages);
  const iconMappingsReverse = reverseMap(iconMap);
  const stringMappingsReverse = reverseMap(stringMap);

  // Gộp chung để xử lý trong file code
  const allReplacements = mergeAll(
    folderMappingsReverse,
    fileMappingsReverse,
    packageMappingsReverse,
    iconMappingsReverse,
    stringMappingsReverse,
    reverseMap(keyReplaceMap),
    reverseMap(codeReplaceMap.class),
    reverseMap(codeReplaceMap.object),
    reverseMap(codeReplaceMap.fun)
  );

  renameFilesByMapping(path.join(rootDir, "res"), iconMappingsReverse);
  renameFilesByMapping(rootDir, fileMappingsReverse);
  renameFoldersByMapping(rootDir, folderMappingsReverse);
  console.log("Tổng số key cần revert:", Object.keys(allReplacements).length);

  // // Duyệt toàn bộ project
  const files = walk(rootDir);
  for (const file of files) {
    revertInFile(file, allReplacements);
  }

  // console.log(`✅ Đã revert xong ${files.length} file.`);
}

// Ví dụ chạy:
revertAll({
  rootDir: ROOT_DIR,
  keychangeFile: config.fileChangeOutputFile,
  iconReplaceFile: config.drawablesOutputFile,
  stringReplaceFile: config.languagesOutputFile,
  keyReplaceFile: config.firebaseOutputFile,
  codeReplaceFile: config.codeSymbolsOutputFile,
});
