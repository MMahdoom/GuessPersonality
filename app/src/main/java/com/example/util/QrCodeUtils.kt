package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeUtils {

    fun buildJoinDeepLink(roomCode: String): String =
        "guesspersonality://join?code=${roomCode.trim().uppercase()}"

    fun buildShareInviteMessage(roomName: String, roomCode: String): String {
        val link = buildJoinDeepLink(roomCode)
        return "Join my Guess the Personality room \"$roomName\"!\nRoom Code: $roomCode\nDirect Join Link: $link"
    }

    fun extractRoomCodeFromScan(rawValue: String): String? {
        val trimmed = rawValue.trim()
        if (trimmed.isBlank()) return null

        // Check deep-link or URL query parameter ?code=ABCDEF
        try {
            val uri = Uri.parse(trimmed)
            val queryCode = uri.getQueryParameter("code")
            if (!queryCode.isNullOrBlank()) {
                val clean = queryCode.uppercase().filter { it.isLetterOrDigit() }
                if (clean.length in 4..10) return clean
            }
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank() && trimmed.contains("/join/", ignoreCase = true)) {
                val clean = lastSegment.uppercase().filter { it.isLetterOrDigit() }
                if (clean.length in 4..10) return clean
            }
        } catch (_: Exception) {
            // Fall through to raw code extraction
        }

        // Bare alphanumeric room code
        val direct = trimmed.uppercase().filter { it.isLetterOrDigit() }
        return if (direct.length in 4..10) direct else null
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            val darkColor = Color.rgb(29, 21, 40) // Deep Aubergine Ink
            val lightColor = Color.WHITE
            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    pixels[rowOffset + x] = if (bitMatrix[x, y]) darkColor else lightColor
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }
}
