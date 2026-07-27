/**
 * rename-languages.js
 */

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const glob = require("glob");
const { XMLParser, XMLBuilder } = require("fast-xml-parser");
const config = require("./rename-config-global");

// ⚙️ Cấu hình

const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);
const RES_DIR = path.join(ROOT_DIR, "res");
const MAPPING_FILE = path.resolve(__dirname, config.languagesOutputFile);
const RANDOM_SALT = config.RANDOM_SALT;
const HASH_LENGTH = config.HASH_LENGTH;

// Map lưu trữ key cũ → mới
const keyMappings = new Map();

// 🔑 Hàm tạo key mới
function hashName(key) {
  let newKey = crypto
    .createHash("sha1")
    .update(key + RANDOM_SALT)
    .digest("hex")
    .substring(0, HASH_LENGTH);

  if (/^\d/.test(newKey)) newKey = "l" + newKey;
  return newKey;
}

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

// 📄 Đọc mapping cũ (nếu có)
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

// 🧩 Đổi key trong file strings.xml
function processStringsXml(filePath) {
  console.log(`🔧 Đang xử lý ${filePath}`);
  const xmlText = fs.readFileSync(filePath, "utf8");

  // Parser - thử parse trước với preserveOrder: false (thường)
  const parserNormal = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    parseTagValue: false,
  });

  // Parser dự phòng nếu file được parse ở chế độ preserveOrder
  const parserPreserve = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    parseTagValue: false,
    preserveOrder: true,
  });

  let parsed;
  let usedPreserve = false;

  try {
    parsed = parserNormal.parse(xmlText);
  } catch (e) {
    parsed = null;
  }

  // Nếu parse bình thường không cho node resources => thử preserveOrder
  if (!parsed || !parsed.resources) {
    try {
      parsed = parserPreserve.parse(xmlText);
      usedPreserve = true;
    } catch (e) {
      parsed = null;
    }
  }

  if (!parsed) {
    console.error(`❌ Không thể parse XML: ${filePath}`);
    return;
  }

  // --- Xử lý trường hợp preserveOrder: true (mảng các node) ---
  if (usedPreserve) {
    // parsed có dạng mảng preserveOrder
    // Tìm phần tử resources
    const resourcesIndex = parsed.findIndex((node) => node && node.resources);
    if (resourcesIndex === -1) {
      console.warn(`⚠️ Không tìm thấy <resources> trong ${filePath}`);
      return;
    }

    const resourcesNode = parsed[resourcesIndex].resources; // đây là mảng các node con của <resources>

    // resourcesNode có dạng mảng: mỗi phần tử là { string: [...], ':@': { '@_name': '...' } } hoặc tương tự
    for (const node of resourcesNode) {
      // Node có thể là object chứa key 'string' (1 thẻ <string>)
      if (node && node.string) {
        // node[':@'] hoặc node[':@'] hoặc node['@_name'] phụ thuộc parse
        // Lấy tên cũ:
        let oldName = null;
        if (node[":@"] && node[":@"]["@_name"]) oldName = node[":@"]["@_name"];
        else if (node[":@"] && node[":@"]["@_name"])
          oldName = node[":@"]["@_name"];
        else if (
          node.string &&
          Array.isArray(node.string) &&
          node.string[0] &&
          node.string[0][":@"] &&
          node.string[0][":@"]["@_name"]
        ) {
          oldName = node.string[0][":@"]["@_name"];
        }

        // Nếu vẫn null, thử tìm thuộc tính trực tiếp (thường không xảy ra)
        if (!oldName) {
          // bỏ qua nếu không tìm được name
          continue;
        }

        // chỉ xử lý nếu là thẻ <string> (không phải string-array/plurals)
        // Đổi key nếu cần
        if (!keyMappings.has(oldName))
          keyMappings.set(oldName, hashName(oldName));
        const newName = keyMappings.get(oldName);

        // Gán lại vào nơi thích hợp
        if (node[":@"] && node[":@"]["@_name"]) node[":@"]["@_name"] = newName;
        else if (node[":@"] && node[":@"]["@_name"])
          node[":@"]["@_name"] = newName;
        else if (
          node.string &&
          Array.isArray(node.string) &&
          node.string[0] &&
          node.string[0][":@"] &&
          node.string[0][":@"]["@_name"]
        ) {
          node.string[0][":@"]["@_name"] = newName;
        }
      }
    }

    // Build lại dùng preserveOrder:true để giữ cấu trúc tương ứng
    const builder = new XMLBuilder({
      ignoreAttributes: false,
      attributeNamePrefix: "@_",
      textNodeName: "#text",
      preserveOrder: true,
      format: true,
      indentBy: "    ",
    });

    const newXml = builder.build(parsed);

    // Backup + ghi file
    fs.copyFileSync(filePath, filePath + ".bak");
    fs.writeFileSync(filePath, newXml, "utf8");
    console.log(`✅ Đã đổi key (preserveOrder) trong ${filePath}`);
    return;
  }

  // --- Xử lý trường hợp parse bình thường (object với resources) ---
  const resources = parsed.resources;

  // resources.string có thể là 1 object hoặc array
  let strings = [];
  if (resources.string === undefined) {
    // không có string tag
    console.warn(`⚠️ Không tìm thấy <string> trong ${filePath}`);
    return;
  } else if (Array.isArray(resources.string)) {
    strings = resources.string;
  } else {
    // 1 thẻ string -> normalize thành mảng
    strings = [resources.string];
  }

  // Duyệt từng phần tử string, chỉ lấy thẻ dạng <string name="...">text</string>
  for (const item of strings) {
    if (!item) continue;
    const oldKey = item["@_name"];
    if (!oldKey) continue; // skip nếu không có name (ví dụ thẻ comment khác)
    if (!keyMappings.has(oldKey)) keyMappings.set(oldKey, hashName(oldKey));
    item["@_name"] = keyMappings.get(oldKey);
  }

  // Build lại XML (không preserveOrder)
  const builder = new XMLBuilder({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    format: true,
    indentBy: "    ",
    suppressEmptyNode: true,
  });

  const newXml = builder.build(parsed);

  fs.writeFileSync(filePath, newXml.trim() + "\n", "utf8");
  console.log(`✅ Đã đổi key trong ${filePath}`);
}

