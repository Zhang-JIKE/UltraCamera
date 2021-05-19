package com.jike.ultracamera;

import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.daily.flexui.util.AppContextUtils;
import com.daily.flexui.view.CircleImageView;
import com.jike.ultracamera.camera2.UCameraController;
import com.jike.ultracamera.camera2.UCameraManager;
import com.jike.ultracamera.cameradata.CamMode;
import com.jike.ultracamera.cameradata.CamSetting;
import com.jike.ultracamera.interfaces.CameraTouchListener;
import com.jike.ultracamera.helper.CaptureListenerHelper;
import com.jike.ultracamera.interfaces.OnHandFocusListener;
import com.jike.ultracamera.interfaces.SurfaceTextureListenerAdapter;
import com.jike.ultracamera.view.Camera2View;
import com.jike.ultracamera.view.CameraControllerView;
import com.jike.ultracamera.view.LensView;
import com.jike.ultracamera.view.ShutterView;
import com.jike.ultracamera.view.TabTextView;

public class CameraFragment extends Fragment implements View.OnClickListener{

    private boolean isOpend = false;

    public Camera2View cameraView;

    private ImageView ivFlash;
    private ImageView ivHdr;
    private ImageView ivFilter;
    private ImageView ivSuperRes;
    private ImageView ivSettings;
    private ImageView ivFacingSwitch;
    private CircleImageView gallery;
    private TabTextView tabTextView;
    private LensView lensView;

    private ShutterView shutterView;

    private CameraControllerView controllerView;
    private CircleImageView ivPicture;
    private TextView tips;

    private SensorManager sensorManager;
    private CameraTouchListener touchListener;

    private final SurfaceTextureListenerAdapter textureListenerAdapter = new SurfaceTextureListenerAdapter() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            cameraView.configureTransform(width, height);
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
        initView(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        //UCameraController.getInstance().startBackgroundThread();
        initSensorListener();

        if (cameraView.isAvailable()) {
            openCamera();
        } else {
            cameraView.setSurfaceTextureListener(textureListenerAdapter);
        }
        CaptureListenerHelper.bindView(shutterView,tips);
    }

    @Override
    public void onPause() {
        super.onPause();
        //Camera2Controller.getInstance().stopBackgroundThread();
        /*CaptureListenerHelper.unBindView();
        cameraView.getController().closeCamera();*/
        //cameraView.getController().stopBackgroundThread();
    }

