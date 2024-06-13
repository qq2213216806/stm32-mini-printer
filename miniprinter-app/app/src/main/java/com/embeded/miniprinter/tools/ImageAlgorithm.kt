package com.embeded.miniprinter.tools

import android.graphics.Bitmap

object ImageAlgorithm {

    fun convertGreyImgByFloyd(img: Bitmap): Bitmap {
        val width = img.width //获取位图的宽
        val height = img.height //获取位图的高
        val pixels = IntArray(width * height) //通过位图的大小创建像素点数组
        img.getPixels(pixels, 0, width, 0, 0, width, height)
        val gray = IntArray(height * width)
        for (i in 0 until height) {
            for (j in 0 until width) {
                val grey = pixels[width * i + j]
                val red = grey and 0x00FF0000 shr 16
                gray[width * i + j] = red
            }
        }

        var e = 0
        for (i in 0 until height) {
            for (j in 0 until width) {
                val g = gray[width * i + j]
                if (g >= 128) {
                    pixels[width * i + j] = -0x1
                    e = g - 255
                } else {
                    pixels[width * i + j] = -0x1000000
                    e = g - 0
                }
                if (j < width - 1 && i < height - 1) { //右边像素处理
                    gray[width * i + j + 1] += 3 * e / 8 //下
                    gray[width * (i + 1) + j] += 3 * e / 8 //右下
                    gray[width * (i + 1) + j + 1] += e / 4
                } else if (j == width - 1 && i < height - 1) {//靠右或靠下边的像素的情况
                    //下方像素处理
                    gray[width * (i + 1) + j] += 3 * e / 8
                } else if (j < width - 1 && i == height - 1) {
                    //右边像素处理
                    gray[width * i + j + 1] += e / 4
                }
            }
        }

        val mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
//        mBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
//        imageView5.setImageBitmap(mBitmap)
//        saveBmp(mBitmap)
        return mBitmap
    }
}