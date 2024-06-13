package com.embeded.miniprinter.tools;

import android.graphics.Bitmap;
import android.graphics.Color;


public class EPaperPicture
{
    private static int srcW, srcH; // Width and height of source image
    private static int dstW, dstH; // Width and height of destination image

    private static Bitmap srcBmp; // Bitmap of source image
    private static Bitmap dstBmp; // Bitmap of destination image

    private static int[] curPal; // Current palette


    private static int[][] palettes = new int[][] // Palettes
            {
                    new int[] { Color.BLACK, Color.WHITE                        }, //1黑白
                    new int[] { Color.BLACK, Color.WHITE, Color.RED             }, //2黑白红
                    new int[] { Color.BLACK, Color.WHITE, Color.GRAY            }, //3黑白灰
                    new int[] { Color.BLACK, Color.WHITE, Color.GRAY, Color.RED }, //4黑白灰红
                    new int[] { Color.BLACK, Color.WHITE                        }, //5黑白
                    new int[] { Color.BLACK, Color.WHITE, Color.YELLOW          }, //6黑白黄
                    new int[] {},                                                  //7
                    new int[] { 0xff000000, 0xffffffff, 0xff00ff00, 0xff0000ff, 0xffff0000, 0xffffff00,0xffff8000}, //8RGB 全彩
            };

    // Return the square error of {r, g, b},
    // that means how far them are from standard color stdCol
    //---------------------------------------------------------
    private static double getErr(double r, double g, double b, int stdCol)
    {
        r -= Color.red  (stdCol);
        g -= Color.green(stdCol);
        b -= Color.blue (stdCol);

        return r*r + g*g + b*b;
    }

    // Return the index of current palette color which is
    // nearest to the {r, g, b}
    //---------------------------------------------------------
    private static int getNear(double r, double g, double b)
    {
        int ind = 0;
        double err = getErr(r, g, b, curPal[0]);

        for (int i = 1; i < curPal.length; i++)
        {
            double cur = getErr(r, g, b, curPal[i]);
            if (cur < err) { err = cur; ind = i; }
        }

        return ind;
    }

    // Return the index of current palette color which is
    // nearest to the color clr
    //---------------------------------------------------------
    private static int getNear(int clr)
    {
        return getNear(Color.red(clr), Color.green(clr), Color.blue(clr));
    }

    // Adding of color {r, g, b} into e color array with
    // weight k. Here every r, g or b channel takes one cell
    // in e color array and can have any integer value.
    //---------------------------------------------------------
    private static void addVal(double[] e, int i, double r, double g, double b, double k)
    {
        int index = i * 3;
        e[index    ] = (r * k) / 16 + e[index    ];
        e[index + 1] = (g * k) / 16 + e[index + 1];
        e[index + 2] = (b * k) / 16 + e[index + 2];
    }

    // Returns a color from the current palette
    // which is nearest to source bitmap pixel at (x, y), or
    // returns default color if (x, y) is out of the bitmap
    //---------------------------------------------------------
    private static int nearColor(int x, int y)
    {
        if ((x >= srcW) || (y >= srcH)) return curPal[(x + y) % 2 == 0 ? 1 : 0];
        return curPal[getNear(srcBmp.getPixel(x, y))];
    }


