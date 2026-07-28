package com.anim.where.am.i.presentation.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object QrCodeGenerator {
    fun encode(text: String, sizePx: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = createBitmap(sizePx, sizePx)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }
}
