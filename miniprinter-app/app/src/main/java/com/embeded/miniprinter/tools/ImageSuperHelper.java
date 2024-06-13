package com.embeded.miniprinter.tools;


import android.graphics.Bitmap;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.Paint;
        import android.graphics.Rect;

import com.embeded.miniprinter.device.PrinterByte;

import java.io.ByteArrayOutputStream;
        import java.util.ArrayList;
        import java.util.List;

public class ImageSuperHelper {

    public static Bitmap convertToPrinterFormat(Bitmap image) {
        Bitmap resizedImage = resizeImage(image, 384);
        if (resizedImage == null) {
            return null;
        }

        Bitmap bwImage = floydSteinbergDithering(resizedImage);
        if (bwImage == null) {
            return null;
        }

        List<byte[]> dataRows = convertToDataRows(bwImage);

        return bwImage;
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

    public static Bitmap floydSteinbergDithering(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap bwImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);


        boolean[] isTextPixel = new boolean[width * height];


        int edgeThreshold = 10;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                int pixelValue = Color.red(pixels[index]);
                int leftValue = Color.red(pixels[index - 1]);
                int rightValue = Color.red(pixels[index + 1]);
                int topValue = Color.red(pixels[index - width]);
                int bottomValue = Color.red(pixels[index + width]);

                if (Math.abs(pixelValue - leftValue) > edgeThreshold ||
                        Math.abs(pixelValue - rightValue) > edgeThreshold ||
                        Math.abs(pixelValue - topValue) > edgeThreshold ||
                        Math.abs(pixelValue - bottomValue) > edgeThreshold) {
                    isTextPixel[index] = true;
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;

                if (isTextPixel[index]) {
                    pixels[index] = Color.rgb(pixels[index] > 127 ? 255 : 0, pixels[index] > 127 ? 255 : 0, pixels[index] > 127 ? 255 : 0);
                    continue;
                }

                int oldPixel = Color.red(pixels[index]);
                int newPixel = oldPixel > 127 ? 255 : 0;
                pixels[index] = Color.rgb(newPixel, newPixel, newPixel);
                int quantError = oldPixel - newPixel;

                if (x < width - 1) {
                    pixels[index + 1] = clampColor(Color.red(pixels[index + 1]) + quantError * 7 / 16);
                }
                if (y < height - 1) {
                    if (x > 0) {
                        pixels[index + width - 1] = clampColor(Color.red(pixels[index + width - 1]) + quantError * 3 / 16);
                    }
                    pixels[index + width] = clampColor(Color.red(pixels[index + width]) + quantError * 5 / 16);
                    if (x < width - 1) {
                        pixels[index + width + 1] = clampColor(Color.red(pixels[index + width + 1]) + quantError * 1 / 16);
                    }
                }
            }
        }

        bwImage.setPixels(pixels, 0, width, 0, 0, width, height);
        return bwImage;
    }

    private static int clampColor(int value) {
        return Math.min(Math.max(value, 0), 255);
    }

    public static List<byte[]> convertToDataRows(Bitmap bwImage) {
        int width = bwImage.getWidth();
        int height = bwImage.getHeight();

        byte[] pixels = new byte[width * height];
        int[] sourcePixels = new int[width * height];
        bwImage.getPixels(sourcePixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < sourcePixels.length; i++) {
            pixels[i] = (byte)(Color.red(sourcePixels[i]) == 0 ? 1 : 0);
        }

        List<byte[]> rows = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            byte[] rowData = new byte[width / 8];
            for (int x = 0; x < width; x++) {
                int byteIndex = x / 8;
                int bitIndex = 7 - (x % 8);
                if (pixels[y * width + x] == 1) {
                    byte mask = (byte)(1 << bitIndex);
                    rowData[byteIndex] |= mask;
                }
            }
//            for(int i = 0;i < rowData.length;i++){
//                rowData[i] = (byte) ~rowData[i];
//            }
            rows.add(rowData);
            PrinterByte.INSTANCE.addSendFrames(rowData);
        }
        PrinterByte.INSTANCE.sendStartPrinter();

        return rows;
    }

}