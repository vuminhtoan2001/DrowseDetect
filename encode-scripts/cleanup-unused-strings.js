/**
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
const stringsToRemove = new Set([
  "ADDRESS",
  "ADD_TO_CONTACTS",
  "ADD_TO_EVENTS",
  "ALLDAY",
  "APPLY",
  "ARTIST_NAME",
  "AUTO_COPY_TO_CLIPBOARD",
  "BIRTHDAY",
  "CALENDAR",
  "CALL",
  "CANCEL",
  "CHANNEL_ID",
  "CLEAR_FILTER",
  "CLEAR_HISTORY",
  "CLIPBOARD",
  "CONNECT_TO_WIFI",
  "CONTACTS",
  "CONTACT_US",
  "CONTENT",
  "COPY",
  "COPY_PASSWORD",
  "COPY_TO_CLIPBOARD",
  "CREATE",
  "CUSTOMIZE",
  "CUSTOMIZE_AND_SAVE",
  "CUSTOMIZE_QRCODE",
  "ConnectionFailed",
  "ConnectionSuccessful",
  "DATA_EMPTY",
  "DESCRIPTION",
  "DONE",
  "EMAIL",
  "EMAIL_ADDRESS",
  "END",
  "ENTER",
  "FACEBOOK_ID",
  "FACEBOOK_PROFILE",
  "FEEDBACK",
  "FILTER_HISTORY",
  "GENERAL_SETTINGS",
  "HELP",
  "HISTORY",
  "INSTAGRAM",
  "INVALID_PHONE",
  "LINK",
  "MAXIMUM_MESSAGE",
  "ME_LINK",
  "ME_USERNAME",
  "MYCARD",
  "NAME",
  "NETWORK_NAME",
  "NO",
  "NONE",
  "NOTE",
  "NOTIFICATION",
  "NOT_FIND_QRCODE",
  "OPEN_BROWSER",
  "ORG",
  "PASSWORD",
  "PAYPAL",
  "PHONE_NUMBER",
  "PLEASE_ENTER_SOMETHING",
  "Personal",
  "SAVE",
  "SAVE_CUSTOMIZE",
  "SAVE_IMAGE",
  "SAVE_OPTION",
  "SCAN",
  "SECURITY_MODE",
  "SEND_EMAIL",
  "SEND_SMS",
  "SHARE",
  "SHOW_ON_MAP",
  "SMS",
  "SONG_NAME",
  "SPOTIFY",
  "START",
  "SUB_DELETE",
  "Social",
  "TAP_HERE_TO_CHANGE",
  "TEL",
  "TEXT",
  "TEXT_EMPTY_FORM",
  "TEXT_MESSAGE",
  "TITLE",
  "TITLE_CREATE",
  "TWITTER",
  "USERNAME",
  "Utilitles",
  "VIBER",
  "VIDEO_ID",
  "WEBSITE",
  "WiFI",
  "YES",
  "YOUTUBE_URL",
  "af84d5",
  "autoOpen",
  "autoScan",
  "autoScanSub",
  "aw1",
  "aw2",
  "aw3",
  "aw4",
  "aw5",
  "cancelsubscriptions",
  "cb0af4",
  "close",
  "delete_history_success",
  "duplicateCode",
  "duplicateCodeSub",
  "e6a423",
  "egwthe",
  "export",
  "f5776c5",
  "g672873",
  "g6dd514",
  "i874505",
  "i8f46e6",
  "import_str",
  "j9c4c14",
  "layout_button_send",
  "layout_do_you_like",
  "layout_language_header",
  "layout_thanks",
  "ledDark",
  "light",
  "lightOrDark",
  "loadingAds",
  "lowContrast",
  "manualScan",
  "manualScanSub",
  "mm_deleted",
  "mm_ok",
  "mm_saved",
  "newVersion_isReady_title",
  "noPerCamera",
  "note",
  "perCamera",
  "perCameraSub",
  "policy",
  "requestCamera",
  "saved_customize_success",
  "saved_image_success",
  "saving_image",
  "scanControl",
  "scanImage",
  "scanningTips",
  "select_country",
  "selected",
  "share_cancel",
  "share_image_success",
  "share_success",
  "skip",
  "textSplash",
  "tip0",
  "tip90",
  "tipOther",
  "titleuninstall",
  "tooClose",
  "uninstall",
  "update_isReady_mss",
  "vibrate"
]);

console.log(`🗑️ Sẽ xóa ${stringsToRemove.size} unused strings...`);

// Xóa strings từ các file strings.xml
function cleanupStringsXml() {
  const pattern = path.join(RES_DIR, "values*/strings.xml").replace(/\\/g, "/");
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
            console.log(`  🗑️ Xóa: ${key}`);
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
            console.log(`  🗑️ Xóa array: ${key}`);
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
            console.log(`  🗑️ Xóa plural: ${key}`);
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
        fs.writeFileSync(filePath, newXml.trim() + "\n", "utf8");
        console.log(`✅ Đã cập nhật: ${filePath}`);
      }

    } catch (error) {
      console.error(`❌ Lỗi khi xử lý ${filePath}:`, error.message);
    }
  }
}

console.log("⚠️  BACKUP các file đã được tạo với extension .backup");
console.log("🚀 Bắt đầu dọn dẹp...");
cleanupStringsXml();
console.log("✨ Hoàn tất!");
