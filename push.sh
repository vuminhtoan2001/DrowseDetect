#!/usr/bin/env bash
# push.sh - Đẩy code lên 2 remote
#
# ORIGIN     : tất cả file (kể cả encode-scripts/)
# BUILD_TOOL : encode → BUILD TEST → nếu OK thì mới xóa file và push
#              KHÔNG gồm: encode-scripts/, .md, drive-download/
#
# Cách dùng:
#   ./push.sh              # đẩy cả 2
#   ./push.sh origin
#   ./push.sh build_tool

set -euo pipefail

# ── Cấu hình ──────────────────────────────────────────────────────────────────

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS_DIR="$REPO_ROOT/encode-scripts"
WORK_BRANCH="androidsdkads"
PUSH_BRANCH="androidsdkads"
# build_tool đẩy sang nhánh main (nhánh default của repo build)
BUILD_PUSH_BRANCH="main"
TARGET="${1:-both}"

# LƯU Ý: KHÔNG hash config key (rename-config-key) vì app đọc key động
# (getBoolean("${placement}_enable"), getInt("${p}_ratio")) → hash sẽ làm release đọc trượt key
# → tắt hết ads. Giữ config key readable ở cả config.xml + code + firebase.
ENCODE_SCRIPTS=(
    rename-drawables.js
    rename-languages.js
    rename-file.js
    rename-code-symbols.js
)

# Tất cả file cần copy tạm ra root để chạy
TEMP_FILES=(
    rename-config-global.js
    rename-config-key.js
    rename-drawables.js
    rename-file.js
    rename-languages.js
    rename-code-symbols.js
    reverse.js
    package.json
)

EXCLUDED_FOLDER="drive-download-20260629T074041Z-3-001"

# ── Helpers ───────────────────────────────────────────────────────────────────

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step()  { echo -e "\n${CYAN}>>> $*${NC}"; }
ok()    { echo -e "    ${GREEN}OK: $*${NC}"; }
warn()  { echo -e "    ${YELLOW}WARN: $*${NC}"; }
die()   { echo -e "\n${RED}LỖI: $*${NC}"; exit 1; }

copy_scripts_to_root() {
    for f in "${TEMP_FILES[@]}"; do
        [ -f "$SCRIPTS_DIR/$f" ] && cp "$SCRIPTS_DIR/$f" "$REPO_ROOT/$f" \
            || warn "Không tìm thấy: $SCRIPTS_DIR/$f"
    done
}

remove_scripts_from_root() {
    for f in "${TEMP_FILES[@]}"; do rm -f "$REPO_ROOT/$f"; done
    [ -L "$REPO_ROOT/node_modules" ] && rm "$REPO_ROOT/node_modules" || true
}

temp_branch() { echo "temp/$1-$(date +%s)"; }

# ── Kiểm tra ──────────────────────────────────────────────────────────────────

cd "$REPO_ROOT"
[ -d "$SCRIPTS_DIR" ] || die "Không tìm thấy $SCRIPTS_DIR"
git status --porcelain | grep -qE '^[MADRC]' && die "Có staged changes chưa commit. Hãy commit trước."

# ── PUSH TO ORIGIN ────────────────────────────────────────────────────────────

push_origin() {
    step "PUSH TO ORIGIN"
    local tb; tb=$(temp_branch "origin")

    _cleanup() { git checkout "$WORK_BRANCH" 2>/dev/null || true; git branch -D "${tb:-}" 2>/dev/null || true; }
    trap _cleanup EXIT

    git checkout -b "$tb"
    git add --all

    if git status --porcelain | grep -q .; then
        git commit -m "sync: full snapshot for origin [auto]"
    else
        ok "Không có thay đổi."
    fi

    step "Đẩy lên origin/$PUSH_BRANCH"
    git push origin "${tb}:${PUSH_BRANCH}" --force-with-lease
    ok "origin/$PUSH_BRANCH ✓"

    _cleanup; trap - EXIT
}

# ── PUSH TO BUILD_TOOL ────────────────────────────────────────────────────────

