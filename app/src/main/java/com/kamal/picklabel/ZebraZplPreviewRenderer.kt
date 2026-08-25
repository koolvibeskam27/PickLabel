package com.kamal.picklabel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


class ZebraZplPreviewRenderer {

    fun renderPreview(zpl: String): Bitmap {
        val lines = zpl.split("\n")
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
        }

        val width = 600
        val height = lines.size * 50 + 100

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = 60f
        for (line in lines) {
            canvas.drawText(line, 20f, y, paint)
            y += 50f
        }

        return bitmap
    }
}
