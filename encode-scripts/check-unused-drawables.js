/**
 * check-unused-drawables.js
 * Kiểm tra các drawable resources được khai báo nhưng không được sử dụng
 */

const fs = require("fs");
const path = require("path");
const glob = require("glob");
const config = require("./rename-config-global") || {};

// ⚙️ Cấu hình
const ROOT_DIR = path.resolve(__dirname, config.srcCodePath || "app/src/main");
const RES_DIR = path.join(ROOT_DIR, "res");
const UNUSED_REPORT_FILE = path.resolve(
  __dirname,
  "unused-drawables-report.json"
);

// Set lưu trữ tất cả các drawable names được khai báo
const declaredDrawables = new Set();

// Map lưu file path của từng drawable (để có thể xóa sau này)
const drawableFiles = new Map(); // key: drawable_name, value: [file_paths]

// Set lưu trữ các drawable được sử dụng
const usedDrawables = new Set();

// 🔍 Hàm tìm file đệ quy
function findFilesRecursive(dir, exts) {
  const results = [];
  if (!fs.existsSync(dir)) return results;

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

// 📄 Thu thập tất cả các drawable files
function collectDeclaredDrawables() {
  console.log("📖 Đang đọc các drawable files đã khai báo...");

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

  let fileCount = 0;

  for (const dir of drawableDirs) {
    if (!fs.existsSync(dir)) continue;

    const files = fs.readdirSync(dir);
    for (const file of files) {
      const ext = path.extname(file).toLowerCase();
      if (!exts.includes(ext)) continue;

      const baseName = path.basename(file, ext);
      const fullPath = path.join(dir, file);

      declaredDrawables.add(baseName);

      // Lưu file path để có thể xóa sau
      if (!drawableFiles.has(baseName)) {
        drawableFiles.set(baseName, []);
      }
      drawableFiles.get(baseName).push(fullPath);

      fileCount++;
    }
  }

  console.log(
    `✅ Tìm thấy ${fileCount} drawable files (${declaredDrawables.size} unique names)`
  );
}

// 🔍 Tìm các drawable được sử dụng trong XML files
function findUsedDrawablesInXml() {
  console.log("🔍 Đang quét các file XML...");

  // Tìm tất cả file XML
  const xmlFiles = glob.sync(
    path.join(RES_DIR, "**/*.xml").replace(/\\/g, "/")
  );

  // Thêm AndroidManifest.xml
  const manifestPath = path.join(ROOT_DIR, "AndroidManifest.xml");
  if (fs.existsSync(manifestPath)) {
    xmlFiles.push(manifestPath);
  }

  // Pattern để tìm @drawable/xxx, @mipmap/xxx
  const drawableRegex = /@(drawable|mipmap)\/([a-zA-Z0-9_]+)/g;

  // Pattern để tìm android:drawable="..." trong XML (selector, layer-list, etc)
  const androidDrawableRegex =
    /android:drawable="@(drawable|mipmap)\/([a-zA-Z0-9_]+)"/g;

  let foundCount = 0;

  for (const filePath of xmlFiles) {
    try {
      const content = fs.readFileSync(filePath, "utf8");
      let match;

      // Tìm @drawable/xxx pattern
      while ((match = drawableRegex.exec(content)) !== null) {
        usedDrawables.add(match[2]);
        foundCount++;
      }

      // Tìm android:drawable pattern
      drawableRegex.lastIndex = 0; // Reset regex
      while ((match = androidDrawableRegex.exec(content)) !== null) {
        usedDrawables.add(match[2]);
        foundCount++;
      }
    } catch (error) {
      console.error(`❌ Lỗi khi đọc ${filePath}:`, error.message);
    }
  }

  console.log(
    `✅ Tìm thấy ${foundCount} drawable references trong ${xmlFiles.length} XML files`
  );
}

// 🔍 Tìm các drawable được sử dụng trong Java/Kotlin files
function findUsedDrawablesInCode() {
  console.log("🔍 Đang quét các file Java/Kotlin...");

  const codeFiles = findFilesRecursive(ROOT_DIR, [".java", ".kt"]);

  // Pattern để tìm R.drawable.xxx, R.mipmap.xxx
  const drawableRegex = /R\.(drawable|mipmap)\.([a-zA-Z0-9_]+)/g;

  // Pattern để tìm "drawable_name" trong context.getDrawable, ContextCompat.getDrawable
  const getDrawableRegex =
    /(?:getDrawable|getDrawableCompat|getDrawableForDensity)\s*\(\s*R\.(drawable|mipmap)\.([a-zA-Z0-9_]+)/g;

  let foundCount = 0;

  for (const filePath of codeFiles) {
    try {
      const content = fs.readFileSync(filePath, "utf8");
      let match;

      // Tìm R.drawable.xxx pattern
      while ((match = drawableRegex.exec(content)) !== null) {
        usedDrawables.add(match[2]);
        foundCount++;
      }

      // Tìm getDrawable pattern
      drawableRegex.lastIndex = 0; // Reset regex
      while ((match = getDrawableRegex.exec(content)) !== null) {
        usedDrawables.add(match[2]);
        foundCount++;
      }
    } catch (error) {
      console.error(`❌ Lỗi khi đọc ${filePath}:`, error.message);
    }
  }

  console.log(
    `✅ Tìm thấy ${foundCount} drawable references trong ${codeFiles.length} code files`
  );
}

// 📊 So sánh và tạo report
function generateReport() {
  console.log("📊 Đang tạo báo cáo...");

  const unusedDrawables = [];
  const unusedFiles = [];
  let totalFileSize = 0;

  for (const drawableName of declaredDrawables) {
    if (!usedDrawables.has(drawableName)) {
      const files = drawableFiles.get(drawableName) || [];

      // Tính tổng kích thước files
      let drawableSize = 0;
      const fileDetails = [];

      for (const filePath of files) {
        try {
          const stats = fs.statSync(filePath);
          drawableSize += stats.size;
          fileDetails.push({
            path: filePath.replace(ROOT_DIR, ""),
            size: stats.size,
            sizeKB: (stats.size / 1024).toFixed(2),
          });
        } catch (e) {
          // File không tồn tại hoặc lỗi khác
        }
      }

      totalFileSize += drawableSize;

      unusedDrawables.push({
        name: drawableName,
        fileCount: files.length,
        totalSize: drawableSize,
        totalSizeKB: (drawableSize / 1024).toFixed(2),
        totalSizeMB: (drawableSize / (1024 * 1024)).toFixed(2),
        files: fileDetails,
      });

      unusedFiles.push(...files);
    }
  }

  // Sắp xếp theo kích thước giảm dần
  unusedDrawables.sort((a, b) => b.totalSize - a.totalSize);

  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalDeclared: declaredDrawables.size,
      totalUsed: usedDrawables.size,
      totalUnused: unusedDrawables.length,
      totalUnusedFiles: unusedFiles.length,
      usageRate: `${(
        (usedDrawables.size / declaredDrawables.size) *
        100
      ).toFixed(2)}%`,
      wastedSpace: {
        bytes: totalFileSize,
        KB: (totalFileSize / 1024).toFixed(2),
        MB: (totalFileSize / (1024 * 1024)).toFixed(2),
      },
    },
    unusedDrawables: unusedDrawables,
  };

  // Ghi file JSON
  fs.writeFileSync(UNUSED_REPORT_FILE, JSON.stringify(report, null, 2), "utf8");

  // Hiển thị kết quả
  console.log("\n" + "=".repeat(70));
  console.log("📊 BÁO CÁO DRAWABLE RESOURCES KHÔNG SỬ DỤNG");
  console.log("=".repeat(70));
  console.log(
    `📝 Tổng số drawable đã khai báo: ${report.summary.totalDeclared}`
  );
  console.log(`✅ Số drawable được sử dụng: ${report.summary.totalUsed}`);
  console.log(`❌ Số drawable KHÔNG sử dụng: ${report.summary.totalUnused}`);
  console.log(`📁 Số file KHÔNG sử dụng: ${report.summary.totalUnusedFiles}`);
  console.log(`📈 Tỷ lệ sử dụng: ${report.summary.usageRate}`);
  console.log(
    `💾 Dung lượng lãng phí: ${report.summary.wastedSpace.MB} MB (${report.summary.wastedSpace.KB} KB)`
  );
  console.log("=".repeat(70));

  if (unusedDrawables.length > 0) {
    console.log("\n🗑️ Top 20 drawables không được sử dụng (theo kích thước):");
    console.log("-".repeat(70));

    const displayLimit = 20;
    const displayDrawables = unusedDrawables.slice(0, displayLimit);

    displayDrawables.forEach((item, index) => {
      console.log(
        `${(index + 1).toString().padStart(3)}. ${item.name.padEnd(
          40
        )} ${item.totalSizeKB.padStart(10)} KB (${item.fileCount} file${
          item.fileCount > 1 ? "s" : ""
        })`
      );
    });

    if (unusedDrawables.length > displayLimit) {
      console.log(
        `\n... và ${unusedDrawables.length - displayLimit} drawables khác`
      );
    }

    console.log("-".repeat(70));
  }

  console.log(`\n💾 Chi tiết đầy đủ đã được lưu vào: ${UNUSED_REPORT_FILE}`);
  console.log("=".repeat(70) + "\n");

  return report;
}

