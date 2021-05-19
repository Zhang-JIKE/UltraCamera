package com.jike.ultracamera.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.camera2.UCameraManager;
import com.jike.ultracamera.interfaces.OnImageDetectedListener;


public class BaseCameraView extends TextureView {
    
    protected int mRatioWidth = 0;
    protected int mRatioHeight = 0;
    public float mScaleTime = 1;

    protected OnImageDetectedListener onImageDetectedListener;

    public void setOnImageDetectedListener(OnImageDetectedListener onImageDetectedListener) {
        this.onImageDetectedListener = onImageDetectedListener;
    }

    public CameraControllerView cameraControllerView;

    public void setCameraControllerView(CameraControllerView cameraControllerView) {
        this.cameraControllerView = cameraControllerView;
    }

    public void setAspectRatio(int width, int height) {
        Log.e("Ratio","w:"+width+"h:"+height);
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();

        cameraControllerView.setAspectRatio(width,height);
        cameraControllerView.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    public void configureTransform(int viewWidth, int viewHeight) {

        int rotation = AppContextUtils.getAppActivity().getWindowManager().getDefaultDisplay().getRotation();
        Log.e("Rotation",rotation+"");
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect;
        bufferRect = new RectF(0, 0, UCameraManager.getPicSize().getHeight(), UCameraManager.getPicSize().getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {

            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            float viewLongEdge = viewWidth > viewHeight ? viewWidth : viewHeight;
            float viewShortEdge = viewWidth <= viewHeight ? viewWidth : viewHeight;
            float scale = Math.max(
                    (float) viewShortEdge / UCameraManager.getPicSize().getHeight(),
                    (float) viewLongEdge / UCameraManager.getPicSize().getWidth());
            matrix.postScale(scale, scale, centerX, centerY);

            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        } else {
            //matrix.postRotate(90, centerX, centerY);
        }
        setTransform(matrix);
    }

    public BaseCameraView(Context context) {
        super(context);
    }

    public BaseCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
