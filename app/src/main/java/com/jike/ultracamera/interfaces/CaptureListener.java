package com.jike.ultracamera.interfaces;

public interface CaptureListener{
    void onCaptureStarted(int duration, boolean useIndicator);
    void onCaptureFinished(boolean useIndicator);
    void onAlgorithmStarted(boolean useIndicator);
    void onAllFinished(boolean useIndicator);
    void onImageTaken(String path, int idx, int total);
}