    //originalImage 原始图片
    //isLvl 1 色阶算法 0 抖动算法
    //width 输出文件宽度
    //height 输出文件高度
    //index_t  可以选择6种数据格式 palettes
    public static Bitmap createIndexedImage(Bitmap originalImage,boolean isLvl,int width,int height,int index_t)
    {
        //EPaperDisplay epd = EPaperDisplay.getDisplays()[EPaperDisplay.epdInd];
        srcBmp = originalImage;
        dstBmp = Bitmap.createBitmap(width,height, srcBmp.getConfig());

        int palInd = index_t;
        // if (!isRed) palInd = palInd & 0xE;
        curPal = palettes[palInd];

        dstW = dstBmp.getWidth();
        dstH = dstBmp.getHeight();

        srcW = srcBmp.getWidth();
        srcH = srcBmp.getHeight();

        int[] srcArr = new int[srcW * srcH];
        int[] dstArr = new int[dstW * dstH];

        int index = 0;
        srcBmp.getPixels(srcArr, 0, srcW, 0, 0, srcW, srcH);

        if (isLvl) //色阶算法
        {
            for (int y = 0; y < dstH; y++)
                for (int x = 0; x < dstW; x++)
                    dstArr[index++] = nearColor(x, y);
        }
        else  //抖动算法
        {

            int aInd = 0;
            int bInd = 1;

            double[][] errArr = new double[2][];

            errArr[0] = new double[3*dstW];
            errArr[1] = new double[3*dstW];

            for (int i = 0; i < dstW; i++)
            {
                errArr[bInd][3*i    ] = 0;
                errArr[bInd][3*i + 1] = 0;
                errArr[bInd][3*i + 2] = 0;
            }

            for (int j = 0; j < dstH; j++)
            {
                if (j >= srcH)
                {
                    for (int i = 0; i < dstW; i++, index++)
                        dstArr[index] = curPal[(i + j) % 2 == 0 ? 1 : 0];
                    continue;
                }

                aInd = ((bInd = aInd) + 1) & 1;

                for (int i = 0; i < dstW; i++)
                {
                    errArr[bInd][3*i    ] = 0;
                    errArr[bInd][3*i + 1] = 0;
                    errArr[bInd][3*i + 2] = 0;
                }

                for (int i = 0; i < dstW; i++)
                {
                    if (i >= srcW)
                    {
                        dstArr[index++] = curPal[(i + j) % 2 == 0 ? 1 : 0];
                        continue;
                    }

                    int srcPix = srcArr[j * srcW + i];

                    double r = Color.red  (srcPix) + errArr[aInd][3*i    ];
                    double g = Color.green(srcPix) + errArr[aInd][3*i + 1];
                    double b = Color.blue (srcPix) + errArr[aInd][3*i + 2];

                    int colVal = curPal[getNear(r, g, b)];
                    dstArr[index++] = colVal;

                    r -= Color.red  (colVal);
                    g -= Color.green(colVal);
                    b -= Color.blue (colVal);

                    if (i == 0)
                    {
                        addVal(errArr[bInd], (i    ), r, g, b, 7.0);
                        addVal(errArr[bInd], (i + 1), r, g, b, 2.0);
                        addVal(errArr[aInd], (i + 1), r, g, b, 7.0);
                    }
                    else if (i == dstW - 1)
                    {
                        addVal(errArr[bInd], (i - 1), r, g, b, 7.0);
                        addVal(errArr[bInd], (i    ), r, g, b, 9.0);
                    }
                    else
                    {
                        addVal(errArr[bInd], (i - 1), r, g, b, 3.0);
                        addVal(errArr[bInd], (i    ), r, g, b, 5.0);
                        addVal(errArr[bInd], (i + 1), r, g, b, 1.0);
                        addVal(errArr[aInd], (i + 1), r, g, b, 7.0);
                    }
                }
            }
        }

        // Put converted pixels into destination image bitmap
        //-----------------------------------------------------
        dstBmp.setPixels(dstArr, 0, dstW, 0, 0, dstW, dstH);
        return dstBmp;
    }

    // Returns the index of color in palette
    //-----------------------------------------------------
    public int getVal(int color)
    {
        int r = Color.red(color);
        int b = Color.blue(color);

        if((r == 0xFF) && (b == 0xFF)) return 1;
        if((r == 0x7F) && (b == 0x7F)) return 2;
        if((r == 0xFF) && (b == 0x00)) return 3;

        return 0;
    }
    // Returns the index of color in palette just for 5.65f e-Paper
    //-----------------------------------------------------
    public int getVal_7color(int color)
    {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        if((r == 0x00) && (g == 0x00) && (b == 0x00)) return 0;
        if((r == 0xFF) && (g == 0xFF) && (b == 0xFF)) return 1;
        if((r == 0x00) && (g == 0xFF) && (b == 0x00)) return 2;
        if((r == 0x00) && (g == 0x00) && (b == 0xFF)) return 3;
        if((r == 0xFF) && (g == 0x00) && (b == 0x00)) return 4;
        if((r == 0xFF) && (g == 0xFF) && (b == 0x00)) return 5;
        if((r == 0xFF) && (g == 0x80) && (b == 0x00)) return 6;
        return 7;
    }


}

