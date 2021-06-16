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
import com.jike.ultracamera.cameradata.CamPara;
import com.jike.ultracamera.cameradata.CamSetting;
import com.jike.ultracamera.cameradata.CameraPara;

import java.util.ArrayList;
import java.util.List;

public class FusionModule extends BaseModule{

    @Override
    public String getModuleName() {
        return "融合";
    }

    @Override
    public int getIconResId() {
        return R.drawable.ic_indicator_fusion;
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

        saver.setFusionProcessor();
        List<CaptureRequest> buildersSuperRes = new ArrayList<>();

        for (int i = 0; i < saver.getProcessor().getFrameCount(); i++) {
            CaptureRequest.Builder builderSuperRes = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builderSuperRes.set(CaptureRequest.SCALER_CROP_REGION, cropRect);

            builderSuperRes.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builderSuperRes.set(CaptureRequest.CONTROL_AWB_LOCK, true);
            builderSuperRes.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);

            builderSuperRes.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100);
            long expTime = (long) (CameraPara.exposureTime * (int) (CameraPara.iso / 50f)*CameraPara.isoBoost/100f);

            /*if(CamSetting.isNightOpened) {
                expTime = CamPara.timeIncrease(expTime, 2);
            }*/

            builderSuperRes.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
            builderSuperRes.set(CaptureRequest.SENSOR_SENSITIVITY, 50*4);

            builderSuperRes.addTarget(saver.getImageReader().getSurface());
            builderSuperRes.addTarget(surface);
            builderSuperRes.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            buildersSuperRes.add(builderSuperRes.build());
        }
        session.captureBurst(buildersSuperRes, callback, null);

    }


}
