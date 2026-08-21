#!/usr/bin/env bash
# push.sh - Đẩy code lên 2 remote
#
# ORIGIN     → origin/main     : tất cả file (kể cả encode-scripts/), code GỐC chưa mã hoá
# BUILD_TOOL → build_tool/main : encode → BUILD TEST → nếu OK thì mới xóa file và push
#              KHÔNG gồm: encode-scripts/, .md, drive-download/
#
# Cách dùng:
#   ./push.sh                          # đẩy cả 2
#   ./push.sh origin
#   ./push.sh build_tool
#
# Mức kiểm tra code đã mã hoá (chỉ ảnh hưởng luồng build_tool):
#   (mặc định)  --full      ./gradlew assembleRelease            ~10-12 phút
#               --quick     compileReleaseKotlin + lintVitalRelease
#               --no-build  không build gì                       ~30 giây
#
#   Xem `docs/integration/PLAYBOOK_TICH_HOP.md` §E.7 để biết mỗi mức BẮT ĐƯỢC và BỎ SÓT lỗi gì —
#   đọc trước khi dùng --quick/--no-build, đừng chọn theo cảm giác "lần trước không lỗi".

set -euo pipefail

# ── Cấu hình ──────────────────────────────────────────────────────────────────

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS_DIR="$REPO_ROOT/encode-scripts"
# Cả 2 remote đều dùng nhánh `main`:
#   origin/main     = code GỐC, đầy đủ (kể cả encode-scripts/, docs/*.md)
#   build_tool/main = code ĐÃ MÃ HOÁ, đã strip file không liên quan tới build
#
# WORK_BRANCH phải là nhánh bạn đang làm việc thật (đối chiếu `git branch --show-current`). Script tạo
# nhánh tạm rồi `git checkout` về đây lúc dọn dẹp — khai tên một nhánh không tồn tại thì `git checkout`
# thất bại và bạn BỊ BỎ LẠI TRÊN NHÁNH TẠM: ở luồng build_tool nhánh đó là bản đã xoá sạch `.md` +
# `encode-scripts/` → trông như mất file.
WORK_BRANCH="main"
PUSH_BRANCH="main"
BUILD_PUSH_BRANCH="main"

# Nhận tham số theo THỨ TỰ TỰ DO (`./push.sh --quick build_tool` cũng đúng như đảo lại) — vì bản trước
# chỉ đọc `$1` nên `./push.sh --quick` sẽ âm thầm hiểu là target không hợp lệ.
TARGET="both"
VERIFY="full"
for arg in "$@"; do
    case "$arg" in
        origin | build_tool | both) TARGET="$arg" ;;
        --full)     VERIFY="full" ;;
        --quick)    VERIFY="quick" ;;
        --no-build) VERIFY="none" ;;
        *) echo "Tham số không hợp lệ: $arg" >&2
           echo "  dùng: ./push.sh [origin|build_tool|both] [--full|--quick|--no-build]" >&2
           exit 1 ;;
    esac
done

# Danh sách bước bật/tắt qua rename-config-global.js -> pipeline (KHÔNG sửa ở đây nữa — xem
# encode-scripts/rename-all.js). Mặc định renameConfigKey TẮT vì app đọc key Remote Config động
# (getBoolean("${placement}_enable"), getInt("${p}_ratio")) → hash sẽ làm release đọc trượt key
# → tắt hết ads. Giữ config key readable ở cả config.xml + code + firebase.

# Danh sách này KHÔNG còn dùng để copy ra root (xem `run_encode_in_place` bên dưới) — giữ lại chỉ để
# DỌN bản copy còn sót ở root từ những lần chạy trước của phiên bản script cũ. Copy sót mà không dọn
# thì `git add --all` sẽ commit chúng lên build_tool.
TEMP_FILES=(
    rename-config-global.js
    rename-config-key.js
    safe-skip-base.js
    rename-drawables.js
    rename-file.js
    rename-languages.js
    rename-code-symbols.js
    rename-all.js
    reverse.js
    package.json
)

