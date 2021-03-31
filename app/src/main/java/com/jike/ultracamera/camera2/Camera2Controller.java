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
import android.hardware.camera2.params.RggbChannelVector;
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
import com.jike.ultracamera.cameradata.CamRates;
import com.jike.ultracamera.cameradata.CamSetting;
import com.jike.ultracamera.cameradata.CameraParameter;
import com.jike.ultracamera.cameradata.CameraResolution;
import com.jike.ultracamera.imagereader.ImageReaderHelper;
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

    //摄像头id
    private int mCamIdx;
    private String mCameraId;
    private String[] mCameraIdList;

    private int viewHeight;
    private int viewWidth;
    private float mScaleTime = 1;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mCameraCaptureSession;
    private Camera2Listener camera2Listener;

    public CameraParameter mCameraParameter = CameraParameter.getInstance();
    public CameraResolution mCameraResolution = CameraResolution.getInstance();

    private CaptureRequest.Builder previewBuilder;
    private CaptureRequest previewRequest;
    private CaptureResult captureResult;
    
    private SurfaceTexture texture;
    private Surface surface;

    private ImageReaderHelper imageReaderHelper;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    protected static RggbChannelVector rggbVector					= null;
    protected static int[] 				colorTransformMatrix 		= new int[]{258, 128, -119, 128, -10, 128, -40, 128, 209, 128, -41, 128, -1, 128, -74, 128, 203, 128};
    protected static float				multiplierR					= 1.6f;
    protected static float				multiplierG					= 1.0f;
    protected static float				multiplierB					= 2.4f;

    public void initCameraController(int viewWidth, int viewHeight, SurfaceTexture texture, Camera2Listener camera2Listener) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.texture = texture;
        this.camera2Listener = camera2Listener;
    }

    public static CameraDevice getCameraDevice() {
        return Camera2Controller.getInstance().mCameraDevice;
    }

    public static CameraCharacteristics getCameraCharacteristics() {
        return Camera2Controller.getInstance().mCameraCharacteristics;
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
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                previewBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                previewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                previewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

               /* previewBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);

                float[] t22 = new float[]{
                        0.0000f, 0.0000f, 0.0667f, 0.2920f, 0.1333f, 0.4002f, 0.2000f, 0.4812f,
                        0.2667f, 0.5484f, 0.3333f, 0.6069f, 0.4000f, 0.6594f, 0.4667f, 0.7072f,
                        0.5333f, 0.7515f, 0.6000f, 0.7928f, 0.6667f, 0.8317f, 0.7333f, 0.8685f,
                        0.8000f, 0.9035f, 0.8667f, 0.9370f, 0.9333f, 0.9691f, 1.0000f, 1.0000f };

                TonemapCurve t22curve = new TonemapCurve(t22, t22, t22);
                previewBuilder.set(CaptureRequest.TONEMAP_CURVE, t22curve);*/

               /* float R = 0;
                float G_even = 0;
                float G_odd  = 0;
                float B      = 0;
                float tmpKelvin = 50000/100;
                if(tmpKelvin <= 66)
                    R = 255;
                else
                {
                    double tmpCalc = tmpKelvin - 60;
                    tmpCalc = 329.698727446 * Math.pow(tmpCalc, -0.1332047592);
                    R = (float)tmpCalc;
                    if(R < 0) R = 0.0f;
                    if(R > 255) R = 255;
                }

                if(tmpKelvin <= 66) {
                    double tmpCalc = tmpKelvin;
                    tmpCalc = 99.4708025861 * Math.log(tmpCalc) - 161.1195681661;
                    G_even = (float)tmpCalc;
                    if(G_even < 0)
                        G_even = 0.0f;
                    if(G_even > 255)
                        G_even = 255;
                    G_odd = G_even;
                }
                else {
                    double tmpCalc = tmpKelvin - 60;
                    tmpCalc = 288.1221695283 * Math.pow(tmpCalc, -0.0755148492);
                    G_even = (float)tmpCalc;
                    if(G_even < 0)
                        G_even = 0.0f;
                    if(G_even > 255)
                        G_even = 255;
                    G_odd = G_even;
                }
                if(tmpKelvin <= 19) {
                    B = 0.0f;
                }
                else {
                    double tmpCalc = tmpKelvin - 10;
                    tmpCalc = 138.5177312231 * Math.log(tmpCalc) - 305.0447927307;
                    B = (float)tmpCalc;
                    if(B < 0) B = 0;
                }
                R = (R/255) * multiplierR;
                G_even = (G_even/255) * multiplierG;
                G_odd = G_even;
                B = (B/255) * multiplierB;

                rggbVector = new RggbChannelVector(R, G_even, G_odd, B);
                previewBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbVector);
                previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_OFF);

                previewBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, new ColorSpaceTransform(colorTransformMatrix))*/;

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
            texture.setDefaultBufferSize(mCameraResolution.getPicSize().getWidth(), mCameraResolution.getPicSize().getHeight());
            surface = new Surface(texture);
            
            previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);

            imageReaderHelper = new ImageReaderHelper(mCameraResolution.getPicSize().getWidth(),
                    mCameraResolution.getPicSize().getHeight(),
                    ImageFormat.YUV_420_888);

            setCameraParams();

            mCameraDevice.createCaptureSession(Arrays.asList(surface,
                    imageReaderHelper.getImageReader().getSurface()),
                    captureSessionStateListener,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initCamera(){
        mCameraManager = (CameraManager) AppContextUtils.getAppActivity().getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList;
        try {
            cameraIdList = mCameraManager.getCameraIdList();
            mCameraId = cameraIdList[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    
    private void initCameraParams(){
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mCameraResolution.setPicSizes(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
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

        previewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
        previewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

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
            if(camera2Listener !=null) {
                camera2Listener.onFaceDetected(rects);
                
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
                    imageReaderHelper.setSimpleProcessor();
                    List<CaptureRequest> buildersn = new ArrayList<>();
                    for(int i = 0; i < imageReaderHelper.getProcessor().getFrameCount();i++){
                        CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builder.set(CaptureRequest.SCALER_CROP_REGION,new Rect(l,t,r,b));
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        if(!CamSetting.isDenoiseOpened) {
                            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        }

                        builder.addTarget(imageReaderHelper.getImageReader().getSurface());
                        buildersn.add(builder.build());
                    }
                    mCameraCaptureSession.captureBurst(buildersn, null, null);
                    break;

                case HDR:
                    imageReaderHelper.setHdrProcessor();
                    List<CaptureRequest> buildersHdr = new ArrayList<>();
                    for (int i = 0; i < imageReaderHelper.getProcessor().getFrameCount(); i++) {
                            CaptureRequest.Builder builderHdr = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
                            builderHdr.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, CamRates.rawFpsRanges[0]);
                            builderHdr.set(CaptureRequest.CONTROL_AWB_LOCK, true);

                            builderHdr.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                            if(i==0){
                                //CvHdrQueueProcessor.times[i] = CameraParameter.exposureTime * 1.0f / CameraParameter.ONE_SECOND;
                            }else if(i==1){
                                //CvHdrQueueProcessor.times[i] = CameraParameter.exposureTime / 2.0f / CameraParameter.ONE_SECOND;
                                // /2
                                builderHdr.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                                builderHdr.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, mCameraParameter.isoBoost);
                                builderHdr.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mCameraParameter.exposureTime/4);
                                builderHdr.set(CaptureRequest.SENSOR_SENSITIVITY, mCameraParameter.iso);
                            }else if(i==2) {
                                // *2
                                //CvHdrQueueProcessor.times[i] = CameraParameter.exposureTime * 2.0f / CameraParameter.ONE_SECOND;
                                builderHdr.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, mCameraParameter.isoBoost);
                                builderHdr.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mCameraParameter.exposureTime / 2);
                                builderHdr.set(CaptureRequest.SENSOR_SENSITIVITY, mCameraParameter.iso);
                            }

                            builderHdr.addTarget(imageReaderHelper.getImageReader().getSurface());
                            buildersHdr.add(builderHdr.build());
                        }
                    mCameraCaptureSession.captureBurst(buildersHdr, null, null);
                    break;

                case PIX_FUSION:
                    imageReaderHelper.setFusionProcessor();
                    List<CaptureRequest> buildersSuperRes = new ArrayList<>();
                    try {
                        for (int i = 0; i < imageReaderHelper.getProcessor().getFrameCount(); i++) {
                            CaptureRequest.Builder builderSuperRes = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                            builderSuperRes.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                            builderSuperRes.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                            builderSuperRes.set(CaptureRequest.CONTROL_AWB_LOCK, true);
                            builderSuperRes.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);

                            builderSuperRes.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100);
                            long expTime = (long) (mCameraParameter.exposureTime * (int) (mCameraParameter.iso / 50f)*mCameraParameter.isoBoost/100f) + (CamSetting.isNightOpened ? CameraParameter.ONE_SECOND * 2 : 0);
                            builderSuperRes.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
                            builderSuperRes.set(CaptureRequest.SENSOR_SENSITIVITY, 50);
                            indicatorDuration += expTime / 1000000;

                            builderSuperRes.addTarget(imageReaderHelper.getImageReader().getSurface());
                            builderSuperRes.addTarget(new Surface(texture));

                            buildersSuperRes.add(builderSuperRes.build());
                        }
                        mCameraCaptureSession.captureBurst(buildersSuperRes, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    break;

                case NIGHT:
                    imageReaderHelper.setNightProcessor();
                    List<CaptureRequest> buildersSuperNight = new ArrayList<>();
                    for (int i = 0; i < imageReaderHelper.getProcessor().getFrameCount(); i++) {
                        CaptureRequest.Builder builderSuperNight = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builderSuperNight.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                        builderSuperNight.addTarget(imageReaderHelper.getImageReader().getSurface());
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

                        buildersSuperNight.add(builderSuperNight.build());
                    }
                    mCameraCaptureSession.captureBurst(buildersSuperNight, null, null);
                    break;

                case SUPER_RES:
                    List<CaptureRequest> captureRequests = new ArrayList<>();
                    for (int i = 0; i < imageReaderHelper.getProcessor().getFrameCount(); i++) {
                        CaptureRequest.Builder builder1 =  mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builder1.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));
                        builder1.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        builder1.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                        builder1.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100);
                        builder1.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) (CameraParameter.ONE_SECOND / 4f));
                        builder1.set(CaptureRequest.SENSOR_SENSITIVITY,
                                (int) ((int) (mCameraParameter.iso / (CameraParameter.ONE_SECOND / 4.0f / mCameraParameter.exposureTime)) * mCameraParameter.isoBoost / 100.0f));
                        builder1.addTarget(new Surface(texture));
                        builder1.addTarget(imageReaderHelper.getImageReader().getSurface());
                        captureRequests.add(builder1.build());
                    }
                    mCameraCaptureSession.captureBurst(captureRequests, null, null);

                    break;
            }

            if(imageReaderHelper.getProcessor().isNeedShutterIndicator()){
                CaptureListenerHelper.getListener().onCaptureStarted(indicatorDuration, true);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

   /* private void brightNessDetect(){
        Bitmap bitmap = getBitmap(3, 3);
        int[] pixels = new int[bitmap.getWidth()*bitmap.getHeight()];
        bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        CameraParameter.brightNess = PixFormula.getBrightness(bitmap);
        if(paramListener != null) {
            paramListener.onParamReceived(mCameraParameter.exposureTime,
                    mCameraParameter.iso,
                    mCameraParameter.isoBoost,
                    mCameraParameter.brightNess);
        }
    }*/

    public void openCamera(int camIdx) {
        mCamIdx = camIdx;

        if (ContextCompat.checkSelfPermission(AppContextUtils.getAppActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        initCamera();
        //mCameraId = mCameraIdList[mCamIdx];

        initCameraParams();
        
        if(camera2Listener != null){
            camera2Listener.onCameraOpened(mCameraResolution.getPicSize().getWidth(),
                    mCameraResolution.getPicSize().getHeight(), camIdx);
        }

        /*configureTransform(width,height);
        setAspectRatio(mCameraResolution.getPicSize().getHeight(), mCameraResolution.getPicSize().getWidth());*/

        CameraManager manager = (CameraManager) AppContextUtils.getAppActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            manager.openCamera(mCameraId, cameraDeviceStateListener, mBackgroundHandler);
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
        imageReaderHelper.getImageReader().close();

        if(camera2Listener != null){
            camera2Listener.onCameraClosed(mCamIdx);
        }
    }
}
