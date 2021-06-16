package com.jike.ultracamera.camera2.module;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;

import com.jike.ultracamera.camera2.CameraController;
import com.jike.ultracamera.camera2.ImageSaver;

public abstract class BaseModule {

    public abstract String getModuleName();
    public abstract int getIconResId();

    protected abstract void onModuleStart();
    protected abstract void onModuleStop();

    public void startModule(){
        onModuleStart();
    }

    public void stopModule(){
        onModuleStop();
    }

    public abstract void onPictureTaken(
            CameraDevice cameraDevice,
            CameraCaptureSession session,
            CameraCharacteristics characteristics,
            CameraCaptureSession.CaptureCallback callback,
            Surface surface,
            Rect cropRect,
            int rotation,
            ImageSaver saver) throws CameraAccessException;
}
