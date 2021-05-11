
package com.jike.ultracamera.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.cameradata.CamMode;
import com.jike.ultracamera.cameradata.CamPara;
import com.jike.ultracamera.cameradata.CamRates;
import com.jike.ultracamera.cameradata.CamSetting;
import com.jike.ultracamera.cameradata.CameraParameter;
import com.jike.ultracamera.cameradata.CameraResolution;
import com.jike.ultracamera.helper.CaptureListenerHelper;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Camera2Controller{

    private static Camera2Controller camera2Controller;

    private Camera2Controller(){

    }

    public static Camera2Controller getInstance(){
        if(camera2Controller == null){
            camera2Controller = new Camera2Controller();
        }
        return camera2Controller;
    }

    private String[] mCameraIdList;
    private String[] mPhyCameraIdList;
    public int curPhyCameraIdx = 0;

    public int curCameraIdx = 0;
    private int backCameraIdx = 0;
    private int frontCameraIdx = 0;
    public boolean isFacingFront = false;

    private int viewHeight;
    private int viewWidth;
    private float mScaleTime = 1;
    private int rotation = 0;



    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mCameraCaptureSession;

    public CameraParameter mCameraParameter = CameraParameter.getInstance();
    public CameraResolution mCameraResolution = CameraResolution.getInstance();

    private CaptureRequest.Builder previewBuilder;
    private CaptureRequest previewRequest;
    private CaptureResult captureResult;
    
    private SurfaceTexture texture;
    private Surface surface;

    private ImageSaver imageSaver;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public void initCameraController(int viewWidth, int viewHeight, SurfaceTexture texture) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.texture = texture;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public static CameraDevice getCameraDevice() {
        return Camera2Controller.getInstance().mCameraDevice;
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return mCameraCharacteristics;
    }

    public CaptureResult getCaptureResult() {
        return captureResult;
    }

    private CameraCaptureSession.CaptureCallback previewCallback = new CameraCaptureSession.CaptureCallback(){
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            if(CamSetting.isFaceDetectOpend) {
                faceDetect(result);
            }
            try {
                mCameraParameter.exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                mCameraParameter.iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
                mCameraParameter.isoBoost = result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST);
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    };

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        int curIndex = 1;

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            Log.e("onCaptureCompleted","onCaptureCompleted");
            captureResult = result;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            Log.e("onCaptureProgressed","onCaptureProgressed");

            if(curIndex == 1){
                CaptureListenerHelper.getListener().onCaptureStarted(
                        (int) (request.get(CaptureRequest.SENSOR_EXPOSURE_TIME) / 1000000 * imageSaver.getProcessor().getFrameCount()),
                        imageSaver.getProcessor().isNeedShutterIndicator());
            }
            curIndex++;
            if(curIndex > imageSaver.getProcessor().getFrameCount()) curIndex = 1;
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            Log.e("onCaptureSequenceCompleted","onCaptureSequenceCompleted");
            CaptureListenerHelper.getListener().onCaptureFinished(imageSaver.getProcessor().isNeedShutterIndicator());
            curIndex = 1;
        }
    };

    private final CameraDevice.StateCallback cameraDeviceStateListener = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private CameraCaptureSession.StateCallback captureSessionStateListener = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCameraCaptureSession = cameraCaptureSession;
            try {
                previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                previewBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                previewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                previewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

                setCameraParams();

                previewBuilder.addTarget(surface);

                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), previewCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(
                @NonNull CameraCaptureSession cameraCaptureSession) {
        }
    };
    
    private void createCameraPreviewSession() {
        try {
            texture.setDefaultBufferSize(mCameraResolution.getPicSize().getWidth(),
                    mCameraResolution.getPicSize().getHeight());

            surface = new Surface(texture);

            imageSaver = new ImageSaver(mCameraResolution.getPicSize().getWidth(),
                    mCameraResolution.getPicSize().getHeight(),
                    CamSetting.isRaw ? ImageFormat.RAW_SENSOR : ImageFormat.JPEG);

            if(mPhyCameraIdList.length <= 1) {
                mCameraDevice.createCaptureSession(Arrays.asList(surface,
                        imageSaver.getImageReader().getSurface()),
                        captureSessionStateListener,
                        mBackgroundHandler
                );
            }else {
                OutputConfiguration configuration1 = new OutputConfiguration(surface);
                configuration1.setPhysicalCameraId(mPhyCameraIdList[curPhyCameraIdx]);

                OutputConfiguration configuration2 = new OutputConfiguration(imageSaver.getImageReader().getSurface());
                configuration2.setPhysicalCameraId(mPhyCameraIdList[curPhyCameraIdx]);

                mCameraDevice.createCaptureSessionByOutputConfigurations(Arrays.asList(configuration1,
                        configuration2),
                        captureSessionStateListener,
                        mBackgroundHandler
                );
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void loadCameraLists(){
        mCameraManager = (CameraManager) AppContextUtils.getAppActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIdList = mCameraManager.getCameraIdList();

            for(int i = 0; i < mCameraIdList.length; i++) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraIdList[i]);
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT){
                    frontCameraIdx = i;
                    break;
                }
            }

            if(isFacingFront) {
                curCameraIdx = frontCameraIdx;
            }else {
                curCameraIdx = backCameraIdx;
            }
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraIdList[curCameraIdx])6
            Object[] objects = mCameraCharacteristics.getPhysicalCameraIds().toArray();
            mPhyCameraIdList = new String[objects.length];
            for (int i = 0; i < objects.length; i++){
                mPhyCameraIdList[i] = (String) objects[i];
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    
    private void loadCameraSize(){
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mCameraResolution.setPicSizes(Arrays.asList(map.getOutputSizes(CamSetting.isRaw ? ImageFormat.RAW_SENSOR : ImageFormat.JPEG)));
        mCameraResolution.setVideoSizes(Arrays.asList(map.getOutputSizes(MediaRecorder.class)));
        CamRates.rawFpsRanges = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        CamSetting.mFlashSupported = available == null ? false : available;
    }

    private void setCameraParams(){
        Range<Integer> fpsRange = CamRates.rawFpsRanges[CamRates.rawFpsRanges.length-2];

        if(CamRates.isForcedOpen60Fps){
            fpsRange = new Range<>(60,60);
        }
        previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

        int[] faceDetectModes = mCameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        for(int i : faceDetectModes){
            if(i ==CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE && CamSetting.isFaceDetectOpend) {
                previewBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE);
                break;
            }
        }
    }

    private void faceDetect(TotalCaptureResult result){
        Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
        Point[] points = new Point[faces.length];
        Rect rectSensor = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if(faces!=null) {
            final Rect[] rects = new Rect[faces.length];
            int screenW = viewWidth;
            int screenH = viewHeight;
            int i = 0;
            for (Face face : faces) {
                Point point = face.getRightEyePosition();
                Rect faceBounds = face.getBounds();
                int l = (int) (screenH * ((float) faceBounds.left / rectSensor.width()));
                int t = (int) (screenW * ((float) faceBounds.top / rectSensor.height()));
                int r = (int) (screenH * ((float) faceBounds.right / rectSensor.width()));
                int b = (int) (screenW * ((float) faceBounds.bottom / rectSensor.height()));

                Rect rect = new Rect( screenW - b,l,screenW-t,r);
                if(point!=null) {
                    point.x = (int) (screenH * ((float) point.x / mCameraResolution.getPicSize().getWidth()));
                    point.y = (int) (screenW - screenW * ((float) point.y / mCameraResolution.getPicSize().getHeight()));
                    points[i] = point;
                }
                rects[i]=rect;
                i++;
            }
        }
    }

    private Runnable detectRunnable = new Runnable() {
        @Override
        public void run() {
            //brightNessDetect();
            if(mBackgroundThread.isAlive()&&mBackgroundHandler!=null) {
                mBackgroundHandler.postDelayed(detectRunnable,500);
            }
        }
    };

    public void setFocus(Point point){
        
        Rect size = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        float realPreviewWidth = size.height();
        float realPreviewHeight = size.width();

        //根据预览像素与拍照最大像素的比例，调整手指点击的对焦区域的位置
        float focusX = (float) realPreviewWidth / viewWidth * point.x;
        float focusY = (float) realPreviewHeight / viewHeight * point.y;

        Rect totalPicSize = previewBuilder.get(CaptureRequest.SCALER_CROP_REGION);

        Log.e("CFocus","x"+focusX+"y"+focusY);
        float cutDx = 0;//(totalPicSize.height() - size.height()) / 2;
        Rect rect2 = new Rect((int)focusY,
                (int)realPreviewWidth - (int)focusX,
                (int)(focusY + 1000),
                (int)realPreviewWidth - (int)(focusX) + 1000);

        Log.e("CFocus","l:"+rect2.left+"t:"+rect2.top);

        previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect2,1000)});
        previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect2,1000)});
        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        previewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

        try {
            mCameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setScaleTime(float time){
        mScaleTime = time;
        Rect rectSensor = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int pixW = rectSensor.width();
        int pixH = rectSensor.height();
        previewBuilder.set(CaptureRequest.SCALER_CROP_REGION,new Rect(
                (int)((pixW-pixW/mScaleTime)*0.5),
                (int)((pixH-pixH/mScaleTime)*0.5),
                (int)((pixW+pixW/mScaleTime)*0.5),
                (int)((pixH+pixH/mScaleTime)*0.5)));
        previewRequest = previewBuilder.build();

        try {
            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(detectRunnable);
    }

    public void stopBackgroundThread() {
        if(mBackgroundThread!=null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void takePicture2(){

    }

    public void takePicture() {
        try {
            if (null == AppContextUtils.getAppActivity() || null == mCameraDevice) {
                return;
            }
            Rect rectSensor = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int pixW = rectSensor.width();
            int pixH = rectSensor.height();
            int l = (int)((pixW-pixW/mScaleTime)*0.5);
            int t = (int)((pixH-pixH/mScaleTime)*0.5);
            int r = (int)((pixW+pixW/mScaleTime)*0.5);
            int b = (int)((pixH+pixH/mScaleTime)*0.5);

            int indicatorDuration = 0;

            switch (CamMode.mode){
                case NORMAL:
                    imageSaver.setSimpleProcessor();
                    List<CaptureRequest> buildersn = new ArrayList<>();
                    for(int i = 0; i < imageSaver.getProcessor().getFrameCount(); i++){
                        CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builder.set(CaptureRequest.SCALER_CROP_REGION,new Rect(l,t,r,b));
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        if(!CamSetting.isDenoiseOpened) {
                            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        }
                        builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                CameraMetadata.CONTROL_AF_TRIGGER_START);

                        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                        builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
                        builder.addTarget(imageSaver.getImageReader().getSurface());
                        buildersn.add(builder.build());
                    }
                    mCameraCaptureSession.captureBurst(buildersn, captureCallback, null);
                    break;

                case HDR:
                    imageSaver.setHdrProcessor();
                    List<CaptureRequest> buildersHdr = new ArrayList<>();
                    for (int i = 0; i < imageSaver.getProcessor().getFrameCount(); i++) {
                        CaptureRequest.Builder builderHdr = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                        if(i==0){
                            builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -15);
                        }else if(i==1){
                            builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
                        }else if(i==2) {
                            builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 15);
                        }
                        builderHdr.set(CaptureRequest.JPEG_ORIENTATION, rotation);
                        builderHdr.addTarget(imageSaver.getImageReader().getSurface());
                        buildersHdr.add(builderHdr.build());
                    }
                    mCameraCaptureSession.captureBurst(buildersHdr, null, null);
                    break;

                case PIX_FUSION:
                    imageSaver.setFusionProcessor();
                    List<CaptureRequest> buildersSuperRes = new ArrayList<>();
                    try {
                        for (int i = 0; i < imageSaver.getProcessor().getFrameCount(); i++) {
                            CaptureRequest.Builder builderSuperRes = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            builderSuperRes.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                            builderSuperRes.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                            builderSuperRes.set(CaptureRequest.CONTROL_AWB_LOCK, true);
                            builderSuperRes.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);

                            builderSuperRes.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100);
                            long expTime = (long) (mCameraParameter.exposureTime * (int) (mCameraParameter.iso / 50f)*mCameraParameter.isoBoost/100f);

                            if(CamSetting.isNightOpened) {
                                expTime = CamPara.timeIncrease(expTime, 2);
                            }

                            builderSuperRes.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
                            builderSuperRes.set(CaptureRequest.SENSOR_SENSITIVITY, 50);
                            indicatorDuration += expTime / 1000000f;

                            builderSuperRes.addTarget(imageSaver.getImageReader().getSurface());
                            builderSuperRes.addTarget(new Surface(texture));
                            builderSuperRes.set(CaptureRequest.JPEG_ORIENTATION, rotation);

                            buildersSuperRes.add(builderSuperRes.build());
                        }
                        mCameraCaptureSession.captureBurst(buildersSuperRes, captureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    break;

                case NIGHT:
                    imageSaver.setNightProcessor();
                    List<CaptureRequest> buildersSuperNight = new ArrayList<>();
                    for (int i = 0; i < imageSaver.getProcessor().getFrameCount(); i++) {
                        CaptureRequest.Builder builderSuperNight = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builderSuperNight.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                        builderSuperNight.addTarget(imageSaver.getImageReader().getSurface());
                        builderSuperNight.addTarget(new Surface(texture));

                        builderSuperNight.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                        builderSuperNight.set(CaptureRequest.CONTROL_AWB_LOCK, true);

                        if(!CamSetting.isDenoiseOpened) {
                            builderSuperNight.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        }
                        builderSuperNight.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST,  200);
                        builderSuperNight.set(CaptureRequest.SENSOR_EXPOSURE_TIME, CameraParameter.ONE_SECOND_DIV_8);
                        builderSuperNight.set(CaptureRequest.SENSOR_SENSITIVITY,
                                (int) ((mCameraParameter.iso / (mCameraParameter.ONE_SECOND_DIV_8 / mCameraParameter.exposureTime)) * mCameraParameter.isoBoost / 100f) / 2);
                        indicatorDuration += CameraParameter.ONE_SECOND_DIV_8 / 1000000;

                        builderSuperNight.set(CaptureRequest.JPEG_ORIENTATION, rotation);

                        buildersSuperNight.add(builderSuperNight.build());
                    }
                    mCameraCaptureSession.captureBurst(buildersSuperNight, captureCallback, null);
                    break;

                case SUPER_RES:
                    List<CaptureRequest> captureRequests = new ArrayList<>();
                    for (int i = 0; i < imageSaver.getProcessor().getFrameCount(); i++) {
                        CaptureRequest.Builder builder1 =  mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builder1.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));
                        builder1.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        builder1.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        builder1.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100);
                        builder1.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) (CameraParameter.ONE_SECOND / 4f));
                        builder1.set(CaptureRequest.SENSOR_SENSITIVITY,
                                (int) ((int) (mCameraParameter.iso / (CameraParameter.ONE_SECOND / 4.0f / mCameraParameter.exposureTime)) * mCameraParameter.isoBoost / 100.0f));
                        builder1.addTarget(new Surface(texture));
                        builder1.set(CaptureRequest.JPEG_ORIENTATION, rotation);
                        builder1.addTarget(imageSaver.getImageReader().getSurface());
                        captureRequests.add(builder1.build());
                    }
                    mCameraCaptureSession.captureBurst(captureRequests, null, null);

                    break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(AppContextUtils.getAppActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            loadCameraLists();
            loadCameraSize();
            mCameraManager.openCamera(mCameraIdList[curCameraIdx], cameraDeviceStateListener, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        imageSaver.getImageReader().close();
    }
}
