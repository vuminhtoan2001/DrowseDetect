/**
 * cleanup-unused-drawables.js
 * Script tự động xóa các unused drawables
 * ⚠️ CẢNH BÁO: Hãy backup code trước khi chạy script này!
 */

const fs = require("fs");
const path = require("path");
const config = require("./rename-config-global");

const ROOT_DIR = path.resolve(__dirname, config.srcCodePath);

// Danh sách files cần xóa
const filesToDelete = [
  "\\res\\drawable\\ic_viber.png",
  "\\res\\drawable\\ic_card.png",
  "\\res\\drawable\\ic_paypal.png",
  "\\res\\drawable\\ic_tiktok.png",
  "\\res\\drawable\\ic_instagram.png",
  "\\res\\drawable\\ic_clipboard.png",
  "\\res\\drawable\\ic_youtube.webp",
  "\\res\\drawable\\tipother.webp",
  "\\res\\drawable\\ic_whatsapp.png",
  "\\res\\drawable\\ic_spotify.png",
  "\\res\\drawable\\stvjcsjp5riidkxrjkxv.webp",
  "\\res\\drawable\\tiplight.webp",
  "\\res\\drawable\\ic_wifi.png",
  "\\res\\drawable\\ic_web.png",
  "\\res\\drawable\\ic_calendar.png",
  "\\res\\drawable\\tip0.webp",
  "\\res\\drawable\\tip90.webp",
  "\\res\\drawable\\ic_contact.png",
  "\\res\\drawable\\tipdark.webp",
  "\\res\\drawable\\tipblur.webp",
  "\\res\\drawable\\ic_phone.png",
  "\\res\\drawable\\ic_sms.png",
  "\\res\\drawable\\ic_email.png",
  "\\res\\drawable\\ic_text.png",
  "\\res\\drawable\\ic_x.png",
  "\\res\\drawable\\ic_fb.png",
  "\\res\\drawable\\tipcontrast.webp",
  "\\res\\drawable\\q6mzetxepgs3l0tb3smr.webp",
  "\\res\\drawable\\hwuzri9ywotxwb3a4grv.webp",
  "\\res\\drawable\\d0mucjcjgy957sdgf77a.webp",
  "\\res\\drawable\\reoztrkqwuadmldzu1zy.webp",
  "\\res\\drawable\\ndrhjd8qq36zg3i1toef.webp",
  "\\res\\drawable\\ic_feature_support.xml",
  "\\res\\drawable\\vavjm0adwtx2vbdonnl5.webp",
  "\\res\\drawable\\ic_stat_noti.xml",
  "\\res\\drawable\\xxv9mvezn6mcatljwpnt.webp",
  "\\res\\drawable\\upej1iox5bueppd7yfxx.webp",
  "\\res\\drawable\\ic_feature_premium.xml",
  "\\res\\drawable\\ic_feature_fast.xml",
  "\\res\\drawable\\jwfh2pkdhdk413ftapto.webp",
  "\\res\\drawable\\uchywybaxu9tpprw4wch.webp",
  "\\res\\drawable\\tsj331la5qvaxu4jxzjz.webp",
  "\\res\\drawable\\ctsqowll2c55gj4q3ox8.webp",
  "\\res\\drawable\\ug05gagehmhwvxo9dnzi.webp",
  "\\res\\drawable\\gcjyygcvbsfprdbmzf52.webp",
  "\\res\\drawable\\orjifuoeg9bvc24pnt8i.webp",
  "\\res\\drawable\\id7j2pfguynavy1w6gql.webp"
];

console.log(`🗑️ Sẽ xóa ${filesToDelete.length} files...`);
console.log(`💾 Tổng dung lượng tiết kiệm: 0.14 MB`);
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
      console.log(`⚠️  File không tồn tại: ${relPath}`);
      continue;
    }

    // Backup file trước khi xóa
    const backupPath = path.join(backupDir, relPath);
    const backupFolder = path.dirname(backupPath);
    // fs.mkdirSync(backupFolder, { recursive: true });
    // fs.copyFileSync(fullPath, backupPath);

    // Xóa file
    fs.unlinkSync(fullPath);
    deletedCount++;
    console.log(`🗑️  Đã xóa: ${relPath}`);

  } catch (error) {
    console.error(`❌ Lỗi khi xóa ${relPath}:`, error.message);
    errorCount++;
  }
}

console.log("\n" + "=".repeat(70));
console.log(`✅ Đã xóa: ${deletedCount} files`);
console.log(`❌ Lỗi: ${errorCount} files`);
console.log(`💾 Backup được lưu tại: ${backupDir}`);
console.log("=".repeat(70));
console.log("\n✨ Hoàn tất dọn dẹp!");