# File/thư mục KHÔNG liên quan tới build → strip khỏi build_tool (vẫn giữ đủ trên origin).
# Đo thật trên vga58a lần push đầu: 104 MB lọt sang remote build vì bước lọc cũ chỉ xoá `*.md`, còn
# ẢNH trong docs/ vẫn ở lại. Repo build chỉ cần thứ Gradle đọc.
EXCLUDED_PATHS=(
    "drive-download-20260629T074041Z-3-001"
    "docs"          # tài liệu + docs/screenshot/*.png (2.4 MB) — .md đã bị bước trên xoá, ảnh thì không
    "docs-sample"   # ảnh mẫu app khác + file .fig Figma (100.6 MB)
    "_ref"          # ảnh so sánh với app tham khảo (0.8 MB)
    "yarn.lock"
    "debug.log"
)

# ── Helpers ───────────────────────────────────────────────────────────────────

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step()  { echo -e "\n${CYAN}>>> $*${NC}"; }
ok()    { echo -e "    ${GREEN}OK: $*${NC}"; }
warn()  { echo -e "    ${YELLOW}WARN: $*${NC}"; }
die()   { echo -e "\n${RED}LỖI: $*${NC}"; exit 1; }

# Chạy encode NGAY TRONG encode-scripts/, KHÔNG copy script ra root.
#
# BUG ĐÃ GẶP THẬT (vga58a): bản trước copy 10 script ra `$REPO_ROOT` rồi chạy ở đó. Nhưng cả bộ script
# giải đường dẫn theo VỊ TRÍ CỦA CHÍNH NÓ — `rename-config-global.js` khai
# `srcCodePath: "../app/src/main"`, các script làm `path.resolve(__dirname, srcCodePath)`. Đặt ở
# `encode-scripts/` thì `..` = repo root → đúng; copy ra root thì `..` = **thư mục cha của repo** →
# `ENOENT: scandir 'C:\workspace\java_app\app\src\main'`. `firebase-merge.js` còn dùng
# `process.cwd()` nên cũng chỉ đúng khi cwd = encode-scripts/.
#
# Nguy hiểm ở chỗ bước 1 (rename-drawables) KHÔNG chết ngay: nó quét thư mục rỗng, in "đã đổi tên 0
# file", rồi mới chết ở bước cập nhật tham chiếu — trông như "encode chạy được một nửa". Và `reverse.js`
# cũng chết vì cùng lý do, nên nếu encode kịp sửa file thật thì đã KHÔNG khôi phục được.
#
# Chạy tại chỗ cũng bỏ luôn được symlink `node_modules` ra root (`ln -s` trên Git Bash/Windows không
# tạo được symlink thật, nó copy cả cây thư mục rồi bỏ lại ở root).
run_encode_in_place() { (cd "$SCRIPTS_DIR" && node rename-all.js); }
run_reverse_in_place() { (cd "$SCRIPTS_DIR" && node reverse.js); }

# keychange*.json là output của encode (bản đồ tên gốc→hash, để reverse dùng). Ghi cạnh script nên
# giờ nằm trong encode-scripts/. Quét cả root để dọn bản sót của lần chạy kiểu cũ.
remove_keychange_files() {
    find "$SCRIPTS_DIR" "$REPO_ROOT" -maxdepth 1 -name "keychange*.json" -delete 2>/dev/null || true
}

# Dọn rác do phiên bản script cũ để lại ở root (copy script + cây node_modules do `ln -s` fallback).
remove_scripts_from_root() {
    for f in "${TEMP_FILES[@]}"; do rm -f "$REPO_ROOT/$f"; done
    if [ -L "$REPO_ROOT/node_modules" ]; then
        rm "$REPO_ROOT/node_modules"
    elif [ -d "$REPO_ROOT/node_modules" ] && [ ! -f "$REPO_ROOT/package.json" ]; then
        # node_modules ở root mà root không có package.json → rác của lần `ln -s` thất bại, không
        # phải dependency thật của project (project này là Android, không dùng npm ở root).
        rm -rf "$REPO_ROOT/node_modules"
    fi
}

temp_branch() { echo "temp/$1-$(date +%s)"; }

# ── Kiểm tra ──────────────────────────────────────────────────────────────────

cd "$REPO_ROOT"
[ -d "$SCRIPTS_DIR" ] || die "Không tìm thấy $SCRIPTS_DIR"
git status --porcelain | grep -qE '^[MADRC]' && die "Có staged changes chưa commit. Hãy commit trước."

