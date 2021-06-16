package com.jike.ultracamera.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.camera2.module.BaseModule;
import com.jike.ultracamera.cameradata.CameraPara;
import com.jike.ultracamera.helper.CaptureListenerHelper;


import java.util.Arrays;

public final class CameraController {

    private static CameraController CameraController;

    private CameraListener cameraListener;

    public void setCameraListener(CameraListener cameraListener) {
        this.cameraListener = cameraListener;
    }

    public Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == MSG_ON_INIT){
                if(cameraListener != null){
                    cameraListener.onInit();
                }
            }else if(msg.what == MSG_ON_READ_FINISED){
                if(cameraListener != null){
                    cameraListener.onReadFinished();
                }
            }else if(msg.what == MSG_ON_START_TO_OPEN){
                if(cameraListener != null){
                    cameraListener.onStartToOpen();
                }
            }else if(msg.what == MSG_ON_START_TO_PREVIEW){
                if(cameraListener != null){
                    cameraListener.onStartPreview();
                }
            }else if(msg.what == MSG_ON_OPEN_FINISHED){
                if(cameraListener != null){
                    cameraListener.onOpenFinished();
                }
            }else if(msg.what == MSG_ON_CLOSED){
                if(cameraListener != null){
                    cameraListener.onClosed();
                }
            }
        }
    };

    private boolean isFirstOpen = true;

    private static final int MSG_ON_INIT = 0x000001;
    private static final int MSG_ON_READ_FINISED = 0x000002;
    private static final int MSG_ON_START_TO_OPEN = 0x000003;
    private static final int MSG_ON_START_TO_PREVIEW = 0x000004;
    private static final int MSG_ON_OPEN_FINISHED = 0x000005;
    private static final int MSG_ON_CLOSED = 0x000006;

    public interface CameraListener{
        void onInit();
        void onReadFinished();
        void onStartToOpen();
        void onStartPreview();
        void onOpenFinished();
        void onClosed();
    }

    private CameraController() {}

    public static CameraController getInstance() {
        if (CameraController == null) {
            CameraController = new CameraController();
        }
        return CameraController;
    }

    private float mScaleTime = 1;
    private int rotation = 0;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mCameraCaptureSession;

    private CaptureRequest.Builder previewBuilder;
    private CaptureRequest previewRequest;
    private CaptureResult captureResult;

    private SurfaceTexture texture;
    private Surface surface;

    private ImageSaver imageSaver;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(AppContextUtils.getAppActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isFirstOpen = true;

        try {
            mCameraManager = (CameraManager) AppContextUtils.getAppActivity().
                    getSystemService(Context.CAMERA_SERVICE);

            UCameraProxy.initCamera(mCameraManager);

            Message msgOnInit = new Message();
            msgOnInit.what = MSG_ON_INIT;
            handler.sendMessage(msgOnInit);

            UCameraProxy.readCamera();

            Message msgReadFinished = Message.obtain();
            msgReadFinished.what = MSG_ON_READ_FINISED;
            handler.sendMessage(msgReadFinished);

            mCameraCharacteristics = UCameraProxy.characteristics;

            UCameraProxy.manager.openCamera(
                    UCameraProxy.UCameras[UCameraProxy.curCameraObjectIndex].getLogicId(),
                    cameraDeviceStateListener,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        try {
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != imageSaver) {
                imageSaver.getImageReader().close();
                imageSaver = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
    }

    public void startPreview() {
        try {
            UCameraProxy.readCamera();

            Message msgOnStartToPreview = Message.obtain();
            msgOnStartToPreview.what = MSG_ON_START_TO_PREVIEW;
            handler.sendMessage(msgOnStartToPreview);

            int w = UCameraProxy.getPicSize().getWidth();
            int h = UCameraProxy.getPicSize().getHeight();

            /*if(h/2 >= 1440){
                w/=2;
                h/=2;
            }*/

            texture.setDefaultBufferSize(w, h);
            surface = new Surface(texture);

            imageSaver = new ImageSaver(UCameraProxy.getPicSize().getWidth(),
                    UCameraProxy.getPicSize().getHeight(),
                    UCameraProxy.imageFormat);

            //单摄解决方案
            if(!UCameraProxy.UCameras[UCameraProxy.curCameraObjectIndex].isHasPhysicalCamera()) {
                mCameraDevice.createCaptureSession(Arrays.asList(surface,
                        imageSaver.getImageReader().getSurface()),
                        captureSessionStateListener,
                        mBackgroundHandler
                );

            }else{
                OutputConfiguration configuration1 = new OutputConfiguration(surface);
                configuration1.setPhysicalCameraId(
                        UCameraProxy.getCameraObject().getCurPhysicId()
                );

                OutputConfiguration configuration2 = new OutputConfiguration(imageSaver.getImageReader().getSurface());
                configuration2.setPhysicalCameraId(
                        UCameraProxy.getCameraObject().getCurPhysicId()
                );

                mCameraDevice.createCaptureSessionByOutputConfigurations(Arrays.asList(configuration1,configuration2),
                        captureSessionStateListener,
                        mBackgroundHandler
                );
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private final CameraDevice.StateCallback cameraDeviceStateListener = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;

            Message msgOnStartToOpen = Message.obtain();
            msgOnStartToOpen.what = MSG_ON_START_TO_OPEN;
            handler.sendMessage(msgOnStartToOpen);

            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;

            Message msgOnClosed = Message.obtain();
            msgOnClosed.what = MSG_ON_CLOSED;
            handler.sendMessage(msgOnClosed);

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            //mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;

            Message msgOnClosed = Message.obtain();
            msgOnClosed.what = MSG_ON_CLOSED;
            handler.sendMessage(msgOnClosed);

        }
    };

    private CameraCaptureSession.StateCallback captureSessionStateListener = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCameraCaptureSession = cameraCaptureSession;

            try {
                //cameraCaptureSession.abortCaptures();

                previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //previewBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                previewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                previewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

                previewBuilder.addTarget(surface);

                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), previewCallback, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(
                @NonNull CameraCaptureSession cameraCaptureSession) {
        }
    };

    private CameraCaptureSession.CaptureCallback previewCallback = new CameraCaptureSession.CaptureCallback(){
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            try {
                CameraPara.exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                CameraPara.iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
                CameraPara.isoBoost = result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST);

                if(isFirstOpen) {
                    Message msgOnOpenFinished = Message.obtain();
                    msgOnOpenFinished.what = MSG_ON_OPEN_FINISHED;
                    handler.sendMessage(msgOnOpenFinished);

                    isFirstOpen = false;
                }

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


    public void setFocus(Point point){
        /*
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
        }*/
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

        //previewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO,time);
        previewRequest = previewBuilder.build();

        try {
            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Background");
        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if(mBackgroundHandler == null || mBackgroundThread == null) return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void takePicture(BaseModule cameraModule) {
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

            cameraModule.onPictureTaken(
                    mCameraDevice,
                    mCameraCaptureSession,
                    mCameraCharacteristics,
                    captureCallback,
                    surface,
                    new Rect(l,t,r,b), rotation ,imageSaver);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setTexture(SurfaceTexture texture) {
        this.texture = texture;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public static CameraDevice getCameraDevice() {
        return CameraController.getInstance().mCameraDevice;
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return mCameraCharacteristics;
    }

    public CaptureResult getCaptureResult() {
        return captureResult;
    }

}
