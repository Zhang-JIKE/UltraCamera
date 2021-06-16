package com.jike.ultracamera.camera2.module;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import com.jike.ultracamera.R;
import com.jike.ultracamera.camera2.CameraController;
import com.jike.ultracamera.camera2.ImageSaver;
import com.jike.ultracamera.cameradata.CamSetting;

import java.util.ArrayList;
import java.util.List;

public class PictureModule extends BaseModule {

    @Override
    public String getModuleName() {
        return "拍照";
    }

    @Override
    public int getIconResId() {
        return R.drawable.ic_indicator_camera;
    }

    @Override
    protected void onModuleStart() {
        CameraController.getInstance().openCamera();
    }

    @Override
    protected void onModuleStop() {
        CameraController.getInstance().closeCamera();
    }

    @Override
    public void onPictureTaken(
            CameraDevice cameraDevice,
            CameraCaptureSession session,
            CameraCharacteristics characteristics,
            CameraCaptureSession.CaptureCallback callback,
            Surface surface,
            Rect cropRect, int rotation, ImageSaver saver) throws CameraAccessException {

        saver.setSimpleProcessor();
        List<CaptureRequest> captureRequests = new ArrayList<>();

        for(int i = 0; i < saver.getProcessor().getFrameCount(); i++){
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            builder.set(CaptureRequest.SCALER_CROP_REGION,cropRect);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            if(!CamSetting.isDenoiseOpened) {
                builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
            }


            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            builder.addTarget(saver.getImageReader().getSurface());
            captureRequests.add(builder.build());
        }
        session.captureBurst(captureRequests, callback, null);
    }


}