# Chặn chạy khi code ĐANG Ở TRẠNG THÁI MÃ HOÁ. `keychange*.json` chỉ tồn tại giữa lúc encode và lúc
# `reverse.js`, nên còn thấy chúng nghĩa là có một lần encode chưa được reverse (thường là encode tay để
# test bản obfuscate rồi quên trả lại). Chạy tiếp lúc đó gây 2 hậu quả nặng:
#   - `push.sh origin`     → `git add --all` commit CODE ĐÃ MÃ HOÁ lên origin/main, tức mất bản đọc được
#                            (guard staged-changes ở trên KHÔNG bắt được vì đây là thay đổi chưa `add`).
#   - `push.sh build_tool` → encode LẦN HAI lên code đã encode, và `reverse.js` chỉ còn bản đồ của lần
#                            hai → không đường về.
#
# Dùng `find` chứ KHÔNG `ls a b`: khi một trong hai glob không khớp gì, bash để nguyên chuỗi glob và `ls`
# trả exit != 0 cho riêng đối số đó — cả lệnh thành "thất bại" dù file kia CÓ tồn tại, nên guard viết
# bằng `ls` im lặng không nổ. Đã tự vấp đúng lỗi này lúc thử guard.
if [ -n "$(find "$SCRIPTS_DIR" "$REPO_ROOT" -maxdepth 1 -name 'keychange*.json' -print -quit 2>/dev/null)" ]; then
    die "Còn keychange*.json → code đang MÃ HOÁ, chưa reverse.
   Trả code về bản gốc rồi chạy lại:
     (cd encode-scripts && node reverse.js) && git checkout -- app/ && rm -f encode-scripts/keychange*.json"
fi

# ── PUSH TO ORIGIN ────────────────────────────────────────────────────────────

