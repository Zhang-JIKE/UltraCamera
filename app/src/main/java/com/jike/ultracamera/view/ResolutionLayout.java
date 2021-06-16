package com.jike.ultracamera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.jike.ultracamera.camera2.UCameraProxy;

public class ResolutionLayout extends AverageLayout {

    private int selectIndex = 0;

    private ResolutionView[] resolutionViews;

    private OnItemSelectedListener onItemSelectedListener;

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public interface OnItemSelectedListener{
        void OnItemSelected(int index);
    }


    public ResolutionLayout(Context context) {
        super(context);
    }

    public ResolutionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(){
        removeAllViews();

        resolutionViews = new ResolutionView[UCameraProxy.Resolution.picSizeList.length];
        for(int i = 0; i < resolutionViews.length; i++){
            resolutionViews[i] = new ResolutionView(getContext());
            resolutionViews[i].setSize(UCameraProxy.Resolution.picSizeList[i]);
            int finalI = i;
            resolutionViews[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    selectIndex = finalI;

                    if(onItemSelectedListener != null){
                        if(UCameraProxy.Resolution.picSizeIndex != selectIndex) {
                            UCameraProxy.Resolution.picSizeIndex = finalI;
                            onItemSelectedListener.OnItemSelected(finalI);
                        }
                    }
                    UCameraProxy.Resolution.picSizeIndex = finalI;
                    resolutionViews[finalI].setChecked(true);

                    for(int j = 0; j < resolutionViews.length; j++){
                        if(finalI != j){
                            resolutionViews[j].setChecked(false);
                        }
                    }
                }
            });
            addView(resolutionViews[i]);
        }

        resolutionViews[UCameraProxy.Resolution.picSizeIndex].setChecked(true);
    }
}
