package dev.kurokawa.transfermovil.expressive.core

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/** "Mi QR": QR for the selected card. Payload is the bank code and the
 *  card PAN joined by ':' (e.g. "BPA:9238…1632"), so another app can
 *  split it back into bank + destination card. Bitmap generation is
 *  CPU work — call it off the UI thread (Dispatchers.Default). */
object QrCode {
    fun payload(bankCode: String, pan: String): String =
        bankCode + ":" + pan.filter { it.isDigit() }

    fun bitmap(content: String, sizePx: Int = 640): Bitmap? = try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2,
        )
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val row = y * sizePx
            for (x in 0 until sizePx) {
                pixels[row + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    } catch (_: Exception) {
        null
    }
}
