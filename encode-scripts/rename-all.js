// rename-all.js — LÕI DÙNG CHUNG, không sửa khi clone app mới.
// Chạy toàn bộ pipeline mã hóa code bằng 1 lệnh: `node rename-all.js`.
// Mỗi bước chạy như 1 tiến trình con (giữ nguyên hành vi khi chạy lẻ `node <script>.js`).
// Dừng ngay khi 1 bước lỗi. Bật/tắt từng bước qua rename-config-global.js -> pipeline.
//
// THỨ TỰ CỐ ĐỊNH — ĐÃ CHỨNG MINH ĐÚNG QUA push.sh THẬT, ĐỪNG ĐẢO:
// rename-file.js PHẢI chạy TRƯỚC rename-code-symbols.js. rename-file.js có bước
// `updateClassNamesInFiles()` đồng bộ lại tên class/object cho khớp tên file mới — bước này tìm
// class theo TÊN CŨ trong nội dung file; nếu rename-code-symbols chạy trước và đã đổi tên
// class/object thành hash, `updateClassNamesInFiles()` sẽ không tìm thấy tên cũ nữa → mất đồng bộ
// tên class/file.
const { execSync } = require("child_process");
const config = require("./rename-config-global");

const pipeline = config.pipeline || {};

const STEPS = [
  { key: "renameDrawables", script: "rename-drawables.js", label: "Đổi tên drawable" },
  { key: "renameLanguages", script: "rename-languages.js", label: "Đổi key ngôn ngữ" },
  { key: "renameConfigKey", script: "rename-config-key.js", label: "Đổi key Firebase (config.xml)" },
  { key: "renameFile", script: "rename-file.js", label: "Đổi tên folder/file/package (PHẢI trước renameCodeSymbols)" },
  { key: "renameCodeSymbols", script: "rename-code-symbols.js", label: "Đổi tên class/object/hàm" },
];

// Mặc định khi 1 key không được khai báo trong config.pipeline: các bước rename chính BẬT,
// renameConfigKey TẮT cho an toàn (xem giải thích trong rename-config-global.js).
const DEFAULT_ENABLED = {
  renameDrawables: true,
  renameLanguages: true,
  renameConfigKey: false,
  renameFile: true,
  renameCodeSymbols: true,
};

function isEnabled(key) {
  if (Object.prototype.hasOwnProperty.call(pipeline, key)) return !!pipeline[key];
  return DEFAULT_ENABLED[key];
}

function fmt(ms) {
  return (ms / 1000).toFixed(1) + "s";
}

(function main() {
  const selected = STEPS.filter((s) => isEnabled(s.key));

  if (selected.length === 0) {
    console.log("⚠️  Không có bước nào được bật trong config.pipeline. Không có gì để chạy.");
    return;
  }

  console.log(`🚀 rename-all: pipeline gồm ${selected.length} bước:\n`);
  selected.forEach((s, i) => console.log(`   ${i + 1}. ${s.label}  (${s.script})`));

  const t0 = Date.now();
  for (let i = 0; i < selected.length; i++) {
    const s = selected[i];
    const bar = "=".repeat(70);
    console.log(
      `\n${bar}\n▶️  BƯỚC ${i + 1}/${selected.length}: ${s.label}  —  node ${s.script}\n${bar}`
    );

    const start = Date.now();
    try {
      execSync(`node "${s.script}"`, { stdio: "inherit", cwd: __dirname });
    } catch (e) {
      console.error(
        `\n❌ THẤT BẠI ở bước ${i + 1}: ${s.label} (${s.script}).` +
          `\n   Pipeline dừng lại. Sửa lỗi rồi chạy lại, hoặc 'node reverse.js' để back.`
      );
      process.exit(e.status || 1);
    }
    console.log(`✅ Xong bước ${i + 1} (${fmt(Date.now() - start)})`);
  }

  console.log(`\n🎉 HOÀN TẤT toàn bộ pipeline trong ${fmt(Date.now() - t0)}.`);
  if (isEnabled("renameConfigKey")) {
    console.log("⚠️  Đã bật đổi key Firebase — nhớ upload file config mới lên Firebase.");
  }
  console.log("ℹ️  Build/test xong, dùng 'node reverse.js' để back lại code gốc.");
})();
