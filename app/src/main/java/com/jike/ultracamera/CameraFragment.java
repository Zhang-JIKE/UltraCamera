package com.jike.ultracamera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.camera2.CameraController;
import com.jike.ultracamera.camera2.UCameraProxy;
import com.jike.ultracamera.camera2.cameraui.CameraUI;
import com.jike.ultracamera.camera2.module.manager.ModuleManager;
import com.jike.ultracamera.interfaces.CameraTouchListener;
import com.jike.ultracamera.interfaces.OnHandFocusListener;
import com.jike.ultracamera.interfaces.SurfaceTextureListenerAdapter;
import com.jike.ultracamera.view.LensView;
import com.jike.ultracamera.view.TabTextView;

public class CameraFragment extends Fragment{

    private boolean isOpend = false;

    private CameraUI cameraUI;
    private ModuleManager moduleManager;

    private SensorManager sensorManager;
    private CameraTouchListener touchListener;

    private final SurfaceTextureListenerAdapter textureListenerAdapter = new SurfaceTextureListenerAdapter() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            CameraController.getInstance().setTexture(cameraUI.cameraView.getSurfaceTexture());
            openModule();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            cameraUI.cameraView.configureTransform(width, height);
        }
    };

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        moduleManager = new ModuleManager();
        initView(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        initSensorListener();
        if (cameraUI.cameraView.isAvailable()) {
            openModule();
        } else {
            cameraUI.cameraView.setSurfaceTextureListener(textureListenerAdapter);
        }
    }

    private void initView(View view){

        cameraUI = new CameraUI(view);

        cameraUI.tabTextView.setData(moduleManager);

        //快门
        cameraUI.shutterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraUI.shutterView.isEnabled) {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    CameraController.getInstance().takePicture(moduleManager.getCurModule());
                }
            }
        });

        //Tab选项
        cameraUI.tabTextView.setTabListener(new TabTextView.TabListener() {
            @Override
            public void onTabSelected(int idx) {
                moduleManager.setCurModule(idx);
                cameraUI.indicatorLayout.show();

                if (cameraUI.cameraView.isAvailable()) {
                    openModule();
                } else {
                    cameraUI.cameraView.setSurfaceTextureListener(textureListenerAdapter);
                }
            }
        });

        //镜头选择
        cameraUI.lensView.setOnLensSelectedListener(new LensView.OnLensSelectedListener() {
            @Override
            public void onLensSelectedChanged(String id) {
                UCameraProxy.getCameraObject().setCurPhysicId(id);

                cameraUI.updateView();
                CameraController.getInstance().startPreview();
            }
        });


        cameraUI.ivFacingSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UCameraProxy.setIsOpenedFrontCamera(!UCameraProxy.isOpenedFrontCamera);
                if (cameraUI.cameraView.isAvailable()) {
                    openModule();
                } else {
                    cameraUI.cameraView.setSurfaceTextureListener(textureListenerAdapter);
                }
            }
        });

        //CaptureListenerHelper.bindView(shutterView,tips);

        cameraUI.cameraView.cameraControllerView.setOnHandFocusListener(new OnHandFocusListener() {
            @Override
            public void onHandFocus(Point point) {
                CameraController.getInstance().setFocus(point);
            }

            @Override
            public void onFocusFallBack() {
            }
        });

        initTab();
    }

    private void initSensorListener(){
        sensorManager = (SensorManager)AppContextUtils.getAppActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                if(x >= -1 && x < 1){
                    if(y > 0){
                        //0
                        CameraController.getInstance().setRotation(90);
                    }
                    if(y < 0){
                        //180
                        CameraController.getInstance().setRotation(90 + 180);
                    }
                }else if(x > 6){
                    //-90
                    CameraController.getInstance().setRotation(90 - 90);
                }else if(x < -6){
                    //90
                    CameraController.getInstance().setRotation(90 + 90);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, sensor, sensorManager.SENSOR_DELAY_UI);
    }

    private void initTab(){
        touchListener = new CameraTouchListener();
        touchListener.setFingerListener(new CameraTouchListener.FingerListener() {
            @Override
            public void onScaleChanged(float scaleV, String scaleS) {
                CameraController.getInstance().setScaleTime(scaleV);
            }

            @Override
            public void onFocusSelected(Point point) {
                cameraUI.cameraView.cameraControllerView.setControlledFocus(point);
            }

            @Override
            public void onSlideToRight() {
                cameraUI.tabTextView.animToPrior();
            }

            @Override
            public void onSlideToLeft() {
                cameraUI.tabTextView.animToNext();
            }
        });
        cameraUI.cameraView.setOnTouchListener(touchListener);

    }

    private void openModule(){
        cameraUI.indicatorLayout.setData(moduleManager.curModule.getModuleName() ,moduleManager.curModule.getIconResId());

        touchListener.oldScale = 1;
        cameraUI.cameraView.mScaleTime = 1;

        CameraController.getInstance().stopBackgroundThread();
        CameraController.getInstance().startBackgroundThread();

        moduleManager.getCurModule().stopModule();
        moduleManager.getCurModule().startModule();

        cameraUI.updateView();
    }
}
