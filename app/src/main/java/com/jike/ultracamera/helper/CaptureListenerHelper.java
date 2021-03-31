package com.jike.ultracamera.helper;

import android.media.MediaActionSound;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.interfaces.CaptureListener;
import com.jike.ultracamera.view.ShutterView;

public class CaptureListenerHelper {

    private static ShutterView shutterView;
    private static TextView tvTips;
    private static MediaActionSound sound;

    public static void bindView(ShutterView view, TextView textView){
        shutterView = view;
        tvTips = textView;
    }

    public static void unBindView(){
        shutterView = null;
        tvTips = null;
    }

    public static CaptureListener getListener(){
        if(sound == null){
            sound = new MediaActionSound();
        }

        if(captureListener == null){
            captureListener = new CaptureListener() {
                @Override
                public void onCaptureStarted(int duration, boolean useIndicator) {
                    if(useIndicator) {
                        if (shutterView != null) {
                            shutterView.startCapture(duration);
                        }

                        if (tvTips != null) {
                            tvTips.setText("请持稳设备");
                            tvTips.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onCaptureFinished(boolean useIndicator) {
                    sound.play(MediaActionSound.SHUTTER_CLICK);
                    if(useIndicator) {
                        if (shutterView != null) {
                            shutterView.startProcess();
                        }
                        if (tvTips != null) {
                            tvTips.setText("优化处理中");
                            tvTips.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onAlgorithmStarted(boolean useIndicator) {
                }

                @Override
                public void onAllFinished(boolean useIndicator) {
                    if(useIndicator) {
                        if (shutterView != null) {
                            shutterView.backToNormal();
                        }
                        if (tvTips != null) {
                            tvTips.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onImageTaken(String path, int idx, int total) {
                    if(idx == total) {
                        Toast.makeText(AppContextUtils.getAppContext(), "拍照完成", Toast.LENGTH_SHORT).show();
                    }
                }
            };
        }

        return captureListener;
    }

    public static CaptureListener captureListener;
}
