/**
 * Gộp thành 1 file cuối `firebase_key.json` để upload Firebase Remote Config.
 * Union 3 nguồn, KHÔNG trùng key:
 *   (1) firebase_old_key.json      — bản React-Native cũ (đã mã hoá theo RN), giữ nguyên.
 *   (2) Kotlin mới (đã mã hoá)     — key config.xml sau khi hash (từ keychangeConfigFirebase.json),
 *                                    value lấy từ config.xml ĐÃ ENCODE; mỗi param có description
 *                                    ghi chú "[KOTLIN] <tên thật>".
 *   (3) fire_base_key_lib_arr.json — key cho thư viện AAR.
 * Trùng key → giữ nguồn xuất hiện trước (old → lib → kotlin).
 *
 * CHẠY SAU bước encode của push.sh (cần config.xml đã encode + keychangeConfigFirebase.json):
 *   node encode-scripts/firebase-merge.js
 */
const fs = require("fs");
const path = require("path");
const { XMLParser } = require("fast-xml-parser");
const config = require("./rename-config-global");

const ROOT = process.cwd();
const OUT = path.join(ROOT, "firebase_key.json");

function readJson(f) {
  const p = path.join(ROOT, f);
  if (!fs.existsSync(p)) { console.warn("[merge] bỏ qua (không có): " + f); return null; }
  return JSON.parse(fs.readFileSync(p, "utf8"));
}

// Group/param cần BỎ khi lấy từ bản RN cũ (yêu cầu): key log + key custom text bottom.
const DROP_GROUPS = new Set(["log_item_ad"]);
function isDropped(key, param) {
  const d = String((param && param.description) || "").toLowerCase();
  if (d.includes("hiển thị log") || d.includes("custom text") || d.includes("customtext")) return true;
  if (String(key).toLowerCase().startsWith("customtext")) return true;
  return false;
}

/** Gom mọi parameter (top-level + trong parameterGroups) thành 1 map phẳng.
 *  [dropLogCustom]=true: bỏ group log + param log/customtext (dùng cho bản RN cũ). */
function collectParams(obj, dropLogCustom = false) {
  const out = {};
  if (!obj) return out;
  const add = (params) => {
    for (const [k, v] of Object.entries(params)) {
      if (dropLogCustom && isDropped(k, v)) continue;
      out[k] = v;
    }
  };
  if (obj.parameters) add(obj.parameters);
  if (obj.parameterGroups) {
    for (const [gname, g] of Object.entries(obj.parameterGroups)) {
      if (dropLogCustom && DROP_GROUPS.has(gname)) continue;
      if (g && g.parameters) add(g.parameters);
    }
  }
  return out;
}

function inferType(v) {
  const s = String(v).trim();
  if (s === "true" || s === "false") return "BOOLEAN";
  if (/^-?\d+$/.test(s)) return "NUMBER";
  if ((s.startsWith("[") && s.endsWith("]")) || (s.startsWith("{") && s.endsWith("}"))) return "JSON";
  return "STRING";
}

// (2) Kotlin: đọc TẤT CẢ entry trong config.xml. Nếu key có mã hoá (keychangeConfigFirebase.json)
// thì dùng tên hash, KHÔNG có thì giữ readable (config key hiện KHÔNG hash — xem push.sh).
function buildKotlinParams() {
  const xmlPath = path.join(ROOT, config.srcCodePath, config.configFirebaseFile);
  if (!fs.existsSync(xmlPath)) {
    console.warn("[merge] thiếu config.xml → bỏ qua phần Kotlin");
    return {};
  }
  const mapPath = path.join(ROOT, config.firebaseOutputFile); // keychangeConfigFirebase.json {real:hash}
  const map = fs.existsSync(mapPath) ? JSON.parse(fs.readFileSync(mapPath, "utf8")) : {};
  const xml = fs.readFileSync(xmlPath, "utf8");
  const parsed = new XMLParser({ ignoreAttributes: false, textNodeName: "#text", parseTagValue: false }).parse(xml);
  let entries = parsed.defaultsMap && parsed.defaultsMap.entry;
  if (!entries) return {};
  if (!Array.isArray(entries)) entries = [entries];
  const out = {};
  for (const e of entries) {
    const real = typeof e.key === "string" ? e.key : (e.key && e.key["#text"]);
    const value = typeof e.value === "string" ? e.value : (e.value && e.value["#text"] !== undefined ? e.value["#text"] : "");
    if (real == null) continue;
    const outKey = map[real] || real; // hash nếu có mapping, else readable
    out[outKey] = {
      defaultValue: { value: String(value) },
      description: `[KOTLIN] ${real}`, // ghi chú mục (2): tên thật của key Kotlin
      valueType: inferType(value),
    };
  }
  return out;
}

const old = collectParams(readJson("firebase_old_key.json"), /* dropLogCustom */ true);
const lib = collectParams(readJson("fire_base_key_lib_arr.json"));
const kotlin = buildKotlinParams();

// Union, không trùng key (nguồn trước thắng)
const merged = {};
const addAll = (src) => { for (const [k, v] of Object.entries(src)) if (!(k in merged)) merged[k] = v; };
addAll(old);
addAll(lib);
addAll(kotlin);

// ads_config (key lib) — value = nội dung default_ads_config.json (ad unit THẬT). Placement
// bên trong KHÔNG bị mã hoá → giữ readable (lib + app Kotlin đều đọc getAdsConfigValue theo tên này).
const adsCfgPath = path.join(ROOT, config.srcCodePath, "assets", "default_ads_config.json");
if (fs.existsSync(adsCfgPath)) {
  const adsCfg = JSON.parse(fs.readFileSync(adsCfgPath, "utf8"));
  merged["ads_config"] = {
    defaultValue: { value: JSON.stringify(adsCfg) },
    description: "[LIB] ads_config — ad unit ids (default_ads_config.json)",
    valueType: "JSON",
  };
  console.log(`[firebase-merge] set ads_config = default_ads_config.json (${Object.keys(adsCfg).length} placements)`);
} else {
  console.warn("[firebase-merge] KHÔNG thấy default_ads_config.json → bỏ qua ads_config");
}

fs.writeFileSync(OUT, JSON.stringify({ parameters: merged }, null, 2), "utf8");
console.log(`[firebase-merge] old=${Object.keys(old).length} lib=${Object.keys(lib).length} kotlin=${Object.keys(kotlin).length} +ads_config → tổng ${Object.keys(merged).length} → firebase_key.json`);