// 🗑️ Tạo script để xóa unused drawables (tùy chọn)
function generateCleanupScript(report) {
  if (report.unusedDrawables.length === 0) {
    console.log("✨ Không có drawable nào cần dọn dẹp!");
    return;
  }

  const scriptPath = path.resolve(__dirname, "cleanup-unused-drawables.js");

  // Tạo danh sách files cần xóa
  const filesToDelete = [];
  report.unusedDrawables.forEach((item) => {
    item.files.forEach((file) => {
      filesToDelete.push(file.path);
    });
  });

  const scriptContent = `/**
 * cleanup-unused-drawables.js
 * Script tự động xóa các unused drawables
 * ⚠️ CẢNH BÁO: Hãy backup code trước khi chạy script này!
 */

const fs = require("fs");
const path = require("path");
const config = require("./rename-config-global");

const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);

// Danh sách files cần xóa
const filesToDelete = ${JSON.stringify(filesToDelete, null, 2)};

console.log(\`🗑️ Sẽ xóa \${filesToDelete.length} files...\`);
console.log(\`💾 Tổng dung lượng tiết kiệm: ${
    report.summary.wastedSpace.MB
  } MB\`);
console.log("⚠️  Đang tạo backup...");

// Tạo thư mục backup
const backupDir = path.resolve(__dirname, "drawable-backup-" + Date.now());
fs.mkdirSync(backupDir, { recursive: true });

let deletedCount = 0;
let errorCount = 0;

for (const relPath of filesToDelete) {
  const fullPath = path.join(ROOT_DIR, relPath);
  
  try {
    if (!fs.existsSync(fullPath)) {
      console.log(\`⚠️  File không tồn tại: \${relPath}\`);
      continue;
    }
    
    // Backup file trước khi xóa
    const backupPath = path.join(backupDir, relPath);
    const backupFolder = path.dirname(backupPath);
    fs.mkdirSync(backupFolder, { recursive: true });
    fs.copyFileSync(fullPath, backupPath);
    
    // Xóa file
    fs.unlinkSync(fullPath);
    deletedCount++;
    console.log(\`🗑️  Đã xóa: \${relPath}\`);
    
  } catch (error) {
    console.error(\`❌ Lỗi khi xóa \${relPath}:\`, error.message);
    errorCount++;
  }
}

console.log("\\n" + "=".repeat(70));
console.log(\`✅ Đã xóa: \${deletedCount} files\`);
console.log(\`❌ Lỗi: \${errorCount} files\`);
console.log(\`💾 Backup được lưu tại: \${backupDir}\`);
console.log("=".repeat(70));
console.log("\\n✨ Hoàn tất dọn dẹp!");
`;

  fs.writeFileSync(scriptPath, scriptContent, "utf8");
  console.log(`\n🛠️  Script dọn dẹp đã được tạo: ${scriptPath}`);
  console.log(
    "   Chạy script này nếu bạn muốn tự động xóa các unused drawables"
  );
  console.log(`   💾 Có thể tiết kiệm: ${report.summary.wastedSpace.MB} MB`);
  console.log("   ⚠️  Nhớ backup code trước khi chạy!\n");
}

// 🚀 Main function
(function main() {
  console.log("🚀 Bắt đầu kiểm tra unused drawables...\n");

  try {
    // Bước 1: Thu thập các drawable đã khai báo
    collectDeclaredDrawables();

    // Bước 2: Tìm các drawable được sử dụng trong XML
    findUsedDrawablesInXml();

    // Bước 3: Tìm các drawable được sử dụng trong code
    findUsedDrawablesInCode();

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
