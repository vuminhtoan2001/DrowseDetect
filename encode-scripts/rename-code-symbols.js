// rename-code-symbols.js — LÕI DÙNG CHUNG, không sửa khi clone app mới (xem rename-config-global.js).
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const config = require("./rename-config-global");
const safeSkipBase = require("./safe-skip-base");

// ✅ Tách riêng mapping theo loại
const classMappings = new Map();
const objectMappings = new Map();
const funMappings = new Map();

// ✅ Blacklist: tên trùng với tên file
const fileNameBlacklist = new Set();

const projectRoot = path.resolve(__dirname, config.srcCodePath);
const outputPath = config.codeSymbolsOutputFile;
const RANDOM_SALT = config.RANDOM_SALT;
const HASH_LENGTH = config.HASH_LENGTH;

// Tên hàm/class/segment package KHÔNG BAO GIỜ rename — gộp danh sách lõi dùng chung
// (safe-skip-base.js) + danh sách riêng của app này (rename-config-global.js →
// SAFE_SKIP_APP_SPECIFIC). KHÔNG thêm tên mới trực tiếp vào đây — thêm vào 1 trong 2 file config.
const SAFE_SKIP_NAMES = new Set([
  ...safeSkipBase.SAFE_SKIP_BASE,
  ...(config.SAFE_SKIP_APP_SPECIFIC || []),
]);

// Ngưỡng độ dài tối thiểu để 1 symbol được rename. Symbol quá ngắn (1-2 ký tự, vd hàm `m()`,
// `fun a()`) rất dễ TRÙNG với 1 từ/âm tiết bất kỳ trong comment/doc tiếng Việt hoặc tiếng Anh khi
// updateReferences() replace theo `\bOLD\b` toàn văn bản — gây hỏng comment (vd "thêm" → "thêm"
// với "m" bị thay bằng hash, ra "thêfcebc90"). An toàn hơn là bỏ qua rename cho các tên quá ngắn.
const MIN_RENAMABLE_NAME_LENGTH = 3;

function shouldSkipRename(name) {
  if (SAFE_SKIP_NAMES.has(name)) return true;
  if (name.length < MIN_RENAMABLE_NAME_LENGTH) return true;
  if (/^[A-Z][a-zA-Z0-9]+ViewModel$/.test(name)) return true;
  if (/^provide[A-Z]/.test(name)) return true;
  if (/^on[A-Z]/.test(name)) return true;
  return false;
}

// ✅ Hash với prefix khác nhau cho mỗi loại
function hashName(name, type) {
  if (
    name.startsWith("get") ||
    name.startsWith("set") ||
    name.startsWith("fetch") ||
    name.startsWith("init") ||
    name.startsWith("open")
  )
    return name;
  const prefix =
    {
      class: "c", // class prefix
      object: "o", // object prefix
      fun: "f", // function prefix
    }[type] || "v";

  let hash = crypto
    .createHash("sha1")
    .update(name + RANDOM_SALT + type)
    .digest("hex")
    .substring(0, HASH_LENGTH);

  let result = prefix + hash;

  // Nếu ký tự đầu của name là chữ hoa thì viết hoa ký tự đầu kết quả
  if (name[0] && name[0] === name[0].toUpperCase()) {
    result = result[0].toUpperCase() + result.slice(1);
  }

  return result;
}

// Tìm khai báo class/object/fun trong 1 đoạn text (dùng chung cho buildFileNameBlacklist/
// processFile/updateReferences). class/object luôn đơn giản (Kotlin không có "extension class").
// fun thì PHỨC TẠP hơn vì Kotlin extension function có receiver-type đứng trước tên hàm thật:
//   - receiver dạng FQN có dấu chấm: `fun androidx.compose.ui.graphics.drawscope.DrawScope.drawX(`
//   - receiver dạng generic:        `fun List<Offset>.containsPoint(`
//   - function-level generic:       `fun <T> Foo.bar(`
// → chỉ lấy đoạn text giữa "fun " và dấu "(" đầu tiên, rồi tách theo dấu "." cuối cùng để lấy ĐÚNG
// tên hàm thật. Bug đã gặp (2 lần, xem git log): bắt nhầm "androidx"/"List" (segment ĐẦU của
// receiver) làm tên hàm rồi replace toàn văn bản → phá import androidx.* / kiểu List<> cả project.
function extractFunName(rawChunk) {
  const lastDotIdx = rawChunk.lastIndexOf(".");
  const tail = lastDotIdx >= 0 ? rawChunk.slice(lastDotIdx + 1) : rawChunk;
  const m = tail.match(/([A-Za-z_][A-Za-z0-9_]*)\s*$/);
  return m ? m[1] : null;
}

function findDeclarations(content) {
  const results = [];
  const regex = /\b(?:(class|object)\s+([A-Za-z_][A-Za-z0-9_]*)|(fun)\s+([^(\n]+?)\s*\()/g;
  let match;
  while ((match = regex.exec(content)) !== null) {
    if (match[1]) {
      results.push({ type: match[1], name: match[2], index: match.index });
    } else {
      const name = extractFunName(match[4]);
      if (name) results.push({ type: "fun", name, index: match.index });
    }
  }
  return results;
}

function getAllCodeFiles(dir) {
  const result = [];
  for (const item of fs.readdirSync(dir)) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) result.push(...getAllCodeFiles(fullPath));
    else if (/\.(kt|java)$/i.test(item)) result.push(fullPath);
  }
  return result;
}

// ✅ Lấy mapping phù hợp theo type
function getMappingByType(type) {
  switch (type) {
    case "class":
      return classMappings;
    case "object":
      return objectMappings;
    case "fun":
      return funMappings;
    default:
      return new Map();
  }
}

