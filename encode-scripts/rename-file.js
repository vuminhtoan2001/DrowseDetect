// rename-file.js
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const config = require("./rename-config-global");

const projectManifest = path.resolve(
  __dirname,
  config.srcCodePath,
  "AndroidManifest.xml"
);

const projectRoot = path.resolve(__dirname, config.srcCodePath, "java");

const RANDOM_SALT = config.RANDOM_SALT;
const HASH_LENGTH = config.HASH_LENGTH;

// ✅ Package root không được đổi
const PROTECTED_PACKAGE_ROOT = config.BASE_NAMESPACE;
const PROTECTED_PATH = PROTECTED_PACKAGE_ROOT.replace(/\./g, path.sep);

const outputFile = config.fileChangeOutputFile;

// ✅ Danh sách tên folder được bảo vệ (không đổi tên, nhưng nội dung bên trong vẫn đổi)
const PROTECTED_FOLDER_NAMES = new Set([
  ...config.BASE_NAMESPACE.split("."),
  "Color",
  "com",
  "smartbudget",
  "trackermoney",
  "res",
  "values",
  "layout",
  "drawable",
  "mipmap",
  "menu",
  "xml",
  "raw",
  "assets",
  "keystore",
  "Type",
  "ui",
  "theme",
  // Thêm các folder khác cần bảo vệ ở đây
]);
// bảo vệ cả folde và file ở trong
const PROTECTED_FOLDER_AND_FILE = new Set([
  "dao",
  "database",
  "core",
  "data",
  "Color",
  "Type",
  "Success",
  "Theme",
  "MainActivity",
  "AdActivity"
]);

// Maps để lưu mapping
const folderMappings = new Map();
const fileMappings = new Map();
const packageMappings = new Map();

function hashName(name, prefix = "d", updateCase) {
  if (PROTECTED_FOLDER_NAMES.has(name)) return name;
  if (PROTECTED_FOLDER_AND_FILE.has(name)) return name;

  let hash = crypto
    .createHash("sha1")
    .update(name + RANDOM_SALT + "path")
    .digest("hex")
    .substring(0, HASH_LENGTH);

  if (/^\d/.test(hash)) hash = prefix + hash;

  if (updateCase) {
    hash = hash.charAt(0).toUpperCase() + hash.slice(1);
  }

  return hash;
}

// ✅ Kiểm tra xem path có phải là protected root không
function isProtectedPath(dirPath) {
  const relativePath = path.relative(projectRoot, dirPath);
  const normalizedPath = relativePath.replace(/\\/g, "/");

  return normalizedPath === PROTECTED_PATH;
}

// ✅ Kiểm tra xem có phải parent của protected path không
function isParentOfProtectedPath(dirPath) {
  const relativePath = path.relative(projectRoot, dirPath);
  const normalizedPath = relativePath.replace(/\\/g, "/");

  return PROTECTED_PATH.startsWith(normalizedPath + "/");
}

// ✅ Kiểm tra xem tên folder có trong danh sách bảo vệ không
function isProtectedFolderName(folderName) {
  return PROTECTED_FOLDER_NAMES.has(folderName);
}

// ✅ BƯỚC 1: Build folder mappings
function buildFolderMappings(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      if (PROTECTED_FOLDER_AND_FILE.has(item)) {
        continue;
      }
      // ✅ Bỏ qua protected path
      if (isProtectedPath(fullPath)) {
        console.log(`🔒 Protected root: ${item}`);
        buildFolderMappings(fullPath);
        continue;
      }

      // ✅ Bỏ qua parent của protected path
      if (isParentOfProtectedPath(fullPath)) {
        console.log(`🔒 Protected parent: ${item}`);
        buildFolderMappings(fullPath);
        continue;
      }

      // ✅ Bỏ qua folder có tên được bảo vệ
      if (isProtectedFolderName(item)) {
        console.log(`🔒 Protected folder name: ${item} (but will scan inside)`);
        buildFolderMappings(fullPath); // Vẫn quét bên trong
        continue;
      }

      // ✅ Tạo mapping cho folder
      if (!folderMappings.has(item)) {
        const newName = hashName(item, "d");
        folderMappings.set(item, newName);
        console.log(`📁 Folder: ${item} -> ${newName}`);
      }

      // Đệ quy
      buildFolderMappings(fullPath);
    }
  }
}

