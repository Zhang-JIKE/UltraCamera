package com.jike.ultracamera.device;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.jike.ultracamera.executor.JobExecutor;
import com.jike.ultracamera.utils.Utils;

public abstract class DeviceManager {

    private String TAG = Utils.getClassName(DeviceManager.class);

    protected CameraManager mCameraManager;
    protected CameraDevice mCameraDevice;
    protected JobExecutor mExecutor;

    public DeviceManager(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mExecutor = new JobExecutor();
    }

    public CameraCharacteristics getCharacteristics(String cameraId) {
        try {
            return mCameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getCameraIdList() {
        try {
            return mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void openCamera() {
        mExecutor.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                //openDevice();
            }
        });
    }

    public void closeCamera() {
        mExecutor.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                closeCamera();
            }
        });
    }


    @SuppressLint("MissingPermission")
    private synchronized void openDevice(String cameraId, Handler handler) {
        try {
            mCameraManager.openCamera(cameraId, stateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeDevice() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public static abstract class CameraListener{
        public void onInit(){}
        public void onReadFinished(){}
        public void onStartToOpen(){}
        public void onStartPreview(){}
        public void onOpenFinished(){}
        public void onClosed(){}
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
        }
    };


}
