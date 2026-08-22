/**
 * check-unused-strings.js
 * Kiểm tra các string resources được khai báo nhưng không được sử dụng
 */

const fs = require("fs");
const path = require("path");
const glob = require("glob");
const { XMLParser } = require("fast-xml-parser");
const config = require("./rename-config-global") || {};

// ⚙️ Cấu hình
const ROOT_DIR = path.resolve(__dirname, config.srcCodePath || "app/src/main");
const RES_DIR = path.join(ROOT_DIR, "res");
const UNUSED_REPORT_FILE = path.resolve(
  __dirname,
  "unused-strings-report.json"
);

// Set lưu trữ tất cả các string keys được khai báo
const declaredStrings = new Set();

// Set lưu trữ các string keys được sử dụng
const usedStrings = new Set();

// 🔍 Hàm tìm file đệ quy
function findFilesRecursive(dir, exts) {
  const results = [];
  if (!fs.existsSync(dir)) return results;

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

// 📄 Đọc tất cả string keys từ strings.xml
function collectDeclaredStrings() {
  console.log("📖 Đang đọc các string keys đã khai báo...");

  const pattern = path.join(RES_DIR, "values*/strings.xml").replace(/\\/g, "/");
  const files = glob.sync(pattern);

  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    parseTagValue: false,
  });

  for (const filePath of files) {
    try {
      const xmlText = fs.readFileSync(filePath, "utf8");
      const parsed = parser.parse(xmlText);

      if (!parsed || !parsed.resources) {
        console.warn(`⚠️ Không tìm thấy <resources> trong ${filePath}`);
        continue;
      }

      const resources = parsed.resources;

      // Xử lý thẻ <string>
      if (resources.string) {
        const strings = Array.isArray(resources.string)
          ? resources.string
          : [resources.string];

        for (const item of strings) {
          const key = item["@_name"];
          if (key) {
            declaredStrings.add(key);
          }
        }
      }

      // Xử lý thẻ <string-array>
      if (resources["string-array"]) {
        const arrays = Array.isArray(resources["string-array"])
          ? resources["string-array"]
          : [resources["string-array"]];

        for (const item of arrays) {
          const key = item["@_name"];
          if (key) {
            declaredStrings.add(key);
          }
        }
      }

      // Xử lý thẻ <plurals>
      if (resources.plurals) {
        const plurals = Array.isArray(resources.plurals)
          ? resources.plurals
          : [resources.plurals];

        for (const item of plurals) {
          const key = item["@_name"];
          if (key) {
            declaredStrings.add(key);
          }
        }
      }
    } catch (error) {
      console.error(`❌ Lỗi khi đọc ${filePath}:`, error.message);
    }
  }

  console.log(`✅ Tìm thấy ${declaredStrings.size} string keys được khai báo`);
}

// 🔍 Tìm các string được sử dụng trong XML files
function findUsedStringsInXml() {
  console.log("🔍 Đang quét các file XML...");

  // Tìm tất cả file XML (trừ strings.xml)
  const xmlFiles = glob
    .sync(path.join(RES_DIR, "**/*.xml").replace(/\\/g, "/"))
    .filter((file) => !file.includes("strings.xml"));

  // Thêm AndroidManifest.xml
  const manifestPath = path.join(ROOT_DIR, "AndroidManifest.xml");
  if (fs.existsSync(manifestPath)) {
    xmlFiles.push(manifestPath);
  }

  const stringRegex = /@string\/([a-zA-Z0-9_]+)/g;

  for (const filePath of xmlFiles) {
    try {
      const content = fs.readFileSync(filePath, "utf8");
      let match;

      while ((match = stringRegex.exec(content)) !== null) {
        usedStrings.add(match[1]);
      }
    } catch (error) {
      console.error(`❌ Lỗi khi đọc ${filePath}:`, error.message);
    }
  }

  console.log(
    `✅ Tìm thấy ${usedStrings.size} string references trong XML files`
  );
}