// ✅ BƯỚC 1: Quét tất cả file để build blacklist trước
function buildFileNameBlacklist() {
  const allFiles = getAllCodeFiles(projectRoot);

  for (const file of allFiles) {
    const content = fs.readFileSync(file, "utf8");
    const ext = path.extname(file);
    const fileBase = path.basename(file, ext);

    // Tìm tất cả class/object/fun trong file
    console.log("fileBase", fileBase);
    for (const { type, name } of findDeclarations(content)) {
      // ✅ Nếu tên trùng với tên file -> thêm vào blacklist
      if (name === fileBase) {
        fileNameBlacklist.add(name);
        console.log(
          `🔒 Blacklist: "${name}" (${type} trùng tên file ${fileBase}${ext})`
        );
      }
    }
  }

  console.log(`\n✅ Đã tạo blacklist với ${fileNameBlacklist.size} tên.\n`);
}

function processFile(filePath) {
  let content = fs.readFileSync(filePath, "utf8");
  const ext = path.extname(filePath);
  const fileBase = path.basename(filePath, ext);

  // 🧩 Tìm các class|object|fun
  for (const { type, name: oldName, index } of findDeclarations(content)) {
    // ✅ Bỏ qua override fun — method override từ AAR library (BaseApplication, NativeCallback,
    //    AdCallback...) KHÔNG được rename (sẽ "overrides nothing" vì hàm gốc trong lib giữ tên thật).
    if (type === "fun") {
      const ls = content.lastIndexOf("\n", index) + 1;
      const le = content.indexOf("\n", index);
      const line = content.substring(ls, le < 0 ? content.length : le);
      if (/\boverride\b/.test(line)) {
        continue;
      }
    }

    // ✅ Skip nếu trong blacklist (ưu tiên cao nhất)
    if (fileNameBlacklist.has(oldName)) {
      continue;
    }

    if (shouldSkipRename(oldName)) continue;

    const mapping = getMappingByType(type);
    if (!mapping.has(oldName)) {
      mapping.set(oldName, hashName(oldName, type));
    }
  }
}

function updateReferences() {
  const allFiles = getAllCodeFiles(projectRoot);

  for (const file of allFiles) {
    let content = fs.readFileSync(file, "utf8");

    // ✅ Kiểm tra nếu file chứa enum Screen với route strings
    const isScreenEnumFile = /enum\s+class\s+Screen\s*\(\s*val\s+route\s*:\s*String\s*\)/.test(content);

    // Xử lý theo thứ tự: class -> object -> fun
    [
      { type: "class", mapping: classMappings },
      { type: "object", mapping: objectMappings },
      { type: "fun", mapping: funMappings },
    ].forEach(({ type, mapping }) => {
      for (const [oldName, newName] of mapping) {
        // ✅ Kiểm tra blacklist trước khi replace
        if (fileNameBlacklist.has(oldName)) {
          continue;
        }

        // Chỉ replace nơi không phải declaration của type khác
        const lines = content.split("\n");
        const newLines = lines.map((line) => {
          const declOnLine = findDeclarations(line).find((d) => d.name === oldName);
          if (declOnLine && declOnLine.type !== type) {
            return line; // Giữ nguyên declaration của type khác
          }

          // ✅ Nếu là file Screen.kt và line chứa enum với string literal, không replace string trong ngoặc kép
          if (isScreenEnumFile && /^\s*[A-Za-z0-9_]+\s*\(\s*"[^"]*"\s*\)/.test(line)) {
            // Chỉ replace phần identifier (trước dấu ngoặc), không replace string literal
            return line.replace(new RegExp(`^(\\s*)${oldName}(\\s*\\([^)]*\\))`, 'g'), `$1${newName}$2`);
          }

          return line.replace(new RegExp(`\\b${oldName}\\b`, "g"), newName);
        });
        content = newLines.join("\n");
      }
    });

    fs.writeFileSync(file, content, "utf8");
  }

  const totalMappings =
    classMappings.size + objectMappings.size + funMappings.size;
  console.log(
    `✅ Đã cập nhật references (${totalMappings} symbols: ${classMappings.size} classes, ${objectMappings.size} objects, ${funMappings.size} functions).`
  );
}

// MAIN
(function main() {
  console.log("🔍 Bắt đầu quét project...\n");

  // ✅ BƯỚC 1: Build blacklist trước tiên
  buildFileNameBlacklist();

  // ✅ BƯỚC 2: Process files và tạo mappings
  console.log("🔨 Đang tạo mappings...\n");
  const files = getAllCodeFiles(projectRoot);
  for (const file of files) processFile(file);

  // ✅ BƯỚC 3: Update references
  console.log("\n🔄 Đang cập nhật references...\n");
  updateReferences();

  // ✅ Lưu mapping ra file JSON với phân loại
  const allMappings = {
    metadata: {
      blacklist: Array.from(fileNameBlacklist).sort(),
      totalSymbols: classMappings.size + objectMappings.size + funMappings.size,
      timestamp: new Date().toISOString(),
    },
    class: Object.fromEntries(classMappings),
    object: Object.fromEntries(objectMappings),
    fun: Object.fromEntries(funMappings),
  };

  fs.writeFileSync(outputPath, JSON.stringify(allMappings, null, 2));
  console.log(`\n✅ ${outputPath} đã được tạo.`);
  console.log(`\n📊 Thống kê:`);
  console.log(`   - Blacklist: ${fileNameBlacklist.size} tên`);
  console.log(`   - Classes: ${classMappings.size}`);
  console.log(`   - Objects: ${objectMappings.size}`);
  console.log(`   - Functions: ${funMappings.size}`);
  console.log(
    `   - Tổng: ${classMappings.size + objectMappings.size + funMappings.size}`
  );
})();
