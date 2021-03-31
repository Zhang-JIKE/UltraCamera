package com.jike.ultracamera;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.daily.flexui.util.AppContextUtils;
import com.daily.flexui.view.CircleImageView;
import com.jike.ultracamera.camera2.Camera2Listener;
import com.jike.ultracamera.cameradata.CamMode;
import com.jike.ultracamera.cameradata.CamSetting;
import com.jike.ultracamera.interfaces.CameraTouchListener;
import com.jike.ultracamera.helper.CaptureListenerHelper;
import com.jike.ultracamera.interfaces.OnHandFocusListener;
import com.jike.ultracamera.interfaces.OnImageDetectedListener;
import com.jike.ultracamera.interfaces.SurfaceTextureListenerAdapter;
import com.jike.ultracamera.view.Camera2View;
import com.jike.ultracamera.view.ShutterView;
import com.jike.ultracamera.view.TabTextView;


import java.text.DecimalFormat;

public class CameraFragment extends Fragment implements View.OnClickListener{

    final int[] pos = {6};
    private int camIdx = 0;
    private boolean isFaceingFront = false;
    public Camera2View cameraView;

    private ImageView ivFlash;
    private ImageView ivHdr;
    private ImageView ivFilter;
    private ImageView ivSuperRes;
    private ImageView ivSettings;
    private ImageView ivFacingSwitch;
    private CircleImageView gallery;
    private TabTextView tabTextView;

    private ShutterView shutterView;

    private FrameLayout textureLayout;
    private CircleImageView ivPicture;
    private TextView tvInfo;
    private TextView tvScaler,tips;



    private final SurfaceTextureListenerAdapter textureListenerAdapter = new SurfaceTextureListenerAdapter() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera();
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

        if (cameraView.isAvailable()) {
            openCamera();
        } else {
            cameraView.setSurfaceTextureListener(textureListenerAdapter);
        }
        CaptureListenerHelper.bindView(shutterView,tips);
        //cameraView.c2helper.setScaleTime(cameraView.mScaleTime);
        DecimalFormat mFormat = new DecimalFormat(".0");
        String formatNum = mFormat.format(cameraView.mScaleTime);
        if(formatNum.contains(".0")){
            formatNum=formatNum.substring(0,formatNum.indexOf("."));
        }
        tvScaler.setText(formatNum+"x");
    }

    @Override
    public void onPause() {
        super.onPause();
        CaptureListenerHelper.unBindView();
        cameraView.getCamera2Controller().closeCamera();
        cameraView.getCamera2Controller().stopBackgroundThread();
    }

    private void initView(View view){
        ivFacingSwitch = view.findViewById(R.id.iv_facing_switch);
        tips = view.findViewById(R.id.tips);
        tvInfo = view.findViewById(R.id.information);
        tvScaler = view.findViewById(R.id.scaler);
        gallery = view.findViewById(R.id.iv_picture);

        ivFacingSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFaceingFront = !isFaceingFront;

                if(isFaceingFront) {
                    camIdx = 1;
                }
                else {
                    camIdx = 0;
                }
                //cameraView.closeCamera();
                if (cameraView.isAvailable()) {
                    openCamera();
                } else {
                    cameraView.setSurfaceTextureListener(textureListenerAdapter);
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AppContextUtils.getAppContext(), ViewActivity.class);
                startActivity(intent);
            }
        });

        cameraView = view.findViewById(R.id.texture);
        cameraView.setOnImageDetectedListener(new OnImageDetectedListener() {
            @Override
            public void onSceneDetected(String tag) {
                /*Message message = Message.obtain();
                message.what = 1;
                message.obj = tag;*/
                //handler.sendMessage(message);
            }

            @Override
            public void onFaceDetected() {

            }
        });

        textureLayout = view.findViewById(R.id.texture_container);
        ivPicture = view.findViewById(R.id.iv_picture);

        ivFlash = view.findViewById(R.id.iv_flash);
        ivHdr = view.findViewById(R.id.iv_hdr);
        ivFilter = view.findViewById(R.id.iv_filter);
        ivSuperRes = view.findViewById(R.id.iv_super_res);
        ivSettings = view.findViewById(R.id.iv_settings);
        shutterView = view.findViewById(R.id.shutter);

        ivSettings.setOnClickListener(this);
        ivHdr.setOnClickListener(this);
        ivSuperRes.setOnClickListener(this);
        shutterView.setOnClickListener(this);

        CaptureListenerHelper.bindView(shutterView,tips);

        initTab(view);
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


        tabTextView = view.findViewById(R.id.tablayout);
        CameraTouchListener touchListener = new CameraTouchListener();
        touchListener.setFingerListener(new CameraTouchListener.FingerListener() {
            @Override
            public void onScaleChanged(float scaleV, String scaleS) {
                cameraView.getCamera2Controller().setScaleTime(scaleV);
                tvScaler.setText(scaleS);
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
                cameraView.getCamera2Controller().takePicture();
            }
        }
    }


    private void openCamera(){
        cameraView.getCamera2Controller().openCamera(camIdx);
        cameraView.setControllerView(textureLayout);
        cameraView.setUpTexturePadding(textureLayout);
        cameraView.getCamera2Controller().initCameraController(
                cameraView.getCamera2Controller().mCameraResolution.getPicSize().getWidth(),
                cameraView.getCamera2Controller().mCameraResolution.getPicSize().getHeight(),
                cameraView.getSurfaceTexture(),
                new Camera2Listener() {
                    @Override
                    public void onCameraOpened(int w, int h, int camIdx) {

                    }

                    @Override
                    public void onCameraClosed(int camIdx) {

                    }

                    @Override
                    public void onFaceDetected(Rect[] rects) {

                    }
                });

        cameraView.cameraControllerView.setOnHandFocusListener(new OnHandFocusListener() {
            @Override
            public void onHandFocus(Point point) {
                cameraView.getCamera2Controller().setFocus(point);

            }

            @Override
            public void onFocusFallBack() {
            }
        });
        cameraView.getCamera2Controller().startBackgroundThread();
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
