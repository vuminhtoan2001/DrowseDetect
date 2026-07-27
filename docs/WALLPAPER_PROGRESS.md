# Tien do Wallpaper Creator

## 2026-07-27

- [x] Buoc 1: Doc lai tien trinh dang do va xac nhan loi hien tai: `WallpaperEditorScreen.kt` dang tham chieu `TemplateSlot/TemplateSlotDetector` khong ton tai, trong khi repo da co `TemplateFrameLoader` doc `templates_v2/template_N/data.json`.
- [x] Buoc 2: Chuyen editor sang dung `TemplateFrameSet` tu JSON.
- [x] Buoc 3: Ve anh nguoi dung vao tung polygon frame bang perspective matrix.
- [x] Buoc 4: Kiem tra build va ghi chu ket qua: `.\gradlew.bat :app:compileDebugKotlin` da BUILD SUCCESSFUL.
- [x] Bo sung: Decode anh frame bang `remember(...)` dong bo trong composition de preview va anh export deu co anh da chon.
- [x] Buoc 5: Xoa template 1, 2, 17 khoi `assets/images/templates_v2` gom ca folder editor va file preview `.webp`.
- [x] Buoc 6: Sua man list wallpaper de hien thi preview toan template thay vi crop vao mot goc.
- [x] Buoc 7: Build lai va cai APK len thiet bi hien tai qua adb: `assembleDebug` thanh cong va `adb install -r` vao `R92X7128G2Y` thanh cong.
- [x] Buoc 8: Dieu chinh list wallpaper: giu dung anh preview tong `template_N.webp` nhung doi tu `ContentScale.Fit` sang `ContentScale.Crop` de giam khoang trong giua/trong item, van nhin duoc goc rong hon background cu.
