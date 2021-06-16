package com.jike.ultracamera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.R;
import com.jike.ultracamera.camera2.UCameraProxy;

public class ResolutionView extends LinearLayout {

    private TextView tvRatio;
    private TextView tvResolution;

    public boolean isChecked = false;

    public ResolutionView(Context context) {
        super(context);
        initView();
    }

    public ResolutionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_resolution, this);
        tvRatio = view.findViewById(R.id.tv_ratio);
        tvResolution = view.findViewById(R.id.tv_resolution);
    }

    public void setText(String ratio,String res){
        tvRatio.setText(ratio);
        tvResolution.setText(res);
    }

    public void setSize(Size size){
        if(Math.abs((size.getWidth() / size.getHeight())
                - (AppContextUtils.getAppContext().getDisplay().getHeight()
                / AppContextUtils.getAppContext().getDisplay().getWidth())) < 0.5){
            tvRatio.setText("Full");
        }else {
            tvRatio.setText(UCameraProxy.Resolution.getRatio(size));
        }
        tvResolution.setText(UCameraProxy.Resolution.getMegaPixel(size));
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
        if(isChecked){
            tvRatio.setBackgroundResource(R.drawable.shape_button_checked);
            tvResolution.setTextColor(getResources().getColor(R.color.colorAccent));
        }else {
            tvRatio.setBackgroundResource(R.drawable.shape_button);
            tvResolution.setTextColor(getResources().getColor(R.color.white));
        }
    }
}
