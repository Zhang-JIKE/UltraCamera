package com.jike.ultracamera.interfaces;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.view.ShutterView;

public class CaptureListenerHelper {

    private static ShutterView shutterView;
    private static TextView tvTips;

    public static void bindView(ShutterView view, TextView textView){
        shutterView = view;
        tvTips = textView;
    }

    public static void unBindView(){
        shutterView = null;
        tvTips = null;
    }

    public static CaptureListener getListener(){
        if(captureListener == null){
            captureListener = new CaptureListener() {
                @Override
                public void onCaptureStarted(int duration) {
                    Log.e("CaptureListener","onCaptureStarted");
                    if(shutterView != null){
                        shutterView.startCapture(duration);
                    }

                    if(tvTips != null){
                        tvTips.setText("请持稳设备");
                        tvTips.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCaptureFinished() {
                    Log.e("CaptureListener","onCaptureFinished");
                    if(shutterView != null){
                        shutterView.startProcess();
                    }
                    if(tvTips != null){
                        tvTips.setText("优化处理中");
                        tvTips.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAlgorithmStarted() {
                    Log.e("CaptureListener","onAlgorithmStarted");
                }

                @Override
                public void onAlgorithmFinished() {
                    Log.e("CaptureListener","onAlgorithmFinished");
                    if(shutterView != null){
                        shutterView.backToNormal();
                    }
                    if(tvTips != null){
                        tvTips.setVisibility(View.GONE);
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