    private void initView(View view){
        lensView = view.findViewById(R.id.lens_view);
        ivFacingSwitch = view.findViewById(R.id.iv_facing_switch);
        tips = view.findViewById(R.id.tips);
        gallery = view.findViewById(R.id.iv_picture);
        cameraView = view.findViewById(R.id.texture);
        controllerView = view.findViewById(R.id.controllerView);
        ivPicture = view.findViewById(R.id.iv_picture);
        ivFlash = view.findViewById(R.id.iv_flash);
        ivHdr = view.findViewById(R.id.iv_hdr);
        ivFilter = view.findViewById(R.id.iv_filter);
        ivSuperRes = view.findViewById(R.id.iv_super_res);
        ivSettings = view.findViewById(R.id.iv_settings);
        shutterView = view.findViewById(R.id.shutter);
        tabTextView = view.findViewById(R.id.tablayout);

        lensView.setOnLensSelectedListener(new LensView.OnLensSelectedListener() {
            @Override
            public void onLensSelectedChanged(String id) {
                UCameraManager.getCameraObject().setCurPhysicId(id);
                openCamera();
            }
        });


        ivFacingSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UCameraManager.setIsOpenedFrontCamera(!UCameraManager.isOpenedFrontCamera);
                openCamera();
            }
        });


        ivSettings.setOnClickListener(this);
        ivHdr.setOnClickListener(this);
        ivSuperRes.setOnClickListener(this);
        shutterView.setOnClickListener(this);

        CaptureListenerHelper.bindView(shutterView,tips);
        cameraView.setCameraControllerView(controllerView);

        initTab(view);
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
                        UCameraController.getInstance().setRotation(90);
                    }
                    if(y < 0){
                        //180
                        UCameraController.getInstance().setRotation(90 + 180);
                    }
                }else if(x > 6){
                    //-90
                    UCameraController.getInstance().setRotation(90 - 90);
                }else if(x < -6){
                    //90
                    UCameraController.getInstance().setRotation(90 + 90);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, sensor, sensorManager.SENSOR_DELAY_UI);
    }

    private void initTab(View view){
        int idx = TabTextView.nowIndex;
        if(idx == 0){
            setNightOn();
        }else if(idx == 1){
            setNormal();
        }else if(idx == 2){
            setFusionOn();
        }else if(idx == 3){
            setSuperRes();
        }


        touchListener = new CameraTouchListener();
        touchListener.setFingerListener(new CameraTouchListener.FingerListener() {
            @Override
            public void onScaleChanged(float scaleV, String scaleS) {
                cameraView.getController().setScaleTime(scaleV);
            }

            @Override
            public void onFocusSelected(Point point) {
                cameraView.cameraControllerView.setControlledFocus(point);
            }

            @Override
            public void onSlideToRight() {
                tabTextView.animToPrior();
            }

            @Override
            public void onSlideToLeft() {
                tabTextView.animToNext();
            }
        });
        cameraView.setOnTouchListener(touchListener);

        tabTextView.setTabListener(new TabTextView.TabListener() {
            @Override
            public void onTabSelected(int idx) {
                if(idx == 0){
                    setNightOn();
                }else if(idx == 1){
                    setNormal();
                }else if(idx == 2){
                    setFusionOn();
                }else if(idx == 3){
                    setSuperRes();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == ivFlash.getId()){

        } else if(id == ivHdr.getId()){
            if ((CamMode.mode != CamMode.Mode.HDR)) {
                setHdrOn();
            } else {
                setNormal();
            }
        } else if(id == ivFilter.getId()){

        } else if(id == ivSuperRes.getId()){
            if(CamMode.mode != CamMode.Mode.PIX_FUSION){
                setFusionOn();
            }else {
                setNormal();
            }
        } else if(id == ivSettings.getId()){
            Intent intent = new Intent(AppContextUtils.getAppContext(), SettingsActivity.class);
            startActivity(intent);
        } else if(id == shutterView.getId()){
            if (shutterView.isEnabled) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                cameraView.getController().takePicture();
            }
        }
    }


    private void openCamera(){
        touchListener.oldScale = 1;
        cameraView.mScaleTime = 1;
        //tvScaler2.setText(CameraTouchListener.scale2String(touchListener.oldScale));

        cameraView.getController().closeCamera();
        cameraView.getController().openCamera();

        cameraView.setAspectRatio(
                UCameraManager.getPicSize().getHeight(),
                UCameraManager.getPicSize().getWidth()
        );

        //cameraView.configureTransform(UCamera.getPicSize().getHeight(),UCamera.getPicSize().getWidth());

        lensView.setLens(UCameraManager.getCameraObject().getTitleList(),
                UCameraManager.getCameraObject().getPhysicIds(),
                UCameraManager.getCameraObject().getAngleList());

        cameraView.getController().setTexture(cameraView.getSurfaceTexture());

        cameraView.cameraControllerView.setOnHandFocusListener(new OnHandFocusListener() {
            @Override
            public void onHandFocus(Point point) {
                cameraView.getController().setFocus(point);
            }

            @Override
            public void onFocusFallBack() {
            }
        });

    }

    private void setHdrOn(){
        setAllOff();
        ivHdr.setImageResource(R.drawable.ic_hdr_on);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.HDR;
    }

    private void setFusionOn(){
        setAllOff();
        ivSuperRes.setImageResource(R.drawable.ic_super_res_on);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.PIX_FUSION;
    }

    private void setNightOn(){
        setAllOff();
        ivFlash.setVisibility(View.INVISIBLE);
        ivHdr.setVisibility(View.INVISIBLE);
        ivFilter.setVisibility(View.INVISIBLE);
        ivSuperRes.setVisibility(View.INVISIBLE);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.NIGHT;
    }

    private void setAllOff(){
        ivHdr.setImageResource(R.drawable.ic_hdr_off);
        ivSuperRes.setImageResource(R.drawable.ic_super_res_off);
    }

    private void setNormal(){
        setAllOff();
        ivFlash.setVisibility(View.VISIBLE);
        ivHdr.setVisibility(View.VISIBLE);
        ivFilter.setVisibility(View.VISIBLE);
        ivSuperRes.setVisibility(View.VISIBLE);
        CamMode.mode = CamMode.Mode.NORMAL;
    }

    private void setSuperRes(){
        setAllOff();
        ivFlash.setVisibility(View.VISIBLE);
        ivHdr.setVisibility(View.VISIBLE);
        ivFilter.setVisibility(View.VISIBLE);
        ivSuperRes.setVisibility(View.VISIBLE);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.SUPER_RES;
    }
}
