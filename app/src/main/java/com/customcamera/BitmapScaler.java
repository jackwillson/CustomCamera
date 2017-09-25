package com.customcamera;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class BitmapScaler {
    public static Bitmap scaleToFitWidth(Bitmap b, int width) {
        return Bitmap.createScaledBitmap(b, width, (int) (((float) b.getHeight()) * (((float) width) / ((float) b.getWidth()))), true);
    }

    public static Bitmap scaleToFitHeight(Bitmap b, int height) {
        return Bitmap.createScaledBitmap(b, (int) (((float) b.getWidth()) * (((float) height) / ((float) b.getHeight()))), height, true);
    }

    public static Bitmap scaleToFill(Bitmap b, int width, int height) {
        float factorToUse;
        float factorH = ((float) height) / ((float) b.getWidth());
        float factorW = ((float) width) / ((float) b.getWidth());
        if (factorH > factorW) {
            factorToUse = factorW;
        } else {
            factorToUse = factorH;
        }
        return Bitmap.createScaledBitmap(b, (int) (((float) b.getWidth()) * factorToUse), (int) (((float) b.getHeight()) * factorToUse), true);
    }

    public static Bitmap strechToFill(Bitmap b, int width, int height) {
        return Bitmap.createScaledBitmap(b, (int) (((float) b.getWidth()) * (((float) width) / ((float) b.getWidth()))), (int) (((float) b.getHeight()) * (((float) height) / ((float) b.getHeight()))), true);
    }

    public static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
