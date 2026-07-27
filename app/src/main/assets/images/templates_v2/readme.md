# Template phát hiện khung ảnh và xuất JSON

## JSON ví dụ

```json
{
  "version": 1,
  "imageSize": {
    "width": 829,
    "height": 1792
  },
  "frames": [
    {
      "id": "frame_1",
      "points": [
        { "x": 292, "y": 313 },
        { "x": 727, "y": 344 },
        { "x": 663, "y": 818 },
        { "x": 238, "y": 756 }
      ]
    },
    {
      "id": "frame_2",
      "points": [
        { "x": 192, "y": 786 },
        { "x": 598, "y": 764 },
        { "x": 641, "y": 1248 },
        { "x": 223, "y": 1272 }
      ]
    },
    {
      "id": "frame_3",
      "points": [
        { "x": 141, "y": 1218 },
        { "x": 544, "y": 1288 },
        { "x": 610, "y": 1712 },
        { "x": 205, "y": 1644 }
      ]
    }
  ]
}
```

## Thứ tự các điểm

1.  Top Left
2.  Top Right
3.  Bottom Right
4.  Bottom Left

---

# Quy trình

## Bước 1. Phát hiện khung

- Detect contour của các khung ảnh.
- Nối các đoạn bị che bởi text hoặc sticker.
- Tạo polygon kín cho từng khung.

## Bước 2. Lấy polygon

- `approxPolyDP()`
- Lấy polygon 4 đỉnh.
- Sắp xếp lại theo thứ tự:
  - Top Left
  - Top Right
  - Bottom Right
  - Bottom Left

## Bước 3. Xuất JSON

Lưu metadata của template gồm kích thước ảnh và danh sách các khung.

## Bước 4. Ghép ảnh

Tạo source points từ ảnh người dùng:

```text
(0,0)
(width,0)
(width,height)
(0,height)
```

Tính phép biến đổi phối cảnh:

```text
PerspectiveTransform(sourcePoints, frame.points)
```

## Bước 5. Warp ảnh

Có thể dùng:

- OpenCV: `warpPerspective()`
- HTML Canvas: `ctx.transform(...)`
- WebGL: Perspective Matrix

Ảnh sẽ được biến đổi để khớp chính xác với từng khung.

---

# Kiến trúc đề xuất

Mỗi template lưu một file JSON:

```json
{
  "template": "birthday_01",
  "size": {
    "width": 829,
    "height": 1792
  },
  "frames": [
    {
      "id": "frame_1",
      "points": []
    }
  ]
}
```

Pipeline:

    Template
        ↓
    Detect contour
        ↓
    Hoàn thiện đường viền
        ↓
    Detect 4 đỉnh
        ↓
    Xuất JSON
        ↓
    Warp ảnh
