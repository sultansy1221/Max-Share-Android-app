package com.portablefileserver.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrGenerator {
    fun make(text: String, size: Int): Bitmap {
        val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 1)
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val px = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            px[y * size + x] = if (m.get(x, y)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    /** نص QR الذي يجعل الهاتف الآخر يتصل بالشبكة مباشرة. */
    fun wifi(ssid: String, pass: String): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
        return "WIFI:T:WPA;S:" + esc(ssid) + ";P:" + esc(pass) + ";;"
    }
}
