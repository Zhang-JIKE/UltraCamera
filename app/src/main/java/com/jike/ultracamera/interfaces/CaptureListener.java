package com.jike.ultracamera.interfaces;

public interface CaptureListener{
    void onCaptureStarted(int duration);
    void onCaptureFinished();
    void onAlgorithmStarted();
    void onAlgorithmFinished();
    void onImageTaken(String path, int idx, int total);
}