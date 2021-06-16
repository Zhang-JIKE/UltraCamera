package com.jike.ultracamera.view;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jike.ultracamera.R;

public class IndicatorLayout extends FrameLayout {

    private String describe;
    private int resId;

    protected int mRatioWidth = 0;
    protected int mRatioHeight = 0;

    public IndicatorLayout(@NonNull Context context) {
        super(context);
    }

    public IndicatorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectRatio(int width, int height) {
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
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

    public void setData(String describe, int resId){
        this.describe = describe;
        this.resId = resId;

        removeAllViews();

        IndicatorView view = new IndicatorView(getContext());
        view.setText(describe);
        view.setImage(resId);

        LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        view.setLayoutParams(params);

        addView(view);
    }

    public void show(){
        setVisibility(VISIBLE);
        animate().alphaBy(0).alpha(1).setInterpolator(new LinearInterpolator()).setDuration(300).start();
    }

    public void hide(){
        animate().alphaBy(1).alpha(0).setInterpolator(new LinearInterpolator()).setDuration(400).start();
    }
}