// 🔍 Tìm tất cả strings.xml trong các values folders
function renameAllStrings() {
  const pattern = path.join(RES_DIR, "values*/strings.xml").replace(/\\/g, "/");
  const files = glob.sync(pattern);
  // processStringsXml(files[0]);
  files.forEach(processStringsXml);
  console.log(`📦 Đã xử lý ${files.length} file strings.xml`);
  saveMapping();
}

// 🔄 Thay thế trong các file code & xml
function updateReferences() {
  console.log("🔍 Updating references across source files...");

  // 🟩 Tìm toàn bộ file XML + Manifest
  const xmlFiles = [
    ...glob.sync(path.join(ROOT_DIR, "res/**/*.xml").replace(/\\/g, "/")),
    path.join(ROOT_DIR, "AndroidManifest.xml"),
  ].filter(fs.existsSync);

  // 🟩 Tìm toàn bộ file Java/Kotlin (đệ quy)
  const codeFiles = findFilesRecursive(ROOT_DIR, [".java", ".kt"]);

  const allFiles = [...xmlFiles, ...codeFiles];

  const listKey = Object.entries(Object.fromEntries(keyMappings));

  //   const listKey = [["close", "k9d0c01"]];
  for (const filePath of allFiles) {
    let content = fs.readFileSync(filePath, "utf8");
    let updated = false;

    for (const [oldKey, newKey] of listKey) {
      // @string/key trong XML
      const xmlRegex = new RegExp(`@string/${oldKey}\\b`, "g");
      // R.string.key trong code
      const codeRegex = new RegExp(`R\\.string\\.${oldKey}\\b`, "g");

      if (xmlRegex.test(content) || codeRegex.test(content)) {
        content = content.replace(xmlRegex, `@string/${newKey}`);
        content = content.replace(codeRegex, `R.string.${newKey}`);
        updated = true;
      }
    }

    if (updated) {
      fs.writeFileSync(filePath, content, "utf8");
      console.log(`✅ Updated references in ${filePath}`);
    }
  }

  console.log("✨ All references updated successfully.");
}
// 🚀 Chạy toàn bộ quy trình
(function main() {
  console.log("🚀 Bắt đầu rename key Android strings...");
  loadExistingMapping();
  renameAllStrings();
  updateReferences();
  console.log("🎉 Hoàn tất rename tất cả key!");
})();
