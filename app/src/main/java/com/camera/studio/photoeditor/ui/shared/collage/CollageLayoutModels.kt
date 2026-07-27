package com.camera.studio.photoeditor.ui.shared.collage

import android.graphics.Rect

/**
 * 7 layout dùng chung giữa Collage Maker (M5) và Camera More-menu → Collage live-shoot (M2), khớp
 * `Screenshot_20260724_094708_*.jpg` (1 ô vuông, 2 cột, 2 hàng, lưới 4, 3 hàng, 3 cột, lưới 9).
 */
enum class CollageLayout(val paneCount: Int) {
    SINGLE(1),
    TWO_VERTICAL(2),
    TWO_HORIZONTAL(2),
    FOUR_GRID(4),
    THREE_HORIZONTAL(3),
    THREE_VERTICAL(3),
    NINE_GRID(9);

    /** Chia 1 canvas (0,0,width,height) thành các ô theo layout, thứ tự trái→phải, trên→dưới. */
    fun panes(width: Int, height: Int, gap: Int = 4): List<Rect> = when (this) {
        SINGLE -> listOf(Rect(0, 0, width, height))
        TWO_VERTICAL -> splitCols(width, height, 2, gap)
        TWO_HORIZONTAL -> splitRows(width, height, 2, gap)
        THREE_VERTICAL -> splitCols(width, height, 3, gap)
        THREE_HORIZONTAL -> splitRows(width, height, 3, gap)
        FOUR_GRID -> splitGrid(width, height, 2, 2, gap)
        NINE_GRID -> splitGrid(width, height, 3, 3, gap)
    }

    private fun splitCols(w: Int, h: Int, n: Int, gap: Int): List<Rect> {
        val colW = (w - gap * (n - 1)) / n
        return (0 until n).map { i -> Rect(i * (colW + gap), 0, i * (colW + gap) + colW, h) }
    }

    private fun splitRows(w: Int, h: Int, n: Int, gap: Int): List<Rect> {
        val rowH = (h - gap * (n - 1)) / n
        return (0 until n).map { i -> Rect(0, i * (rowH + gap), w, i * (rowH + gap) + rowH) }
    }

    private fun splitGrid(w: Int, h: Int, cols: Int, rows: Int, gap: Int): List<Rect> {
        val cellW = (w - gap * (cols - 1)) / cols
        val cellH = (h - gap * (rows - 1)) / rows
        val result = mutableListOf<Rect>()
        for (r in 0 until rows) for (c in 0 until cols) {
            val left = c * (cellW + gap)
            val top = r * (cellH + gap)
            result.add(Rect(left, top, left + cellW, top + cellH))
        }
        return result
    }
}
