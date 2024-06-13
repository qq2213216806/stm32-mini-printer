package com.embeded.miniprinter.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.util.Log;

import com.embeded.miniprinter.device.PrinterByte;

public class ImageFilter {

    private static final String TAG = "ImageFilter";

    //获取灰度图片
    public static Bitmap getHuiDu(Bitmap bitMap) {

        int width = bitMap.getWidth();//Width 图片宽度
        int height = bitMap.getHeight();// Height 图片高度

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        // bmpGrayscale 设置灰度之后的bitmap对象

        //设置灰度
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bitMap, 0, 0, paint);
        c.save();
        c.restore();
        return bmpGrayscale;
    }

    /**
     * 将彩色图转换为纯黑白二色
     *
     * @param
     * @return 返回转换好的位图
     */
    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                //分离三原色
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                //转化成灰度像素
                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        //新建图片
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        //设置图片数据
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);

        Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, 380, 460);
        return resizeBmp;
    }

    public static Bitmap convertToDithering(Bitmap bmp){
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        Log.i(TAG,"width = "+width + " "+height);
        return EPaperPicture.createIndexedImage(bmp,false,width,height,0);
    }

    public static Bitmap resizeImage(Bitmap image, float newWidth) {
        if (newWidth == image.getWidth()) {
            return image;
        }
        float scale = newWidth / image.getWidth();
        float newHeight = image.getHeight() * scale;

        Bitmap resizedBitmap = Bitmap.createBitmap((int)newWidth, (int)newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resizedBitmap);
        Rect dstRect = new Rect(0, 0, (int)newWidth, (int)newHeight);
        canvas.drawBitmap(image, null, dstRect, null);

        return resizedBitmap;
    }


    //Bitmap二值化
    public static Bitmap convertToBMW(Bitmap bmp,Boolean printerState,int tmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        Log.i(TAG,"width = "+width + " "+height);
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        // 二值化参考值（中间比较值）二值化的域值 设定二值化的域值，默认值为100
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        String colorBit;
        for (int i = 0; i < height; i++) {
            StringBuilder stringBuilder = new StringBuilder();    //创建一个 StringBuilder 对象，用来储存矩阵数据
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                // 分离三原色
                alpha = ((grey & 0xFF000000) >> 24);
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);
                int i1 = 0xFFFF;

                if (red > tmp) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (blue > tmp) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                if (green > tmp) {
                    green = 255;
                } else {
                    green = 0;
                }
                pixels[width * i + j] = alpha << 24 | red << 16 | green << 8
                        | blue;
                if (pixels[width * i + j] == -1) {
                    pixels[width * i + j] = -1;
                    colorBit = ".";
                    if(printerState)
                        PrinterByte.INSTANCE.addByteArr(false,false);
                } else {
                    pixels[width * i + j] = -16777216;
                    colorBit = "0";
                    if(printerState)
                        PrinterByte.INSTANCE.addByteArr(false,true);
                }
                stringBuilder.append(colorBit);
            }
            if(printerState)
                PrinterByte.INSTANCE.addByteArr(true,false);
            Log.i(TAG,"length="+stringBuilder.length()+" "+stringBuilder);
        }
        if(printerState)
            PrinterByte.INSTANCE.sendStartPrinter();

        // 新建图片
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 设置图片数据
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, width, height);

        return resizeBmp;
    }
}