// ✅ Lưu mappings ra file
function saveMappings() {
  const output = {
    metadata: {
      protectedPackageRoot: PROTECTED_PACKAGE_ROOT,
      protectedFolderNames: Array.from(PROTECTED_FOLDER_NAMES).sort(),
      timestamp: new Date().toISOString(),
      totalFolders: folderMappings.size,
      totalFiles: fileMappings.size,
      totalPackages: packageMappings.size,
    },
    folders: Object.fromEntries(folderMappings),
    files: Object.fromEntries(fileMappings),
    packages: Object.fromEntries(packageMappings),
  };

  fs.writeFileSync(outputFile, JSON.stringify(output, null, 2));
  console.log(`\n✅ Mappings saved to: ${outputFile}`);
}

// ✅ BƯỚC 2: Build file mappings (đơn giản: đổi tất cả)
function buildFileMappings(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);
  console.log("items", items);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      if (!PROTECTED_FOLDER_AND_FILE.has(item)) {
        buildFileMappings(fullPath);
      }
    } else if (/\.(kt|java)$/i.test(item)) {
      const ext = path.extname(item);
      const fileBase = path.basename(item, ext);

      console.log("fileBase", fileBase, fileMappings.has(fileBase));
      // ✅ Tạo mapping cho file (không check gì cả, đổi hết)
      if (!fileMappings.has(fileBase)) {
        const newFileBase = hashName(fileBase, "F", true);
        fileMappings.set(fileBase, newFileBase);
        console.log(`📄 File: ${fileBase}${ext} -> ${newFileBase}${ext}`);
      }
    }
  }
}

// ✅ BƯỚC 3: Build package mappings
function buildPackageMappings() {
  console.log("\n📦 Building package mappings...\n");

  function scanPackages(dir) {
    if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
      return;
    }

    const items = fs.readdirSync(dir);

    for (const item of items) {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);

      if (stat.isDirectory()) {
        scanPackages(fullPath);
      } else if (/\.(kt|java)$/i.test(item)) {
        const content = fs.readFileSync(fullPath, "utf8");
        const packageMatch = content.match(/^package\s+([\w.]+)/m);

        if (packageMatch) {
          const oldPackage = packageMatch[1];
          const newPackage = convertPackagePath(oldPackage);

          if (oldPackage !== newPackage && !packageMappings.has(oldPackage)) {
            packageMappings.set(oldPackage, newPackage);
            console.log(`📦 Package: ${oldPackage}`);
            console.log(`       -> ${newPackage}`);
          }
        }
      }
    }
  }

  scanPackages(projectRoot);
}

// ✅ Convert package path sang tên mới
function convertPackagePath(packagePath) {
  const parts = packagePath.split(".");
  const newParts = [];

  const protectedParts = PROTECTED_PACKAGE_ROOT.split(".");

  for (let i = 0; i < parts.length; i++) {
    const part = parts[i];

    // ✅ Giữ nguyên protected root
    if (i < protectedParts.length && part === protectedParts[i]) {
      newParts.push(part);
      continue;
    }

    // ✅ Giữ nguyên protected folder names
    if (isProtectedFolderName(part)) {
      newParts.push(part);
      continue;
    }

    // Đổi theo mapping
    const mappedPart = folderMappings.get(part) || part;
    newParts.push(mappedPart);
  }

  return newParts.join(".");
}

// ✅ BƯỚC 4: Rename folders (từ sâu nhất ra ngoài)
function renameFolders(dir, renamedPaths = new Set()) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);

  // Đệ quy trước (depth-first)
  for (const item of items) {
    const fullPath = path.join(dir, item);
    if (fs.statSync(fullPath).isDirectory()) {
      renameFolders(fullPath, renamedPaths);
    }
  }

  // Rename folder hiện tại
  const dirName = path.basename(dir);
  const parentDir = path.dirname(dir);

  // ✅ Kiểm tra protected conditions
  if (
    isProtectedPath(dir) ||
    isParentOfProtectedPath(dir) ||
    isProtectedFolderName(dirName)
  ) {
    return; // Không đổi tên folder này
  }

  if (folderMappings.has(dirName) && !renamedPaths.has(dir)) {
    const newName = folderMappings.get(dirName);
    const newPath = path.join(parentDir, newName);

    try {
      fs.renameSync(dir, newPath);
      renamedPaths.add(newPath);
      console.log(`✅ Renamed folder: ${dirName} -> ${newName}`);
    } catch (error) {
      console.error(`❌ Error renaming folder ${dirName}:`, error.message);
    }
  }
}

