package com.jike.ultracamera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.daily.flexui.util.DisplayUtils;
import com.daily.flexui.view.abstractview.BaseView;
import com.jike.ultracamera.R;
import com.jike.ultracamera.camera2.UCameraManager;

public class LensView extends BaseView {

    private int selectedIndex = 0;
    private float oldx;

    private String[] lensTitle = new String[]{"1x"};
    private String[] lensIndex = new String[]{"0"};
    private double[] angle = new double[]{1};

    private int stroke;
    private int radius;
    private int textSize;
    private int margin;

    private Rect[] rawRects = new Rect[]{new Rect(0,0,radius*2,radius*2)};

    private Paint circlePaint;
    private Paint unSelectPaint;
    private Paint selectPaint;
    private Paint textPaint;
    private Paint.FontMetricsInt fontMetrics;
    private float fontWidth;
    private float fontHeight;

    private OnLensSelectedListener onLensSelectedListener;

    public void setOnLensSelectedListener(OnLensSelectedListener onLensSelectedListener) {
        this.onLensSelectedListener = onLensSelectedListener;
    }

    public interface OnLensSelectedListener{
        void onLensSelectedChanged(String id);
    }

    public LensView(Context context) {
        super(context);
    }

    public LensView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(AttributeSet attrs) {
        stroke = DisplayUtils.dp2px(1.4f);
        radius = DisplayUtils.dp2px(16);
        textSize = DisplayUtils.sp2px(10);
        margin = DisplayUtils.dp2px(10);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unSelectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        circlePaint.setColor(getResources().getColor(R.color.colorBlack));
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAlpha(100);

        selectPaint.setColor(getResources().getColor(R.color.colorWhite));
        selectPaint.setStyle(Paint.Style.STROKE);
        selectPaint.setStrokeWidth(stroke);

        unSelectPaint.setColor(getResources().getColor(R.color.colorWhite));
        unSelectPaint.setStyle(Paint.Style.STROKE);
        unSelectPaint.setStrokeWidth(stroke);
        unSelectPaint.setAlpha(100);


        textPaint.setColor(getResources().getColor(R.color.colorWhite));
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        fontMetrics = textPaint.getFontMetricsInt();


        fontHeight = fontMetrics.bottom - fontMetrics.top;
        Log.e("fontHeight",""+fontHeight);
    }

    public void setLens(String[] lensTitle, String[] lensIndex, double[] angle) {
        if(lensTitle != null && lensIndex != null && lensTitle.length > 0 && lensIndex.length > 0) {
            this.lensTitle = lensTitle;
            this.lensIndex = lensIndex;
            rawRects = new Rect[lensIndex.length];
            bubbleSort(angle);

            for (int i = 0; i < rawRects.length; i++) {
                rawRects[i] = new Rect();
                rawRects[i].left = i * radius*2 + i * margin + getPaddingLeft();
                rawRects[i].right = (i + 1) * radius*2 + i * margin + getPaddingLeft();
                rawRects[i].top = getPaddingTop();
                rawRects[i].bottom = getPaddingTop() + radius*2;

                if(UCameraManager.getCameraObject().getCurPhysicId().equals(lensIndex[i])){
                    selectedIndex = i;
                }
            }
        }else {
            this.lensTitle = new String[]{"1x"};
            this.lensIndex = new String[]{"0"};
            this.rawRects = new Rect[]{new Rect(0,0,radius*2,radius*2)};
            selectedIndex = 0;
        }
        requestLayout();
    }

    public void bubbleSort(double[] angle) {
        double t = 0;
        int id = 0;
        String title;
        String index;
        for (int i = 0; i < angle.length - 1; i++)
            for (int j = 0; j < angle.length - 1 - i; j++)
                if (angle[j] > angle[j + 1]) {
                    t = angle[j];
                    title = lensTitle[j];
                    index = lensIndex[j];

                    angle[j] = angle[j + 1];
                    lensTitle[j] = lensTitle[j + 1];
                    lensIndex[j] = lensIndex[j + 1];

                    angle[j + 1] = t;
                    lensTitle[j + 1] = title;
                    lensIndex[j + 1] = index;
                }
    }

    @Override
    public int getWrapContentWidth() {
        return radius * lensIndex.length * 2
                + margin * (lensIndex.length - 1)
                + getPaddingLeft() + getPaddingRight();
    }

    @Override
    public int getWrapContentHeight() {
        return radius * 2 + getPaddingTop() + getPaddingBottom();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(int i = 0; i < rawRects.length; i++) {
            canvas.drawCircle(rawRects[i].centerX(), rawRects[i].centerY(), radius, circlePaint);
            if(i == selectedIndex) {
                canvas.drawCircle(rawRects[i].centerX(), rawRects[i].centerY(), radius - stroke / 2, selectPaint);
            }else {
                canvas.drawCircle(rawRects[i].centerX(), rawRects[i].centerY(), radius - stroke / 2, unSelectPaint);

            }
            float fonty = rawRects[i].centerY() - fontHeight/2 - fontMetrics.top;

            canvas.drawText(lensTitle[i],rawRects[i].centerX(), fonty,textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        if(action == MotionEvent.ACTION_DOWN){
            oldx = event.getX();
        }else if(action == MotionEvent.ACTION_UP){
            float dx = event.getX() - oldx;
            if(Math.abs(dx)<10){
                int minIdx = 0;
                float min = Math.abs(oldx - rawRects[minIdx].centerX());
                for(int i = 1; i < rawRects.length; i++){
                    float x = Math.abs(oldx - rawRects[i].centerX());
                    if(x < min){
                        min = x;
                        minIdx = i;
                    }
                }
                if(onLensSelectedListener != null && minIdx != selectedIndex){
                    selectedIndex = minIdx;
                    invalidate();
                    onLensSelectedListener.onLensSelectedChanged(lensIndex[selectedIndex]);
                }


            }
        }
        return true;
    }
}
