package com.awakedrive.drowsedetect.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrowsinessAnalyzerTest {
    private val base = Baseline(ear = 0.30f, mar = 0f, pitchDeg = 0f)
    private val frameMs = 66L

    /** Bộ mô phỏng: đẩy frame 15fps với timestamp tăng dần. */
    private class Sim(val analyzer: DrowsinessAnalyzer, val frameMs: Long) {
        var t = 1_000L
        var last: DrowsinessResult? = null
        val seen = mutableSetOf<DrowsinessState>()

        fun feed(
            ms: Long,
            ear: Float = 0.30f,
            jaw: Float = 0f,
            pitch: Float = 0f,
            yaw: Float = 0f,
            face: Boolean = true,
        ) {
            val end = t + ms
            while (t < end) {
                val f = if (face) {
                    FaceFeatures(t, true, earAvg = ear, jawOpen = jaw, pitchDeg = pitch, yawDeg = yaw)
                } else FaceFeatures.noFace(t)
                last = analyzer.process(f)
                seen += last!!.state
                t += frameMs
            }
        }
    }

    private fun sim() = Sim(DrowsinessAnalyzer(baseline = base), frameMs)

    @Test fun `mat mo binh thuong khong bao`() {
        val s = sim()
        s.feed(90_000)
        assertEquals(setOf(DrowsinessState.NORMAL), s.seen)
    }

    @Test fun `chop mat binh thuong khong bao`() {
        val s = sim()
        repeat(25) { s.feed(3_850); s.feed(150, ear = 0.05f) } // ~3.75% thoi gian nham
        assertEquals(setOf(DrowsinessState.NORMAL), s.seen)
        assertTrue(s.last!!.perclos < 0.15f)
    }

    @Test fun `nham mat 1_5s la DANGER microsleep`() {
        val s = sim()
        s.feed(5_000)
        s.feed(1_000, ear = 0.05f)
        assertEquals(DrowsinessState.NORMAL, s.last!!.state)
        s.feed(300, ear = 0.05f) // tong 1.3s > 1.2s
        assertEquals(DrowsinessState.DANGER, s.last!!.state)
        assertEquals(AlertReason.MICROSLEEP, s.last!!.reason)
    }

    @Test fun `blendshape blink cung tinh la nham`() {
        val a = DrowsinessAnalyzer(baseline = base)
        var r: DrowsinessResult? = null
        var t = 0L
        while (t < 1_500) {
            r = a.process(FaceFeatures(t, true, earAvg = 0.30f, blinkLeft = 0.9f, blinkRight = 0.9f)); t += frameMs
        }
        assertEquals(DrowsinessState.DANGER, r!!.state)
    }

    @Test fun `PERCLOS 20 phan tram la WARNING`() {
        val s = sim()
        // chu ky 5s: nham 1.0s (< microsleep) -> 20%
        repeat(10) { s.feed(4_000); s.feed(1_000, ear = 0.05f) }
        assertEquals(DrowsinessState.WARNING, s.last!!.state)
        assertEquals(AlertReason.PERCLOS, s.last!!.reason)
        assertFalse(DrowsinessState.DANGER in s.seen)
    }

    @Test fun `PERCLOS chua danh gia khi cua so duoi 30s`() {
        val s = sim()
        repeat(4) { s.feed(500); s.feed(1_000, ear = 0.05f) } // 66% nhung moi co ~6s
        assertFalse(s.last!!.perclos > 0f)
    }

    @Test fun `3 lan ngap la WARNING`() {
        val s = sim()
        repeat(3) { s.feed(2_300, jaw = 0.8f); s.feed(8_000) }
        assertEquals(DrowsinessState.WARNING, s.last!!.state)
        assertEquals(AlertReason.YAWN, s.last!!.reason)
        assertEquals(3, s.last!!.yawnCount)
    }

    @Test fun `ngap ngan duoi 2s khong tinh`() {
        val s = sim()
        repeat(5) { s.feed(1_000, jaw = 0.8f); s.feed(3_000) }
        assertEquals(0, s.last!!.yawnCount)
    }

    @Test fun `2 lan gat dau trong 60s la WARNING`() {
        val s = sim()
        repeat(2) { s.feed(800, pitch = -25f); s.feed(5_000) }
        assertEquals(DrowsinessState.WARNING, s.last!!.state)
        assertEquals(AlertReason.HEAD_NOD, s.last!!.reason)
    }

    @Test fun `cui dau keo dai kem nham mat la DANGER`() {
        val s = sim()
        s.feed(2_000)
        s.feed(1_000, pitch = -30f) // dau cui, mat mo -> chua bao
        assertNotEquals(DrowsinessState.DANGER, s.last!!.state)
        s.feed(700, pitch = -30f, ear = 0.05f)
        assertEquals(DrowsinessState.DANGER, s.last!!.state)
    }

    @Test fun `hysteresis chi ha cap sau 5s`() {
        val s = sim()
        s.feed(2_000)
        s.feed(1_500, ear = 0.05f)
        assertEquals(DrowsinessState.DANGER, s.last!!.state)
        s.feed(4_500)
        assertEquals(DrowsinessState.DANGER, s.last!!.state)
        s.feed(1_000)
        assertEquals(DrowsinessState.NORMAL, s.last!!.state)
        assertNotNull(s.last)
    }

    @Test fun `quay dau lon thi khong tinh nham mat`() {
        val s = sim()
        s.feed(5_000, ear = 0.05f, yaw = 50f)
        assertEquals(setOf(DrowsinessState.NORMAL), s.seen)
    }

    @Test fun `mat mat qua 3s la NO_FACE va tu hoi phuc`() {
        val s = sim()
        s.feed(2_000)
        s.feed(2_000, face = false)
        assertEquals(DrowsinessState.NORMAL, s.last!!.state) // mat tam thoi chua bao
        s.feed(1_500, face = false)
        assertEquals(DrowsinessState.NO_FACE, s.last!!.state)
        s.feed(500)
        assertEquals(DrowsinessState.NORMAL, s.last!!.state)
    }

    private fun assertNotEquals(a: Any, b: Any) = assertTrue("expected not $a", a != b)
}