// ✅ BƯỚC 5: Rename files
function renameFiles(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      if (!PROTECTED_FOLDER_AND_FILE.has(item)) {
        renameFiles(fullPath);
      }
    } else if (/\.(kt|java)$/i.test(item)) {
      const ext = path.extname(item);
      const fileBase = path.basename(item, ext);

      if (fileMappings.has(fileBase)) {
        const newFileBase = fileMappings.get(fileBase);
        const newFileName = newFileBase + ext;
        const newPath = path.join(path.dirname(fullPath), newFileName);

        try {
          fs.renameSync(fullPath, newPath);
          console.log(`✅ Renamed file: ${item} -> ${newFileName}`);
        } catch (error) {
          console.error(`❌ Error renaming file ${item}:`, error.message);
        }
      }
    }
  }
}

// ✅ BƯỚC 6: Update package declarations
function updatePackageDeclarations(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      updatePackageDeclarations(fullPath);
    } else if (/\.(kt|java)$/i.test(item)) {
      let content = fs.readFileSync(fullPath, "utf8");
      let modified = false;

      // Update package declaration
      for (const [oldPkg, newPkg] of packageMappings) {
        const pkgRegex = new RegExp(
          `^(package\\s+)${oldPkg.replace(/\./g, "\\.")}\\b`,
          "m"
        );

        if (pkgRegex.test(content)) {
          content = content.replace(pkgRegex, `$1${newPkg}`);
          modified = true;
          console.log(`  📦 ${item}: ${oldPkg} -> ${newPkg}`);
        }
      }

      if (modified) {
        fs.writeFileSync(fullPath, content, "utf8");
      }
    }
  }
}

// ✅ BƯỚC 7: Update imports và usage (simplified aggressive approach)
// (Trước đây file này có 2 định nghĩa `updateImports` trùng tên — bản đầu bị bản thứ 2 "đè" theo
// hoisting của JS nên KHÔNG BAO GIỜ chạy, dọn bỏ bản chết đó, chỉ giữ bản dưới đây.)
function updateImports(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      updateImports(fullPath);
    } else if (/\.(kt|java)$/i.test(item)) {
      let content = fs.readFileSync(fullPath, "utf8");
      let modified = false;

      // ✅ Bảo vệ `import <BASE_NAMESPACE>.R` — R là class tài nguyên do AGP sinh ra dưới
      // `namespace` khai báo trong build.gradle.kts (KHÔNG đổi khi rename source), nếu PASS 1 dưới
      // đây blanket-replace theo package source đã đổi thì import sẽ trỏ tới package không tồn
      // tại → "Unresolved reference 'R'" toàn bộ file dùng R.string/R.drawable.
      const rImportRegex = new RegExp(
        `^import\\s+${PROTECTED_PACKAGE_ROOT.replace(/\./g, "\\.")}\\.R\\s*$`,
        "gm"
      );
      const rImportMatches = content.match(rImportRegex) || [];
      const R_IMPORT_PLACEHOLDER = "//__PROTECTED_R_IMPORT_PLACEHOLDER__//";
      if (rImportMatches.length) {
        content = content.replace(rImportRegex, R_IMPORT_PLACEHOLDER);
      }

      // PASS 1: Update package paths trong imports
      for (const [oldPkg, newPkg] of packageMappings) {
        const importRegex = new RegExp(
          `^(import\\s+)${oldPkg.replace(/\./g, "\\.")}(\\.|$)`,
          "gm"
        );

        if (importRegex.test(content)) {
          content = content.replace(
            new RegExp(`^(import\\s+)${oldPkg.replace(/\./g, "\\.")}`, "gm"),
            `$1${newPkg}`
          );
          modified = true;
        }
      }

      if (rImportMatches.length) {
        content = content.replace(
          new RegExp(R_IMPORT_PLACEHOLDER, "g"),
          rImportMatches[0]
        );
      }

      // PASS 1b: Update tham chiếu full-path GIỮA DÒNG CODE (không phải "import ..." ở đầu dòng).
      // Bug đã gặp: code gọi thẳng bằng đường dẫn đủ thay vì import, vd
      // `com.camera.studio.photoeditor.ui.shared.editor.someFunction(...)` — PASS 1 ở trên CHỈ khớp
      // dòng bắt đầu bằng "import", nên các package con (shared/editor) đã bị đổi tên folder trên đĩa
      // nhưng đoạn full-path literal này không được cập nhật theo → "Unresolved reference". Sắp xếp
      // packageMappings theo độ dài GIẢM DẦN trước khi thay để tránh 1 package cha ngắn hơn thay
      // nhầm/thay thiếu vào giữa 1 package con dài hơn còn chưa được xử lý.
      const sortedPackageMappings = [...packageMappings].sort(
        (a, b) => b[0].length - a[0].length
      );
      for (const [oldPkg, newPkg] of sortedPackageMappings) {
        // Lookahead "(?=\\.[A-Za-z_])" đảm bảo chỉ khớp khi oldPkg là TIỀN TỐ của 1 tham chiếu dài hơn
        // (vd "oldPkg.SomeClass"), không khớp nhầm khi oldPkg đứng 1 mình hoặc là hậu tố của từ khác.
        const inlineRegex = new RegExp(
          `\\b${oldPkg.replace(/\./g, "\\.")}(?=\\.[A-Za-z_])`,
          "g"
        );
        if (inlineRegex.test(content)) {
          content = content.replace(inlineRegex, newPkg);
          modified = true;
        }
      }

      // PASS 2: Update class names trong imports
      for (const [oldFileName, newFileName] of fileMappings) {
        const classImportRegex = new RegExp(
          `^(import\\s+[\\w.]+\\.)${oldFileName}$`,
          "gm"
        );

        if (classImportRegex.test(content)) {
          content = content.replace(classImportRegex, `$1${newFileName}`);
          modified = true;
        }
      }

      // PASS 3: Replace TẤT CẢ class names trong code
      // (bao gồm cả những class trong cùng package không cần import)
      for (const [oldFileName, newFileName] of fileMappings) {
        // Chỉ replace nếu class này thực sự được dùng trong file
        const usageRegex = new RegExp(`\\b${oldFileName}\\b`, "g");

        if (usageRegex.test(content)) {
          const beforeReplace = content;
          content = content.replace(usageRegex, (newName) => {
            // Không replace nếu nằm trong string literal
            // (kiểm tra đơn giản)
            return newFileName;
          });

          if (beforeReplace !== content) {
            modified = true;
            console.log(`    ↳ ${oldFileName} -> ${newFileName}`);
          }
        }
      }

      if (modified) {
        fs.writeFileSync(fullPath, content, "utf8");
        console.log(`  📥 ${item}: updated`);
      }
    }
  }
}