push_build_tool() {
    step "PUSH TO BUILD_TOOL"
    local tb; tb=$(temp_branch "build")
    local encode_done=false

    # Luôn chạy khi thoát (kể cả lỗi)
    _cleanup() {
        # Nếu encode đã chạy mà chưa xóa → cần reverse.
        # Dùng ${var:-false} vì set -u: khi trap EXIT chạy sau khi hàm unwind, biến local
        # có thể "unbound" (từng làm cleanup chết → kẹt ở temp branch + code encode).
        if [ "${encode_done:-false}" = true ]; then
            echo -e "\n${RED}>>> Đang reverse code về bản gốc...${NC}"
            # keychange*.json vẫn còn ở root (chưa xóa) → reverse hoạt động được
            (cd "$REPO_ROOT" && node reverse.js) \
                && ok "Reverse hoàn tất — code đã khôi phục" \
                || warn "reverse.js có lỗi"
        fi
        remove_scripts_from_root
        # Xóa keychange*.json (output của encode, không push lên đâu)
        find "$REPO_ROOT" -maxdepth 1 -name "keychange*.json" -delete 2>/dev/null || true
        git checkout "$WORK_BRANCH" 2>/dev/null || true
        git branch -D "${tb:-}" 2>/dev/null || true
    }
    trap _cleanup EXIT

    git checkout -b "$tb"

    # ── BƯỚC 1: Cài npm ───────────────────────────────────────────────────────
    step "Cài npm packages"
    (cd "$SCRIPTS_DIR" && npm install --silent)
    # Symlink node_modules ra root (script require từ root)
    [ ! -e "$REPO_ROOT/node_modules" ] && ln -s "$SCRIPTS_DIR/node_modules" "$REPO_ROOT/node_modules"

    # ── BƯỚC 2: Copy script ra root ───────────────────────────────────────────
    step "Copy script mã hóa ra root"
    copy_scripts_to_root

    # ── BƯỚC 3: Chạy mã hóa ──────────────────────────────────────────────────
    step "Chạy mã hóa code"
    for script in "${ENCODE_SCRIPTS[@]}"; do
        echo "    → node $script"
        (cd "$REPO_ROOT" && node "$script") || warn "$script thất bại, tiếp tục..."
    done
    encode_done=true
    # Lúc này: code đã bị rename, keychange*.json đã có → reverse.js có thể hoạt động

    # ── BƯỚC 4: BUILD TEST ────────────────────────────────────────────────────
    # Build NGAY SAU encode, trong khi keychange*.json vẫn còn
    # → nếu fail, trap _cleanup sẽ tự reverse
    step "BUILD TEST: ./gradlew assembleRelease"
    echo "    (xem output bên dưới để biết kết quả)"
    echo "──────────────────────────────────────────"

    if (cd "$REPO_ROOT" && ./gradlew assembleRelease); then
        echo "──────────────────────────────────────────"
        ok "BUILD THÀNH CÔNG ✓"
    else
        echo "──────────────────────────────────────────"
        die "BUILD THẤT BẠI — code sẽ được reverse, không push lên build_tool."
    fi

    # ── BƯỚC 5: Build OK → giờ mới xóa/lọc file ─────────────────────────────
    encode_done=false   # tắt cờ reverse (build đã OK, không cần reverse nữa)

    step "Xóa script và keychange*.json (không push)"
    remove_scripts_from_root
    find "$REPO_ROOT" -maxdepth 1 -name "keychange*.json" -delete 2>/dev/null || true

    step "Loại trừ encode-scripts/"
    git rm -r --cached "$SCRIPTS_DIR" 2>/dev/null || true
    rm -rf "$SCRIPTS_DIR"

    step "Loại trừ file .md"
    while IFS= read -r -d '' md; do
        git rm --cached "$md" 2>/dev/null || true
        rm -f "$md"
    done < <(find "$REPO_ROOT" -name "*.md" -not -path "*/node_modules/*" -print0)

    step "Loại trừ $EXCLUDED_FOLDER"
    if [ -d "$REPO_ROOT/$EXCLUDED_FOLDER" ]; then
        git rm -r --cached "$REPO_ROOT/$EXCLUDED_FOLDER" 2>/dev/null || true
        rm -rf "$REPO_ROOT/$EXCLUDED_FOLDER"
    fi

    for f in yarn.lock debug.log; do
        [ -f "$REPO_ROOT/$f" ] && { git rm --cached "$REPO_ROOT/$f" 2>/dev/null || true; rm -f "$REPO_ROOT/$f"; }
    done

    # ── BƯỚC 6: Commit code đã mã hóa ────────────────────────────────────────
    git add --all

    if git status --porcelain | grep -q .; then
        git commit -m "sync: encoded build snapshot [auto]"
    else
        ok "Không có thay đổi."
    fi

    # ── BƯỚC 7: Push ──────────────────────────────────────────────────────────
    step "Đẩy lên build_tool/$BUILD_PUSH_BRANCH"
    git push build_tool "${tb}:${BUILD_PUSH_BRANCH}" --force
    ok "build_tool/$BUILD_PUSH_BRANCH ✓"

    _cleanup; trap - EXIT
}

# ── MAIN ──────────────────────────────────────────────────────────────────────

case "$TARGET" in
    origin)     push_origin ;;
    build_tool) push_build_tool ;;
    both)       push_origin; push_build_tool ;;
    *) die "Target không hợp lệ: $TARGET (dùng: origin | build_tool | both)" ;;
esac

echo -e "\n${GREEN}=== Hoàn tất! ===${NC}"