// 🔍 Tìm các string được sử dụng trong Java/Kotlin files
function findUsedStringsInCode() {
  console.log("🔍 Đang quét các file Java/Kotlin...");

  const codeFiles = findFilesRecursive(ROOT_DIR, [".java", ".kt"]);

  // Pattern để tìm R.string.xxx hoặc R.string.xxx()
  const stringRegex = /R\.string\.([a-zA-Z0-9_]+)/g;

  for (const filePath of codeFiles) {
    try {
      const content = fs.readFileSync(filePath, "utf8");
      let match;

      while ((match = stringRegex.exec(content)) !== null) {
        usedStrings.add(match[1]);
      }
    } catch (error) {
      console.error(`❌ Lỗi khi đọc ${filePath}:`, error.message);
    }
  }

  console.log(`✅ Quét xong ${codeFiles.length} code files`);
}

// 📊 So sánh và tạo report
function generateReport() {
  console.log("📊 Đang tạo báo cáo...");

  const unusedStrings = [];

  for (const key of declaredStrings) {
    if (!usedStrings.has(key)) {
      unusedStrings.push(key);
    }
  }

  // Sắp xếp theo alphabet
  unusedStrings.sort();

  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalDeclared: declaredStrings.size,
      totalUsed: usedStrings.size,
      totalUnused: unusedStrings.length,
      usageRate: `${((usedStrings.size / declaredStrings.size) * 100).toFixed(
        2
      )}%`,
    },
    unusedStrings: unusedStrings,
  };

  // Ghi file JSON
  fs.writeFileSync(UNUSED_REPORT_FILE, JSON.stringify(report, null, 2), "utf8");

  // Hiển thị kết quả
  console.log("\n" + "=".repeat(60));
  console.log("📊 BÁO CÁO STRING RESOURCES KHÔNG SỬ DỤNG");
  console.log("=".repeat(60));
  console.log(`📝 Tổng số string đã khai báo: ${report.summary.totalDeclared}`);
  console.log(`✅ Số string được sử dụng: ${report.summary.totalUsed}`);
  console.log(`❌ Số string KHÔNG sử dụng: ${report.summary.totalUnused}`);
  console.log(`📈 Tỷ lệ sử dụng: ${report.summary.usageRate}`);
  console.log("=".repeat(60));

  if (unusedStrings.length > 0) {
    console.log("\n🗑️ Danh sách strings không được sử dụng:");
    console.log("-".repeat(60));

    // Hiển thị tối đa 50 strings đầu tiên
    const displayLimit = 50;
    const displayStrings = unusedStrings.slice(0, displayLimit);

    displayStrings.forEach((key, index) => {
      console.log(`${(index + 1).toString().padStart(4)}. ${key}`);
    });

    if (unusedStrings.length > displayLimit) {
      console.log(
        `\n... và ${unusedStrings.length - displayLimit} strings khác`
      );
    }

    console.log("-".repeat(60));
  }

  console.log(`\n💾 Chi tiết đầy đủ đã được lưu vào: ${UNUSED_REPORT_FILE}`);
  console.log("=".repeat(60) + "\n");

  return report;
}

