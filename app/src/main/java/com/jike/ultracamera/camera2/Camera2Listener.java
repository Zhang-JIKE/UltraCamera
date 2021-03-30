package com.jike.ultracamera.camera2;

import android.graphics.Rect;

public interface Camera2Listener {
    void onCameraOpened(int w, int h, int camIdx);
    void onCameraClosed(int camIdx);
    void onFaceDetected(Rect[] rects);
}
