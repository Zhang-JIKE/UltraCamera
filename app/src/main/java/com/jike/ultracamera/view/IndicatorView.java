package com.jike.ultracamera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jike.ultracamera.R;

public class IndicatorView extends LinearLayout {

    private ImageView ivIndicator;
    private TextView tvIndicator;

    public IndicatorView(Context context) {
        super(context);
        initView();
    }

    public IndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_indicator, this);
        ivIndicator = view.findViewById(R.id.iv_indicator);
        tvIndicator = view.findViewById(R.id.tv_indicator);
    }

    public void setText(String text){
        tvIndicator.setText(text);
    }

    public void setImage(int resId){
        if(resId != 0) {
            ivIndicator.setImageResource(resId);
        }else {
            ivIndicator.setImageDrawable(null);
        }
    }
}