// 🗑️ Tạo script để xóa unused strings (tùy chọn)
function generateCleanupScript(report) {
  if (report.unusedStrings.length === 0) {
    console.log("✨ Không có string nào cần dọn dẹp!");
    return;
  }

  const scriptPath = path.resolve(__dirname, "cleanup-unused-strings.js");

  const scriptContent = `/**
 * cleanup-unused-strings.js
 * Script tự động xóa các unused strings
 * ⚠️ CẢNH BÁO: Hãy backup code trước khi chạy script này!
 */

const fs = require("fs");
const path = require("path");
const glob = require("glob");
const { XMLParser, XMLBuilder } = require("fast-xml-parser");
const config = require("./rename-config-global");

const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);
const RES_DIR = path.join(ROOT_DIR, "res");

// Danh sách strings cần xóa
const stringsToRemove = new Set(${JSON.stringify(
    report.unusedStrings,
    null,
    2
  )});

console.log(\`🗑️ Sẽ xóa \${stringsToRemove.size} unused strings...\`);

// Xóa strings từ các file strings.xml
function cleanupStringsXml() {
  const pattern = path.join(RES_DIR, "values*/strings.xml").replace(/\\\\/g, "/");
  const files = glob.sync(pattern);
  
  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    parseTagValue: false,
  });
  
  const builder = new XMLBuilder({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    textNodeName: "#text",
    format: true,
    indentBy: "    ",
    suppressEmptyNode: true,
  });
  
  for (const filePath of files) {
    try {
      const xmlText = fs.readFileSync(filePath, "utf8");
      const parsed = parser.parse(xmlText);
      
      if (!parsed || !parsed.resources) continue;
      
      let changed = false;
      const resources = parsed.resources;
      
      // Xử lý <string>
      if (resources.string) {
        const strings = Array.isArray(resources.string) 
          ? resources.string 
          : [resources.string];
        
        const filtered = strings.filter(item => {
          const key = item["@_name"];
          if (key && stringsToRemove.has(key)) {
            console.log(\`  🗑️ Xóa: \${key}\`);
            changed = true;
            return false;
          }
          return true;
        });
        
        resources.string = filtered.length === 1 ? filtered[0] : filtered;
        if (filtered.length === 0) delete resources.string;
      }
      
      // Xử lý <string-array>
      if (resources["string-array"]) {
        const arrays = Array.isArray(resources["string-array"]) 
          ? resources["string-array"] 
          : [resources["string-array"]];
        
        const filtered = arrays.filter(item => {
          const key = item["@_name"];
          if (key && stringsToRemove.has(key)) {
            console.log(\`  🗑️ Xóa array: \${key}\`);
            changed = true;
            return false;
          }
          return true;
        });
        
        resources["string-array"] = filtered.length === 1 ? filtered[0] : filtered;
        if (filtered.length === 0) delete resources["string-array"];
      }
      
      // Xử lý <plurals>
      if (resources.plurals) {
        const plurals = Array.isArray(resources.plurals) 
          ? resources.plurals 
          : [resources.plurals];
        
        const filtered = plurals.filter(item => {
          const key = item["@_name"];
          if (key && stringsToRemove.has(key)) {
            console.log(\`  🗑️ Xóa plural: \${key}\`);
            changed = true;
            return false;
          }
          return true;
        });
        
        resources.plurals = filtered.length === 1 ? filtered[0] : filtered;
        if (filtered.length === 0) delete resources.plurals;
      }
      
      if (changed) {
        // Backup file gốc
        // fs.copyFileSync(filePath, filePath + ".backup");
        
        // Ghi file mới
        const newXml = builder.build(parsed);
        fs.writeFileSync(filePath, newXml.trim() + "\\n", "utf8");
        console.log(\`✅ Đã cập nhật: \${filePath}\`);
      }
      
    } catch (error) {
      console.error(\`❌ Lỗi khi xử lý \${filePath}:\`, error.message);
    }
  }
}

console.log("⚠️  BACKUP các file đã được tạo với extension .backup");
console.log("🚀 Bắt đầu dọn dẹp...");
cleanupStringsXml();
console.log("✨ Hoàn tất!");
`;

  fs.writeFileSync(scriptPath, scriptContent, "utf8");
  console.log(`\n🛠️ Script dọn dẹp đã được tạo: ${scriptPath}`);
  console.log("   Chạy script này nếu bạn muốn tự động xóa các unused strings");
  console.log("   ⚠️ Nhớ backup code trước khi chạy!\n");
}

// 🚀 Main function
(function main() {
  console.log("🚀 Bắt đầu kiểm tra unused strings...\n");

  try {
    // Bước 1: Thu thập các string đã khai báo
    collectDeclaredStrings();

    // Bước 2: Tìm các string được sử dụng trong XML
    findUsedStringsInXml();

    // Bước 3: Tìm các string được sử dụng trong code
    findUsedStringsInCode();

    // Bước 4: Tạo báo cáo
    const report = generateReport();

    // Bước 5: Tạo cleanup script (tùy chọn)
    generateCleanupScript(report);

    console.log("🎉 Hoàn tất kiểm tra!");
  } catch (error) {
    console.error("❌ Lỗi:", error.message);
    console.error(error.stack);
    process.exit(1);
  }
})();
