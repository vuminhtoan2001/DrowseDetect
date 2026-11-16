package com.awakedrive.drowsedetect.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibratorAndExtractorTest {
    @Test fun `median loai bo chop mat`() {
        val c = Calibrator()
        var t = 0L
        var i = 0
        while (t <= 15_000) {
            val blink = i % 30 == 0
            c.add(FaceFeatures(t, true, earAvg = if (blink) 0.05f else 0.31f, mar = 0.05f, pitchDeg = 3f))
            t += 66; i++
        }
        assertTrue(c.isDone(t))
        val r = c.result() as CalibrationResult.Success
        assertEquals(0.31f, r.baseline.ear, 1e-4f)
        assertEquals(3f, r.baseline.pitchDeg, 1e-4f)
    }

    @Test fun `thieu mat thi bao loi`() {
        val c = Calibrator()
        var t = 0L
        while (t <= 15_000) {
            c.add(if ((t / 66) % 2 == 0L) FaceFeatures(t, true, earAvg = 0.3f) else FaceFeatures.noFace(t))
            t += 66
        }
        val r = c.result() as CalibrationResult.Failure
        assertEquals(CalibrationResult.Reason.NOT_ENOUGH_FACE, r.reason)
    }

    @Test fun `EAR tinh dung tren landmark gia lap`() {
        val xs = FloatArray(478)
        val ys = FloatArray(478)
        // Mat trai: rong 0.10 (p1=33 .. p4=133), cao 0.03 -> EAR = 0.03/0.10 = 0.3 (anh vuong 100x100)
        xs[33] = 0.30f; ys[33] = 0.50f
        xs[133] = 0.40f; ys[133] = 0.50f
        xs[160] = 0.33f; ys[160] = 0.485f; xs[144] = 0.33f; ys[144] = 0.515f
        xs[158] = 0.37f; ys[158] = 0.485f; xs[153] = 0.37f; ys[153] = 0.515f
        // Mat phai tuong tu
        xs[362] = 0.60f; ys[362] = 0.50f
        xs[263] = 0.70f; ys[263] = 0.50f
        xs[385] = 0.63f; ys[385] = 0.485f; xs[380] = 0.63f; ys[380] = 0.515f
        xs[387] = 0.67f; ys[387] = 0.485f; xs[373] = 0.67f; ys[373] = 0.515f
        // Mieng: 78..308 rong 0.2, 13..14 cach 0.04 -> MAR 0.2
        xs[78] = 0.4f; ys[78] = 0.8f; xs[308] = 0.6f; ys[308] = 0.8f
        xs[13] = 0.5f; ys[13] = 0.78f; xs[14] = 0.5f; ys[14] = 0.82f

        val f = FeatureExtractor.extract(
            1L, xs, ys, 100, 100, mapOf("eyeBlinkLeft" to 0.2f, "jawOpen" to 0.4f), matrix = null,
        )
        assertEquals(0.3f, f.earAvg, 1e-3f)
        assertEquals(0.2f, f.mar, 1e-3f)
        assertEquals(0.2f, f.blinkLeft, 1e-6f)
        assertEquals(0.4f, f.jawOpen, 1e-6f)
    }

    @Test fun `euler tu ma tran don vi la 0`() {
        val m = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }
        val e = FeatureExtractor.eulerFromMatrix(m)
        assertEquals(0f, e[0], 1e-4f); assertEquals(0f, e[1], 1e-4f); assertEquals(0f, e[2], 1e-4f)
    }
}
