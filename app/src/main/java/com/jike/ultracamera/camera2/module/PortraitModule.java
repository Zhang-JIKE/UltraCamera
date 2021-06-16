package com.jike.ultracamera.camera2.module;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import com.jike.ultracamera.R;
import com.jike.ultracamera.camera2.CameraController;
import com.jike.ultracamera.camera2.ImageSaver;
import com.jike.ultracamera.cameradata.CamSetting;
import com.jike.ultracamera.cameradata.CameraPara;

import java.util.ArrayList;
import java.util.List;

public class PortraitModule extends BaseModule{

    @Override
    public String getModuleName() {
        return "人像";
    }

    @Override
    public int getIconResId() {
        return R.drawable.ic_indicator_portrait;
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
    public void onPictureTaken(CameraDevice cameraDevice,
                               CameraCaptureSession session,
                               CameraCharacteristics characteristics,
                               CameraCaptureSession.CaptureCallback callback,
                               Surface surface,
                               Rect cropRect,
                               int rotation,
                               ImageSaver saver) throws CameraAccessException {

        saver.setNightProcessor();
        List<CaptureRequest> buildersSuperNight = new ArrayList<>();
        for (int i = 0; i < saver.getProcessor().getFrameCount(); i++) {
            CaptureRequest.Builder builderSuperNight = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builderSuperNight.set(CaptureRequest.SCALER_CROP_REGION, cropRect);

            builderSuperNight.addTarget(saver.getImageReader().getSurface());
            builderSuperNight.addTarget(surface);

            builderSuperNight.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builderSuperNight.set(CaptureRequest.CONTROL_AWB_LOCK, true);

            if(!CamSetting.isDenoiseOpened) {
                builderSuperNight.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
            }
            builderSuperNight.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST,  200);
            builderSuperNight.set(CaptureRequest.SENSOR_EXPOSURE_TIME, CameraPara.ONE_SECOND_DIV_4);
            builderSuperNight.set(CaptureRequest.SENSOR_SENSITIVITY,
                    (int) ((CameraPara.iso / (CameraPara.ONE_SECOND_DIV_4 / CameraPara.exposureTime)) * CameraPara.isoBoost / 100f) / 2);

            builderSuperNight.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            buildersSuperNight.add(builderSuperNight.build());
        }
        session.captureBurst(buildersSuperNight, callback, null);
    }


}