function updateManifest(file) {
  let content = fs.readFileSync(file, "utf8");
  console.log("content", content);
  for (const [oldFileName, newFileName] of fileMappings) {
    // Chỉ replace nếu class này thực sự được dùng trong file
    const usageRegex = new RegExp(`\\b${oldFileName}\\b`, "g");

    if (usageRegex.test(content)) {
      const beforeReplace = content;
      content = content.replace(usageRegex, (newName) => {
        // Không replace nếu nằm trong string literal
        // (kiểm tra đơn giản)
        return newFileName;
      });

      if (beforeReplace !== content) {
        modified = true;
        console.log(`    ↳ ${oldFileName} -> ${newFileName}`);
      }
    }
  }

  // ✅ BUG THẬT đã tìm ra (2026-07-08): vòng lặp trên chỉ đổi tên FILE/CLASS trong Manifest —
  // KHÔNG đổi các đoạn FOLDER đã bị đổi tên (vd folder "intro" -> "d263e9" khi
  // android:name=".ui.intro.F34502f") — Manifest và class thật LỆCH NHAU
  // ("ui.intro.F34502f" không tồn tại, thật ra nằm ở "ui.d263e9.F34502f") gây
  // ActivityNotFoundException lúc runtime (app tự đóng/ẩn khi start Intent tường minh).
  //
  // Chỉ thay đoạn folder BÊN TRONG android:name="..." mà giá trị bắt đầu bằng "." (dạng rút gọn
  // ngụ ý gói app) hoặc bằng đúng BASE_NAMESPACE (gói app đầy đủ) — TUYỆT ĐỐI KHÔNG thay
  // android:name="..." của thư viện ngoài (vd "com.google.android.gms.ads.AdActivity" do AdMob tự
  // merge vào Manifest) vì có thể trùng tên 1 folder app mình đã đổi (vd folder "ads" của app này
  // đổi thành "ad877d" — nếu thay mù toàn file sẽ phá luôn khai báo AdActivity của AdMob).
  const basePkgEscaped = config.BASE_NAMESPACE.replace(/\./g, "\\.");
  const ownNameAttrRegex = new RegExp(
    `android:name="(?:\\.[^"]*|${basePkgEscaped}[^"]*)"`,
    "g"
  );
  content = content.replace(ownNameAttrRegex, (attrMatch) => {
    let updated = attrMatch;
    for (const [oldFolderName, newFolderName] of folderMappings) {
      const segRegex = new RegExp(`\\.${oldFolderName}\\.`, "g");
      updated = updated.replace(segRegex, `.${newFolderName}.`);
    }
    if (updated !== attrMatch) {
      modified = true;
      console.log(`    ↳ (manifest package path) ${attrMatch} -> ${updated}`);
    }
    return updated;
  });

  fs.writeFileSync(file, content, "utf8");
}
// ✅ BƯỚC 8: Update class names trong file (nếu trùng tên file)
function updateClassNamesInFiles(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return;
  }

  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      updateClassNamesInFiles(fullPath);
    } else if (/\.(kt|java)$/i.test(item)) {
      let content = fs.readFileSync(fullPath, "utf8");
      const ext = path.extname(item);
      const currentFileBase = path.basename(item, ext);

      // Tìm tên file cũ từ mapping ngược lại
      let oldFileBase = null;
      for (const [old, newName] of fileMappings) {
        if (newName === currentFileBase) {
          oldFileBase = old;
          break;
        }
      }

      if (!oldFileBase) continue;

      let modified = false;

      // Update class/object có tên trùng với tên file cũ
      const classRegex = new RegExp(
        `\\b(class|object)\\s+${oldFileBase}\\b`,
        "g"
      );

      if (classRegex.test(content)) {
        content = content.replace(classRegex, `$1 ${currentFileBase}`);
        modified = true;
        console.log(
          `  🔧 ${item}: updated class/object name ${oldFileBase} -> ${currentFileBase}`
        );
      }

      if (modified) {
        fs.writeFileSync(fullPath, content, "utf8");
      }
    }
  }
}

