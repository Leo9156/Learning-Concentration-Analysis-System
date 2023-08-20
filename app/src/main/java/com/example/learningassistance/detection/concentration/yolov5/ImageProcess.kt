package com.example.learningassistance.detection.concentration.yolov5

import androidx.camera.core.ImageProxy

class ImageProcess {
    private val kMaxChannelValue = 262143
    private var yuvBytes: Array<ByteArray?> = arrayOfNulls(3)

    fun fillBytes(planes: Array<ImageProxy.PlaneProxy>) {
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer.get(yuvBytes[i]!!)
        }
    }

    // YUV to RGB
    private fun yuv2Rgb(y: Int, u: Int, v: Int): Int {
        // Adjust and check YUV values
        val adjustedY = if (y - 16 < 0) 0 else y - 16
        val adjustedU = u - 128
        val adjustedV = v - 128

        val y1192 = 1192 * adjustedY
        val r = (y1192 + 1634 * adjustedV)
        val g = (y1192 - 833 * adjustedV - 400 * adjustedU)
        val b = (y1192 + 2066 * adjustedU)

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        val clippedR = if (r > kMaxChannelValue) kMaxChannelValue else if (r < 0) 0 else r
        val clippedG = if (g > kMaxChannelValue) kMaxChannelValue else if (g < 0) 0 else g
        val clippedB = if (b > kMaxChannelValue) kMaxChannelValue else if (b < 0) 0 else b

        return (0xff000000 or (((clippedR shl 6) and 0xff0000).toLong()) or (((clippedG shr 2) and 0xff00).toLong()) or (((clippedB shr 10) and 0xff).toLong())).toInt()
    }

    // YUV to ARGB8888
    fun yuv420ToARGB8888(
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        out: IntArray
    ) {
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j shr 1)

            for (i in 0 until width) {
                val uvOffset = pUV + (i shr 1) * uvPixelStride
                out[yp++] = yuv2Rgb(0xff and yuvBytes[0]!![pY + i].toInt(), 0xff and yuvBytes[1]!![uvOffset].toInt(), 0xff and yuvBytes[2]!![uvOffset].toInt())
            }
        }
    }

    fun resetYuvBytes() {
        yuvBytes = arrayOfNulls(3)
    }
}