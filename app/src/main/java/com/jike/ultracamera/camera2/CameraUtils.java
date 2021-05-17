package com.jike.ultracamera.camera2;

import android.util.Size;

import java.util.Arrays;
import java.util.Comparator;

public class CameraUtils {

    public static Size[] sortSizes(Size[] sizes){
        Comparator<Size> comparator = new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return o2.getWidth() * o2.getHeight() - o1.getWidth() * o1.getHeight();
            }
        };
        Arrays.sort(sizes, comparator);
        return sizes;
    }
}