push_origin() {
    step "PUSH TO ORIGIN"
    local tb; tb=$(temp_branch "origin")

    # Sau khi xong, ĐƯA commit tự động về luôn nhánh làm việc trước khi xoá nhánh tạm.
    #
    # BUG ĐÃ GẶP THẬT: `git add --all` gói mọi thay đổi CHƯA COMMIT vào commit trên nhánh tạm → push lên
    # origin → `git branch -D` xoá nhánh tạm → những thay đổi đó BIẾN KHỎI working tree local, chỉ còn
    # trên origin (và trong reflog). Người dùng mở IDE ra thấy file trở về như cũ, tưởng bị linter revert.
    # `--ff-only` nên nhánh làm việc chỉ TIẾN, không bao giờ sinh merge commit hay ghi đè gì.
    _cleanup() {
        git checkout "$WORK_BRANCH" 2>/dev/null || true
        git merge --ff-only "${tb:-}" 2>/dev/null || true
        git branch -D "${tb:-}" 2>/dev/null || true
    }
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
            # keychange*.json vẫn còn (chưa xóa) → reverse hoạt động được
            run_reverse_in_place \
                && ok "Reverse hoàn tất — code đã khôi phục" \
                || warn "reverse.js có lỗi"
        fi
        remove_scripts_from_root
        # Xóa keychange*.json (output của encode, không push lên đâu)
        remove_keychange_files

        # KHÔNG che lỗi checkout bằng `|| true`. Nhánh tạm là bản đã xoá `*.md` + `encode-scripts/` và
        # code đang MÃ HOÁ — bị bỏ lại trên đó mà không báo gì thì người dùng mở IDE ra thấy mất file,
        # tưởng encode script phá repo. Đã xảy ra thật 2 lần (WORK_BRANCH sai tên; và file untracked
        # chặn checkout). Báo to + chỉ đúng cách khắc phục.
        if git checkout "$WORK_BRANCH" 2>/dev/null; then
            git branch -D "${tb:-}" 2>/dev/null || true
        else
            echo ""
            warn "KHÔNG checkout được về '$WORK_BRANCH' — BẠN ĐANG Ở NHÁNH TẠM ${tb:-?}."
            warn "Nhánh này: code đã MÃ HOÁ, đã xoá *.md + encode-scripts/. Đừng làm việc trên đó."
            warn "Thường do file untracked chặn checkout. Khắc phục:"
            warn "  git status                       # xem file nào đang chặn"
            warn "  git checkout $WORK_BRANCH && git branch -D ${tb:-}"
        fi
    }
    trap _cleanup EXIT

    git checkout -b "$tb"

    # ── BƯỚC 1: Cài npm ───────────────────────────────────────────────────────
    step "Cài npm packages"
    (cd "$SCRIPTS_DIR" && npm install --silent)

    # ── BƯỚC 2: Dọn rác của lần chạy kiểu cũ ──────────────────────────────────
    # Không còn copy script ra root nữa (xem run_encode_in_place) — chỉ dọn bản sót.
    step "Dọn script/node_modules sót ở root (nếu có)"
    remove_scripts_from_root

    # ── BƯỚC 3: Chạy mã hóa (rename-all.js — thứ tự bước cố định, xem comment trong file đó) ──
    step "Chạy mã hóa code"
    # KHÔNG dùng "|| warn ... tiếp tục" như trước: nếu 1 bước rename lỗi giữa chừng, code đã ở
    # trạng thái rename DỞ DANG (một phần file/class đã đổi tên, phần khác chưa) — chạy tiếp bước
    # sau chỉ làm hỏng thêm. rename-all.js tự dừng ngay khi có bước lỗi (exit code != 0) — để
    # _cleanup phía dưới tự reverse toàn bộ về bản gốc, đúng với mục đích cờ encode_done.
    encode_done=true
    run_encode_in_place || die "Mã hoá thất bại — xem log ở trên. Code sẽ được reverse."
    # Lúc này: code đã bị rename, keychange*.json đã có → reverse.js có thể hoạt động

    # ── BƯỚC 4: KIỂM TRA CODE ĐÃ MÃ HOÁ ──────────────────────────────────────
    # Chạy NGAY SAU encode, trong khi keychange*.json vẫn còn → nếu fail, trap _cleanup tự reverse.
    #
    # Mỗi mức bắt được lớp lỗi khác nhau (chi tiết + bảng đối chiếu: PLAYBOOK_TICH_HOP.md §E.7):
    #   full   assembleRelease — thêm R8 + shrinkResources + dex + đóng gói APK. Chỉ mức này bắt được
    #          trùng class do fat-AAR (§3.10) và các lỗi keep-rule lộ ở bước minify.
    #   quick  compileReleaseKotlin bắt gần hết lỗi encode hay gặp (Unresolved reference R/BuildConfig
    #          §3.1, tên trùng Compose/API §3.4, FQN inline §3.3); lintVitalRelease là bước DUY NHẤT bắt
    #          `MissingClass` khi manifest không theo kịp rename package (§3.2) → giữ lại, đừng bỏ.
    #   none   không kiểm gì. Chỉ dùng khi VỪA chạy full xanh và sau đó chỉ sửa thứ không đổi cấu trúc
    #          (text, màu, con số). Đổi tên/thêm/xoá file, thêm thư viện → phải full.
    #
    # LƯU Ý: không mức nào bắt được lỗi CHẠY (hash đập vào chuỗi JS/CSS, lệch key Remote Config...) —
    # những cái đó chỉ lộ khi cài bản encode lên máy thật. Build xanh ≠ chạy đúng.
    local verify_tasks
    case "$VERIFY" in
        full)  verify_tasks="assembleRelease" ;;
        quick) verify_tasks=":app:compileReleaseKotlin :app:lintVitalRelease" ;;
        none)  verify_tasks="" ;;
    esac

    if [ -z "$verify_tasks" ]; then
        step "BỎ QUA kiểm tra (--no-build)"
        warn "Code đã mã hoá CHƯA được biên dịch thử lần nào. Nếu encode làm vỡ tham chiếu, lỗi sẽ nổ"
        warn "ở server build chứ không phải ở đây."
    else
        step "KIỂM TRA [$VERIFY]: ./gradlew $verify_tasks"
        echo "    (xem output bên dưới để biết kết quả)"
        echo "──────────────────────────────────────────"

        # $verify_tasks KHÔNG bọc nháy: cần bash tách thành nhiều đối số task. Đây là chủ ý, không phải
        # sót quoting — các giá trị đều do case ở trên đặt, không lấy từ input người dùng.
        if (cd "$REPO_ROOT" && ./gradlew $verify_tasks); then
            echo "──────────────────────────────────────────"
            ok "KIỂM TRA THÀNH CÔNG ✓ [$VERIFY]"
        else
            echo "──────────────────────────────────────────"
            die "KIỂM TRA THẤT BẠI — code sẽ được reverse, không push lên build_tool."
        fi
    fi

    # ── BƯỚC 5: Build OK → giờ mới xóa/lọc file ─────────────────────────────
    encode_done=false   # tắt cờ reverse (build đã OK, không cần reverse nữa)

    step "Xóa script và keychange*.json (không push)"
    remove_scripts_from_root
    remove_keychange_files

    step "Loại trừ encode-scripts/"
    git rm -r --cached "$SCRIPTS_DIR" 2>/dev/null || true
    rm -rf "$SCRIPTS_DIR"

    step "Loại trừ file .md"
    while IFS= read -r -d '' md; do
        git rm --cached "$md" 2>/dev/null || true
        rm -f "$md"
    done < <(find "$REPO_ROOT" -name "*.md" -not -path "*/node_modules/*" -print0)

    step "Loại trừ file/thư mục không liên quan tới build"
    for p in "${EXCLUDED_PATHS[@]}"; do
        if [ -e "$REPO_ROOT/$p" ]; then
            git rm -r --cached --quiet "$REPO_ROOT/$p" 2>/dev/null || true
            rm -rf "$REPO_ROOT/$p"
            ok "đã loại: $p"
        fi
    done

    # ── BƯỚC 6: Commit code đã mã hóa ────────────────────────────────────────
    # KHÔNG loại `push.sh` khỏi commit. Đã thử `git rm --cached push.sh` (để repo build không có file
    # quy trình dev) và nó gây hậu quả nặng hơn cái lợi 12 KB: file thành UNTRACKED trên nhánh tạm, nên
    # `git checkout main` lúc dọn dẹp bị git từ chối ("would be overwritten by checkout") → người dùng
    # bị bỏ lại trên nhánh tạm với code đã mã hoá và `.md`/`encode-scripts/` đã xoá.
    git add --all

    if git status --porcelain | grep -q .; then
        git commit -m "sync: encoded build snapshot [auto]"
    else
        ok "Không có thay đổi."
    fi

    # ── BƯỚC 7: Push (FAST-FORWARD, KHÔNG force) ─────────────────────────────
    # `main` trên repo build là PROTECTED BRANCH:
    #   remote: GitLab: You are not allowed to force push code to a protected branch on this project.
    # Lần push đầu tiên qua được chỉ vì repo còn RỖNG (tạo nhánh mới không tính là force). Từ lần 2 trở
    # đi mọi `--force` đều bị pre-receive hook chặn.
    #
    # Nhánh tạm không thể fast-forward vì nó là con của `main` LOCAL, không chứa commit đang là tip của
    # build_tool/main. Nên thay vì force, dựng thẳng một commit ĐÈ LÊN TIP HIỆN TẠI của remote:
    #   tree   = snapshot code đã mã hoá + đã strip (cây của commit trên nhánh tạm)
    #   parent = build_tool/$BUILD_PUSH_BRANCH (bỏ trống nếu remote còn rỗng)
    # → push thành fast-forward, protected branch chấp nhận, và giữ được history bên repo build thay vì
    # ghi đè mất.
    step "Đẩy lên build_tool/$BUILD_PUSH_BRANCH"
    git fetch -q build_tool "$BUILD_PUSH_BRANCH" 2>/dev/null || true

    local snapshot_tree remote_tip build_commit
    snapshot_tree=$(git rev-parse "${tb}^{tree}")
    if remote_tip=$(git rev-parse --verify -q "build_tool/$BUILD_PUSH_BRANCH"); then
        build_commit=$(git commit-tree "$snapshot_tree" -p "$remote_tip" \
            -m "sync: encoded build snapshot [auto]")
    else
        ok "build_tool còn rỗng — tạo commit đầu tiên"
        build_commit=$(git commit-tree "$snapshot_tree" -m "sync: encoded build snapshot [auto]")
    fi
    git push build_tool "${build_commit}:refs/heads/${BUILD_PUSH_BRANCH}"
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
