package com.example.android.tflitecamerademo;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.support.constraint.Constraints.TAG;

public class ImageConverter {

    private ByteBuffer imgData = null;

    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_SIZE = 3;

    private static final int DIM_IMG_SIZE_X = 224;
    private static final int DIM_IMG_SIZE_Y = 224;
    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    ImageConverter() {
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
    }

    public ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return null;
        }

        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();

        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.put((byte) ((val >> 16) & 0xFF));
                imgData.put((byte) ((val >> 8) & 0xFF));
                imgData.put((byte) (val & 0xFF));
            }
        }

        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));

        return imgData;
    }
}
