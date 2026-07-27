/**
 * rename-config-keys.js
 */

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { XMLParser, XMLBuilder } = require("fast-xml-parser");
const config = require("./rename-config-global");

const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);
const RES_XML_PATH = path.join(ROOT_DIR, config.configFirebaseFile);
const MAPPING_FILE = path.resolve(__dirname, config.firebaseOutputFile);
const RANDOM_SALT = config.RANDOM_SALT;
const HASH_LENGTH = config.HASH_LENGTH;

const keyMappings = new Map();

// 🔑 Sinh key mới
function hashName(key) {
  let newKey = crypto
    .createHash("sha1")
    .update(key + RANDOM_SALT)
    .digest("hex")
    .substring(0, HASH_LENGTH);

  if (/^\d/.test(newKey)) newKey = "k" + newKey;
  return newKey;
}

// 🔁 Đệ quy tìm file .java, .kt
function findFilesRecursive(dir, exts) {
  const results = [];
  const items = fs.readdirSync(dir, { withFileTypes: true });
  for (const item of items) {
    const fullPath = path.join(dir, item.name);
    if (item.isDirectory()) {
      results.push(...findFilesRecursive(fullPath, exts));
    } else if (exts.includes(path.extname(item.name))) {
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

// 💾 Ghi mapping
function saveMapping() {
  const obj = Object.fromEntries(keyMappings);
  fs.writeFileSync(MAPPING_FILE, JSON.stringify(obj, null, 2), "utf8");
  console.log(`✅ Đã lưu mapping vào ${MAPPING_FILE}`);
}

// 🧩 Xử lý file config.xml
function processConfigXml() {
  console.log(`🔧 Đang xử lý ${RES_XML_PATH}`);

  if (!fs.existsSync(RES_XML_PATH)) {
    console.error(`❌ Không tìm thấy file: ${RES_XML_PATH}`);
    process.exit(1);
  }

  const xmlText = fs.readFileSync(RES_XML_PATH, "utf8");
  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    parseTagValue: false,
    format: true,
  });
  const builder = new XMLBuilder({
    ignoreAttributes: false,
    format: true,
    indentBy: "    ",
    suppressEmptyNode: true,
  });

  const parsed = parser.parse(xmlText);

  if (!parsed.defaultsMap || !parsed.defaultsMap.entry) {
    console.error("⚠️ Không tìm thấy thẻ <entry> trong config.xml");
    return;
  }

  let entries = parsed.defaultsMap.entry;
  if (!Array.isArray(entries)) entries = [entries];

  for (const entry of entries) {
    const keyNode = entry.key;
    if (!keyNode) continue;
    const oldKey = typeof keyNode === "string" ? keyNode : keyNode["#text"];
    if (!oldKey) continue;

    if (!keyMappings.has(oldKey)) {
      keyMappings.set(oldKey, hashName(oldKey));
    }
    const newKey = keyMappings.get(oldKey);
    entry.key = newKey;
  }

  // Ghi file mới
  // fs.copyFileSync(RES_XML_PATH, RES_XML_PATH + ".bak");
  const newXml = builder.build(parsed);
  fs.writeFileSync(RES_XML_PATH, newXml.trim() + "\n", "utf8");
  console.log(`✅ Đã đổi key trong ${RES_XML_PATH}`);
  saveMapping();
}

// 🔍 Cập nhật references trong code
function updateReferences() {
  console.log("🔍 Updating config key references...");

  const codeFiles = findFilesRecursive(ROOT_DIR, [".java", ".kt"]);
  const entries = Object.entries(Object.fromEntries(keyMappings));

  for (const filePath of codeFiles) {
    let content = fs.readFileSync(filePath, "utf8");
    let updated = false;

    for (const [oldKey, newKey] of entries) {
      // Tìm tất cả "oldKey" hoặc 'oldKey'
      const regexDouble = new RegExp(`"(${oldKey})"`, "g");
      const regexSingle = new RegExp(`'(${oldKey})'`, "g");

      if (regexDouble.test(content) || regexSingle.test(content)) {
        content = content.replace(regexDouble, `"${newKey}"`);
        content = content.replace(regexSingle, `'${newKey}'`);
        updated = true;
      }
    }

    if (updated) {
      fs.writeFileSync(filePath, content, "utf8");
      console.log(`✅ Updated references in ${filePath}`);
    }
  }

  console.log("✨ All config key references updated successfully!");
}

// 🚀 Main
(function main() {
  console.log("🚀 Bắt đầu rename key trong config.xml ...");
  loadExistingMapping();
  processConfigXml();
  updateReferences();
  console.log("🎉 Hoàn tất rename key cho Firebase config!");
})();
