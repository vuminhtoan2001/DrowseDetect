# vga52a-kotlin — Mapping Màn hình ↔ Screenshot

| # | Màn hình | File screenshot | Ghi chú |
|---|---|---|---|
| 1 | Intro (4 slide) | `intro1.jpg`, `intro2.jpg`, `intro3.jpg` | Thiếu ảnh slide 4 "Get Started" |
| 2 | Permission | — | Chưa có ảnh |
| 3 | Home | `home.jpg` | |
| 4 | Setting | — | Chưa có ảnh |
| 5 | TakePhotoSuccessfully | — | Chưa có ảnh |
| 6 | Camera — chế độ Ảnh | `take_photo_wb.jpg`, `takephoto_amp.jpg`, `takephoto_filter.jpg`, `takephoto_collage.jpg`, `Screenshot_..._082506....jpg` | File cuối = panel Exposure 40%, tên chưa chuẩn hoá |
| 7 | Camera — chế độ Video | `take_video.jpg` | |
| 8 | Camera — Menu "More" | `Screenshot_..._094705....jpg`, `Screenshot_..._094708....jpg` | Lần lượt: More→Resolution, More→Collage (chọn layout ô vuông); tên chưa chuẩn hoá |
| 9 | VideoResult | — | Route khai báo sẵn nhưng không được gọi trong luồng quay-video thật (xem `01_...md`) |
| 10 | ImportPhotoResult | `detail_video.jpg`, `detail_video_all.jpg` | Chỉ có bản video, thiếu ảnh case ảnh tĩnh |
| 11 | MediaPickerScreen (chọn ảnh dùng chung) | `collage_list_and_select.jpg` | Dùng chung cho Edit Photo + Collage Maker |
| 12–13 | EditPhotoScreen + 7 tool | `edit_image_all.jpg`, `edit_image_filter.jpg`, `edit_image_frame.jpg`, `edit_image_sticker.jpg` | Thiếu ảnh cho Text/Draw/RemoveBG/Rotate |
| 14 | Collage Picker | (dùng chung `collage_list_and_select.jpg`) | |
| 15 | Collage Maker Editor | `collage_all.jpg`, `collage_background.jpg`, `collage_layout.jpg`, `collage_ratio.jpg`, `collage_sticker.jpg`, `collage_edit_text.jpg`, `collage_text_edit.jpg`, `Screenshot_..._083743....jpg` | `collage_edit_text.jpg` = Text tầng chính; `collage_text_edit.jpg` = Text tầng phụ ARC (tên dễ nhầm); file cuối = panel Draw, tên chưa chuẩn hoá |
| 16 | Wallpaper Templates | `wallpaper_template.jpg` | |
| 17 | Wallpaper Editor | `wallpaper.jpg`, `wallpaper_example_1.jpg`, `wallpaper_example_2_can_edit_image.jpg`, `wallpaer_edit_text.jpg`, `wallpaper_add_edit_text.jpg` | 2 file cuối nội dung gần giống nhau, cần xác nhận lại là 2 bước khác nhau hay trùng; `wallpaer_edit_text.jpg` lỗi chính tả (thiếu "p") |
| 18 | Gallery | `gallary.jpg`, `gallary_photo.jpg`, `list_video.jpg` | 2 file đầu lỗi chính tả ("gallary" → đúng "gallery") |
| — | *(orphan, không khớp màn nào)* | `welcome.jpg` | Màn Welcome của app RN gốc, đã gộp bỏ khi viết lại bằng Kotlin |
