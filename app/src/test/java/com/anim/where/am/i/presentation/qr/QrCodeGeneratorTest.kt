package com.anim.where.am.i.presentation.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeGeneratorTest {
    @Test fun encodesNonEmptyMatrix() {
        val matrix = MultiFormatWriter().encode("whereami://config?id=1", BarcodeFormat.QR_CODE, 64, 64)
        assertTrue(matrix.width == 64 && matrix.height == 64)
    }
}
