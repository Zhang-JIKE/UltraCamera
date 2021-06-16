package com.jike.ultracamera.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.daily.flexui.interpolator.uiengine.FlexUiEngine;
import com.daily.flexui.util.DisplayUtils;
import com.daily.flexui.view.abstractview.BaseView;
import com.jike.ultracamera.camera2.module.manager.ModuleManager;
import com.jike.ultracamera.R;

import java.util.ArrayList;

public class TabTextView extends BaseView {

    private int baseline;
    private ArrayList<String> tabText = new ArrayList<>();
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Point> oldPoints = new ArrayList<>();
    private ArrayList<Integer> tabWidths = new ArrayList<>();
    private float textSize;

    private Paint defaultPaint;
    private Paint.FontMetricsInt fontMetrics;

    private Paint hintPaint;

    public static int nowIndex = 0;

    private ValueAnimator animator;

    int widSum = 0;
    private float donwX;

    private int hMargin = DisplayUtils.dp2px(42);
    private int indicatorRadius = DisplayUtils.dp2px(3);

    public interface TabListener{
        void onTabSelected(int idx);
    }

    private TabListener tabListener;

    public void setTabListener(TabListener tabListener) {
        this.tabListener = tabListener;
    }

    public TabTextView(Context context) {
        super(context);
    }

    public TabTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(AttributeSet attrs) {
        TypedArray array = getContext().getTheme().obtainStyledAttributes(attrs, com.daily.flexui.R.styleable.FIconView, 0, 0);
        textSize = array.getDimension(com.daily.flexui.R.styleable.GradientTextView_gradienttextview_textsize, DisplayUtils.sp2px(14.5f));

        defaultPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        defaultPaint.setColor(Color.WHITE);
        defaultPaint.setTextSize(textSize);
        defaultPaint.setTextAlign(Paint.Align.LEFT);
        defaultPaint.setFakeBoldText(true);
        fontMetrics = defaultPaint.getFontMetricsInt();

        hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintPaint.setColor(getResources().getColor(R.color.colorAccent));
        hintPaint.setStyle(Paint.Style.FILL);


        animator = ValueAnimator.ofFloat();
    }

    public void setData(ModuleManager manager){
        for(int i = 0; i < manager.modules.size(); i++){
            if(manager.modules.get(i).getModuleName().equals("拍照")){
                nowIndex = i;
            }
            tabText.add(manager.modules.get(i).getModuleName());
            points.add(new Point());
        }

        for (int i = 0; i < tabText.size(); i++) {
            int wid = (int) defaultPaint.measureText(tabText.get(i));
            tabWidths.add(wid);
            widSum += wid;
        }
        widSum += (tabText.size() + 1) * hMargin;

        requestLayout();
    }

    @Override
    public int getWrapContentHeight() {
        return (int) (fontMetrics.bottom - fontMetrics.top + getPaddingBottom() + getPaddingTop());
    }

    @Override
    public int getWrapContentWidth() {
        return widSum;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Rect rect = new Rect(0,0,width,height);
        baseline = rect.top + (rect.bottom - rect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;

        if(points != null && points.size() != 0) {
            points.get(nowIndex).set(width / 2 - tabWidths.get(nowIndex) / 2, baseline);

            for (int i = nowIndex - 1; i >= 0; i--) {
                points.get(i).set(points.get(i + 1).x - (tabWidths.get(i + 1)) - hMargin, baseline);
            }

            for (int j = nowIndex + 1; j < tabText.size(); j++) {
                points.get(j).set(points.get(j - 1).x + (tabWidths.get(j - 1)) + hMargin, baseline);
            }

            oldPoints = new ArrayList<>();
            oldPoints.addAll(points);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(int i = 0; i < tabText.size(); i++){
            canvas.drawText(tabText.get(i), points.get(i).x,points.get(i).y - getPaddingBottom() + getPaddingTop(),defaultPaint);
        }

        canvas.drawCircle(width/2,height-indicatorRadius*2,indicatorRadius,hintPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            donwX = event.getX();
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            float deltaX = event.getX() - donwX;

            for(int j = 0; j < points.size(); j++){
                points.set(j, new Point((int) deltaX + oldPoints.get(j).x, baseline));
            }
            invalidate();
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            oldPoints = new ArrayList<>();
            oldPoints.addAll(points);

            //移动值为0视为点击事件
            if(Math.abs(event.getX() - donwX) == 0){
                int nearestIdx = getFingerIndex((int) event.getX());
                animToIndex(nearestIdx);
            }
            //移动值小于间隔视为小滑动事件
            else if(Math.abs(event.getX() - donwX) <= hMargin){
                if ((event.getX() - donwX > 0)) {
                    animToPrior();
                } else {
                    animToNext();
                }
            }
            //移动值大于间隔视为大拖动事件
            else {
                int nearestIdx = getNearestIndex();
                animToIndex(nearestIdx);
            }
        }
        return true;
    }

    private int getNearestIndex(){
        if(points == null || points.size() == 0) return 0;

        int num = Math.abs(points.get(0).x + tabWidths.get(0) / 2 - width / 2);
        int miniIdx = 0;
        for (int i = 1; i < points.size(); i++) {
            int a = Math.abs(points.get(i).x + tabWidths.get(i) / 2 - width / 2);
            if (a < num) {
                num = a;
                miniIdx = i;
            }
        }
        return miniIdx;
    }

    private int getFingerIndex(int x){
        if(points == null || points.size() == 0) return 0;

        int num = Math.abs(points.get(0).x + tabWidths.get(0) / 2 - x);
        int miniIdx = 0;
        for (int i = 1; i < points.size(); i++) {
            int a = Math.abs(points.get(i).x + tabWidths.get(i) / 2 - x);
            if (a < num) {
                num = a;
                miniIdx = i;
            }
        }
        return miniIdx;
    }

    public void animToIndex(final int idx){
        if(points == null || points.size() == 0) return;
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

        if(tabListener != null){
            if(nowIndex != idx) {
                nowIndex = idx;
                tabListener.onTabSelected(idx);
            }
        }
        nowIndex = idx;

        if(animator != null && animator.isRunning()){
            animator.cancel();
        }else {
            float offset = points.get(idx).x - width / 2 + tabWidths.get(idx) / 2;
            animator = ValueAnimator.ofFloat(0, -offset);
            animator.setInterpolator(FlexUiEngine.ScrollInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float v = (float) animation.getAnimatedValue();
                    for (int j = 0; j < points.size(); j++) {
                        points.set(j, new Point((int) v + oldPoints.get(j).x, baseline));
                    }
                    invalidate();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    oldPoints = new ArrayList<>();
                    oldPoints.addAll(points);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    oldPoints = new ArrayList<>();
                    oldPoints.addAll(points);
                    nowIndex = idx;
                }

                @Override
                public void onAnimationPause(Animator animation) {
                    oldPoints = new ArrayList<>();
                    oldPoints.addAll(points);
                    nowIndex = idx;
                }
            });
            animator.setDuration(250);
            animator.start();
        }
    }

    public void animToNext(){
        int i = nowIndex + 1;
        if(i > tabText.size() - 1) {
            i = tabText.size() - 1;
        }
        animToIndex(i);
    }

    public void animToPrior(){
        int i = nowIndex - 1;
        if(i < 0) {
            i = 0;
        }
        animToIndex(i);

    }
}
