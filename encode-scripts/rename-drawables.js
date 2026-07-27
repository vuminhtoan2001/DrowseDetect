/**
 * rename-drawables.js
 */

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const glob = require("glob");
const config = require("./rename-config-global");

// ⚙️ Cấu hình
const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);
const RES_DIR = path.join(ROOT_DIR, "res");
const MAPPING_FILE = path.resolve(__dirname, config.drawablesOutputFile);
const RANDOM_SALT = config.RANDOM_SALT;
const HASH_LENGTH = config.HASH_LENGTH;
// Map lưu trữ tên cũ → tên mới
const keyMappings = new Map();

// 🔑 Sinh tên mới
function hashName(name) {
  let newKey = crypto
    .createHash("sha1")
    .update(name + RANDOM_SALT)
    .digest("hex")
    .substring(0, HASH_LENGTH);

  if (/^\d/.test(newKey)) newKey = "d" + newKey;
  return newKey;
}

// Đệ quy tìm tất cả file có phần mở rộng cần thiết
function findFilesRecursive(dir, exts) {
  const results = [];
  const items = fs.readdirSync(dir, { withFileTypes: true });
  for (const item of items) {
    const fullPath = path.join(dir, item.name);
    if (item.isDirectory()) {
      results.push(...findFilesRecursive(fullPath, exts));
    } else if (exts.includes(path.extname(item.name).toLowerCase())) {
      results.push(fullPath);
    }
  }
  return results;
}

// 📦 Đọc mapping cũ (nếu có)
function loadExistingMapping() {
  if (fs.existsSync(MAPPING_FILE)) {
    const data = JSON.parse(fs.readFileSync(MAPPING_FILE, "utf8"));
    for (const [oldK, newK] of Object.entries(data)) {
      keyMappings.set(oldK, newK);
    }
  }
}

// 💾 Ghi mapping mới
function saveMapping() {
  const obj = Object.fromEntries(keyMappings);
  fs.writeFileSync(MAPPING_FILE, JSON.stringify(obj, null, 2), "utf8");
  console.log(`✅ Đã lưu mapping vào ${MAPPING_FILE}`);
}

// 🧩 Đổi tên các file drawable
function renameDrawableFiles() {
  const drawableDirs = glob.sync(
    path.join(RES_DIR, "drawable*").replace(/\\/g, "/")
  );
  const exts = [
    ".png",
    ".jpg",
    ".jpeg",
    ".webp",
    ".gif",
    ".xml",
    ".svg",
    ".avif",
  ];

  let renamedCount = 0;

  for (const dir of drawableDirs) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
      const ext = path.extname(file).toLowerCase();
      const base = path.basename(file, ext);
      if (!exts.includes(ext)) continue;

      if (!keyMappings.has(base)) {
        keyMappings.set(base, hashName(base));
      }

      const newBase = keyMappings.get(base);
      const oldPath = path.join(dir, file);
      const newPath = path.join(dir, `${newBase}${ext}`);

      if (oldPath !== newPath) {
        fs.renameSync(oldPath, newPath);
        renamedCount++;
        console.log(`🔄 ${file} → ${newBase}${ext}`);
      }
    }
  }

  console.log(`✅ Đã đổi tên ${renamedCount} file drawable`);
  saveMapping();
}

// 🔄 Cập nhật references trong code + XML
function updateReferences() {
  console.log("🔍 Updating drawable references...");

  // Tìm toàn bộ XML và manifest
  const xmlFiles = [
    ...glob.sync(path.join(ROOT_DIR, "res/**/*.xml").replace(/\\/g, "/")),
    path.join(ROOT_DIR, "AndroidManifest.xml"),
  ].filter(fs.existsSync);

  // Tìm toàn bộ file code (.java, .kt)
  const codeFiles = findFilesRecursive(ROOT_DIR, [".java", ".kt"]);

  const allFiles = [...xmlFiles, ...codeFiles];
  const entries = Object.entries(Object.fromEntries(keyMappings));

  for (const filePath of allFiles) {
    let content = fs.readFileSync(filePath, "utf8");
    let updated = false;

    for (const [oldKey, newKey] of entries) {
      // @drawable/oldKey trong XML
      const xmlRegex = new RegExp(`@drawable/${oldKey}\\b`, "g");
      // R.drawable.oldKey trong code
      const codeRegex = new RegExp(`R\\.drawable\\.${oldKey}\\b`, "g");

      if (xmlRegex.test(content) || codeRegex.test(content)) {
        content = content.replace(xmlRegex, `@drawable/${newKey}`);
        content = content.replace(codeRegex, `R.drawable.${newKey}`);
        updated = true;
      }
    }

    if (updated) {
      fs.writeFileSync(filePath, content, "utf8");
      console.log(`✅ Updated references in ${filePath}`);
    }
  }

  console.log("✨ All drawable references updated successfully!");
}

// 🚀 Main process
(function main() {
  console.log("🚀 Bắt đầu rename drawable...");
  loadExistingMapping();
  renameDrawableFiles();
  updateReferences();
  console.log("🎉 Hoàn tất rename drawable!");
})();
