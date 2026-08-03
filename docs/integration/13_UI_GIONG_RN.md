# 13 — Dựng UI Compose bám sát bản React-Native

> Mục tiêu: giao diện Kotlin/Compose **giống bản RN nhất có thể** — cùng màu, cùng bố cục box/header/tab, cùng chuỗi. Dưới đây là quy trình + bảng ánh xạ + các lỗi thực tế đã sửa ở vga31a.

## 1. Quy trình đối chiếu từng màn

1. **Chụp lại app RN thật** (hoặc lấy screenshot cũ) làm chuẩn vàng. So từng vùng: **header → tab → item/box → nút → nền**.
2. Mở file RN của màn đó, đọc `tw\`...\`` (twrnc/tailwind) để lấy **màu, radius, padding, shadow, font-weight**.
3. Dựng Compose theo đúng thông số. Sau khi build, **so screenshot mới vs cũ**, sửa tới khi khớp.
4. Tận dụng lại **assets cũ** (icon, ảnh) thay vì vẽ lại — xem §5.

## 2. Bảng ánh xạ RN → Compose

| RN (tailwind/twrnc) | Compose |
|---|---|
| `<View>` xếp dọc / ngang | `Column` / `Row` |
| `<View>` chồng lớp | `Box` |
| `bg-white rounded-2xl shadow` (box nổi) | `Card(colors=cardColors(Color.White), elevation=CardDefaults.cardElevation(4.dp), shape=RoundedCornerShape(16.dp))` |
| `<FlatList>` | `LazyColumn` (nhớ `item(key=...)` ổn định, nhất là slot ads) |
| `<TouchableOpacity>` | `Modifier.clickable { }` / `Button` / `IconButton` |
| `<Image source={require(...)}>` | `Image(painterResource(R.drawable.xxx))` / Coil `AsyncImage` cho ảnh động |
| `<TextInput>` bo tròn (search) | `OutlinedTextField`/`BasicTextField` bọc trong `Surface(shape=RoundedCornerShape(24.dp))` |
| `px-4` (16) / `py-2` (8) | `padding(horizontal=16.dp, vertical=8.dp)` |
| `text-[22px] font-700` | `fontSize=22.sp, fontWeight=FontWeight.Bold` |
| tab dạng pill | `Row` các `Surface`/`Box` bo tròn, cái đang chọn nền `Primary`, chữ trắng |

## 3. Theme (bám màu RN)

- Lấy **màu chủ đạo** từ RN (thường trong `tailwind.config`/`envApp`/hằng màu). vga31a: **xanh `#2196F3`** (KHÔNG phải xanh lá — lỗi đã gặp).
- `ui/theme/Theme.kt`: đặt `Primary = Color(0xFF2196F3)`; dùng cho tab chọn, nút chính, chấm indicator.
- **Nền màn**: xám rất nhạt hoặc trắng; **box nội dung: TRẮNG + shadow** (đổ bóng) — đây là điểm dễ sai nhất.

## 4. Các lỗi UI thực tế đã sửa (vga31a)

| Sai | Đúng (bản RN) |
|---|---|
| Box nền **xám**, phẳng | Box nền **trắng + shadow** (Card elevation), bo góc |
| Theme **xanh lá** | **Xanh `#2196F3`** |
| Header là **thanh xanh** đậm | **Không có bar màu**; tiêu đề **đen, căn giữa** (hoặc trái theo RN) |
| Tab kiểu underline mặc định | **Pill tab** (bo tròn, cái chọn nền Primary chữ trắng) |
| Ô search vuông | Ô search **bo tròn dạng pill** (radius lớn), có icon kính lúp |
| Có màn xin quyền Usage **riêng trước Home** | RN chỉ hỏi Usage **khi vào chi tiết app → tab App usage** — bỏ màn riêng |
| Nút crown (góc phải Home) mở Settings | Crown mở thẳng **màn IAP của lib** (`IapActivity.start(...)`) |
| Logo placeholder | Dùng **logo/icon thật** của app cũ |
| Padding "session now" dính nhau khi chưa có ads | Chừa **padding** cho slot ads dù ads chưa ra |

## 5. Assets & i18n (giống RN)

- **Icon/ảnh**: bê nguyên bộ drawable + screenshot từ app RN cũ (thư mục `screenshoot`/assets). Đặt vào `res/drawable`, dùng lại tên gợi nhớ (encode sẽ tự đổi khi build_tool).
- **i18n**: port **đúng key RN cũ** sang `res/values[-xx]/strings.xml`. vga31a: 16 ngôn ngữ (en default + vi/de/es/ar/fr/hi/in/it/ja/km/ko/pt/ru/th/zh).
  - ⚠️ **Từ khoá reserved của Android**: key trùng từ khoá XML/Kotlin phải đổi (vd `continue` → `continue_btn`).
  - Chuỗi hướng dẫn đổi ngôn ngữ phải hiển thị theo **locale đang chọn**, không hardcode.
- **Đổi ngôn ngữ**: lib gọi `notifyLanguageSaved(code)` → app ghi prefs `AppStorage.language` + `LocaleHelper.updateLocale`. `MainActivity.onCreate` áp lại locale đã lưu → mọi màn app đổi chuỗi (kể cả khi đổi ngôn ngữ trong Settings, do lib mở `LanguageActivity`).

## 6. Điều hướng khớp RN

- Single-Activity + Navigation Compose. `MainActivity` = `getHomeActivity()` của lib (không MAIN/LAUNCHER).
- Onboarding riêng của app = **Intro** (xem `12_INTRO_CHECK.md`); Language do lib lo.
- Deep-link/shortcut (vd "Uninstall") → route thẳng màn tương ứng.

## 7. Checklist

- [ ] Màu chủ đạo khớp RN; box trắng + shadow; header/tab/search đúng kiểu.
- [ ] Mọi màn so screenshot mới vs cũ đạt.
- [ ] Assets icon/ảnh dùng lại của RN; i18n đủ ngôn ngữ, xử lý key reserved.
- [ ] Đổi ngôn ngữ trong app + trong Settings đều đổi chuỗi (LocaleHelper).
- [ ] Không dựng lại các màn lib đã lo (Splash/Language/IAP).
