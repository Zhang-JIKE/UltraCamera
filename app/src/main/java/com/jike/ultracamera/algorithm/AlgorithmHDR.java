package com.jike.ultracamera.algorithm;

public class AlgorithmHDR {

    public static synchronized native String Initialize();

    public static synchronized native void getAffinity();

    public static synchronized native int Release();

    public static synchronized native String HDRConvertFromJpeg(int[] frame, int[] frame_len, int nFrames, int sx,
                                                                int sy);

    public static synchronized native String HDRAddYUVFrames(byte[] frame, int nFrames, int sx, int sy);

    public static synchronized native String HDRPreview(int nFrames, int sx, int sy, int[] pview, int expoPref,
                                                        int colorPref, int ctrstPref, int microPref, int noSegmPref, int noisePref, boolean mirrored);

    public static synchronized native String HDRPreview2(int sx, int sy, int[] pview, boolean mirrored);

    public static synchronized native String HDRPreview2a(int sx, int sy, int[] pview, boolean rotate, int exposure,
                                                          int vividness, int contrast, int microcontrast, boolean mirrored);

    public static synchronized native byte[] HDRProcess(int sx, int sy, int[] crop, int rotate, boolean mirrored);

    public static synchronized native void HDRStopProcessing();

    public static synchronized native void HDRFreeInstance();

    static
    {
        System.loadLibrary("native-lib");
    }
}