function removeEmptyFolders(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    return false;
  }

  let items = fs.readdirSync(dir);
  let isEmpty = true;

  // Đệ quy vào các subfolder trước
  for (const item of items) {
    const fullPath = path.join(dir, item);

    if (fs.statSync(fullPath).isDirectory()) {
      // Nếu subfolder không bị xóa (không rỗng), thì folder cha cũng không rỗng
      if (!removeEmptyFolders(fullPath)) {
        isEmpty = false;
      }
    } else {
      // Có file -> không rỗng
      isEmpty = false;
    }
  }

  // Nếu folder rỗng, xóa nó
  if (isEmpty && dir !== projectRoot) {
    try {
      fs.rmdirSync(dir);
      console.log(`🗑️  Removed empty folder: ${path.basename(dir)}`);
      return true;
    } catch (error) {
      console.error(`❌ Error removing ${dir}:`, error.message);
      return false;
    }
  }

  return false;
}

// MAIN
(function main() {
  console.log("🚀 Bắt đầu rename folders, files và packages...\n");
  console.log(`🔒 Protected package root: ${PROTECTED_PACKAGE_ROOT}`);
  console.log(
    `🔒 Protected folder names: ${Array.from(PROTECTED_FOLDER_NAMES).join(
      ", "
    )}\n`
  );

  // xóa thư mục trống
  removeEmptyFolders(projectRoot);
  // BƯỚC 1: Build folder mappings
  console.log("📁 Step 1: Building folder mappings...\n");
  buildFolderMappings(projectRoot);

  // BƯỚC 2: Build file mappings
  console.log("\n📄 Step 2: Building file mappings...\n");
  buildFileMappings(projectRoot);
  console.log("fileMappings", fileMappings);

  // BƯỚC 3: Build package mappings
  console.log("\n📦 Step 3: Building package mappings...\n");
  buildPackageMappings();

  // BƯỚC 4: Rename folders
  console.log("\n🔄 Step 4: Renaming folders...\n");
  renameFolders(projectRoot);

  // BƯỚC 5: Rename files
  console.log("\n🔄 Step 5: Renaming files...\n");
  renameFiles(projectRoot);

  // BƯỚC 6: Update package declarations
  console.log("\n📦 Step 6: Updating package declarations...\n");
  updatePackageDeclarations(projectRoot);

  // BƯỚC 7: Update imports
  console.log("\n📥 Step 7: Updating imports...\n");
  updateImports(projectRoot);

  // BƯỚC 8: Update class names trong file
  console.log("\n🔧 Step 8: Updating class/object names...\n");
  updateClassNamesInFiles(projectRoot);

  updateManifest(projectManifest);
  // Save mappings
  saveMappings();
})();
