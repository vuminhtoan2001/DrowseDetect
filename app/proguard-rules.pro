# ============================================================================
# proguard-rules.pro — Rule RIÊNG của vga52a-kotlin.
#
# Rule dùng chung cho mọi app (base-application AAR, Billing, AdMob mediation, Gson, Crashlytics)
# đã tách sang `proguard-rules-base.pro` (xem `proguardFiles(...)` trong app/build.gradle.kts) —
# KHÔNG lặp lại ở đây. File này chỉ giữ rule cho SDK/model ĐẶC THÙ của app này.
#
# Khi clone sang app mới: xoá hết nội dung dưới đây, chỉ giữ 3 dòng comment trên + thêm rule cho SDK
# riêng của app mới (xem "công thức" cho SDK phổ biến ở docs/integration/16_ENCODE_R8_STANDARD.md).
#
# App này hiện chỉ gồm Intro/Permission/Home/Setting + ads/IAP (core), chưa có SDK riêng nào cần
# keep-rule bổ sung — thêm rule vào đây khi bắt đầu 1 tính năng thật (CameraX/Media3/Room/...).
# ============================================================================
