package com.jike.ultracamera.camera2.cameraui;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.daily.flexui.util.AppContextUtils;
import com.daily.flexui.util.DisplayUtils;
import com.daily.flexui.view.CircleImageView;
import com.jike.ultracamera.R;
import com.jike.ultracamera.SettingsActivity;
import com.jike.ultracamera.camera2.CameraController;
import com.jike.ultracamera.camera2.UCameraProxy;
import com.jike.ultracamera.view.Camera2View;
import com.jike.ultracamera.view.CameraControllerView;
import com.jike.ultracamera.view.IndicatorLayout;
import com.jike.ultracamera.view.LensView;
import com.jike.ultracamera.view.ResolutionLayout;
import com.jike.ultracamera.view.ShutterView;
import com.jike.ultracamera.view.TabTextView;

public class CameraUI {

    private boolean isItemOpened = false;

    private ImageView ivFlash;
    private ImageView ivHdr;
    private ImageView ivFilter;
    private ImageView ivRatio;
    private ImageView ivSettings;

    public ResolutionLayout resolutionLayout;
    public IndicatorLayout indicatorLayout;

    public TextView tips;

    public Camera2View cameraView;
    public CameraControllerView controllerView;

    public LensView lensView;
    public TabTextView tabTextView;

    public CircleImageView gallery;
    public ShutterView shutterView;
    public ImageView ivFacingSwitch;

    public CameraUI(View root){
        ivFlash = root.findViewById(R.id.iv_flash);
        ivHdr = root.findViewById(R.id.iv_hdr);
        ivFilter = root.findViewById(R.id.iv_filter);
        ivRatio = root.findViewById(R.id.iv_ratio);
        ivSettings = root.findViewById(R.id.iv_settings);

        resolutionLayout = root.findViewById(R.id.resolutionLayout);

        tips = root.findViewById(R.id.tips);

        cameraView = root.findViewById(R.id.texture);
        controllerView = root.findViewById(R.id.controllerView);
        cameraView.setCameraControllerView(controllerView);

        lensView = root.findViewById(R.id.lens_view);
        tabTextView = root.findViewById(R.id.tablayout);

        gallery = root.findViewById(R.id.iv_picture);
        shutterView = root.findViewById(R.id.shutter);
        ivFacingSwitch = root.findViewById(R.id.iv_facing_switch);

        indicatorLayout = root.findViewById(R.id.indicator_layout);

        ivRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isItemOpened) {
                    hideResolutionLayout();
                }else {
                   showResolutionLayout();
                }
                isItemOpened = !isItemOpened;
            }
        });

        resolutionLayout.setOnItemSelectedListener(new ResolutionLayout.OnItemSelectedListener() {
            @Override
            public void OnItemSelected(int index) {
                CameraController.getInstance().startPreview();
            }
        });


        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AppContextUtils.getAppContext(), SettingsActivity.class);
                AppContextUtils.getAppContext().startActivity(intent);
            }
        });

        CameraController.getInstance().setCameraListener(new CameraController.CameraListener() {
            @Override
            public void onInit() {
                Log.e("CameraListener","onInit");
                //indicatorLayout.show();
            }

            @Override
            public void onReadFinished() {
                Log.e("CameraListener","onReadFinished");
                updateView();
                resolutionLayout.init();
            }

            @Override
            public void onStartToOpen() {
                Log.e("CameraListener","onStartToOpen");
            }

            @Override
            public void onStartPreview() {
                Log.e("CameraListener","onStartPreview");
                updateView();
            }

            @Override
            public void onOpenFinished() {
                Log.e("CameraListener","onOpenFinished");
                indicatorLayout.hide();
            }

            @Override
            public void onClosed() {
                Log.e("CameraListener","onClosed");
            }


        });
    }

    public void setUiClickable(boolean clickable){
        ivFlash.setClickable(clickable);
        ivHdr.setClickable(clickable);
        ivFilter.setClickable(clickable);
        ivRatio.setClickable(clickable);
        ivSettings.setClickable(clickable);

        resolutionLayout.setClickable(clickable);

        lensView.setClickable(clickable);
        tabTextView.setClickable(clickable);

        shutterView.setClickable(clickable);
        ivFacingSwitch.setClickable(clickable);
    }

    public void backToDefault(){
        ivFlash.setImageResource(R.drawable.ic_flash);
        ivHdr.setImageResource(R.drawable.ic_hdr_off);
        ivFilter.setImageResource(R.drawable.ic_filter);
        ivRatio.setImageResource(R.drawable.ic_aspect_ratio);
        ivSettings.setImageResource(R.drawable.ic_settings);

        isItemOpened = false;
        hideResolutionLayout();
    }

    private void hideResolutionLayout(){
        resolutionLayout
                .animate()
                .translationY(-DisplayUtils.dp2px(64))
                .setDuration(700)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        resolutionLayout.animHideChildView(100);
    }

    private void showResolutionLayout(){
        resolutionLayout
                .animate()
                .translationY(0)
                .setDuration(400)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        resolutionLayout.animShowChildView(100);
    }

    public void updateView(){
        cameraView.setAspectRatio(
                UCameraProxy.getPicSize().getHeight(),
                UCameraProxy.getPicSize().getWidth()
        );

        indicatorLayout.setAspectRatio(
                UCameraProxy.getPicSize().getHeight(),
                UCameraProxy.getPicSize().getWidth()
        );

        resolutionLayout.init();

        lensView.setLens(UCameraProxy.getCameraObject().getTitleList(),
                UCameraProxy.getCameraObject().getPhysicIds(),
                UCameraProxy.getCameraObject().getAngleList());

    }
}
