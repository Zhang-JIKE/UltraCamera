package com.jike.ultracamera.dialog;

import android.content.Context;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.daily.flexui.util.DisplayUtils;
import com.jike.ultracamera.R;
import com.jike.ultracamera.camera2.UCamera;
import com.jike.ultracamera.view.CheckView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

public class SettingPicSizeDialog extends BottomSheetDialog {

    private LinearLayout itemRearPicSizes;

    private ArrayList<CheckView> checkViews = new ArrayList<>();

    private OnSelectItemListener onSelectItemListener;

    public SettingPicSizeDialog(@NonNull Context context) {
        super(context);
        createView(context);
    }

    public void createView(Context context) {
        final View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_setting_pic_size, null);
        setContentView(bottomSheetView);

        // 注意：这里要给layout的parent设置peekHeight，而不是在layout里给layout本身设置，下面设置背景色同理，坑爹！！！
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(((View) bottomSheetView.getParent()));

        bottomSheetView.post(new Runnable() {
            @Override
            public void run() {
                bottomSheetBehavior.setPeekHeight(((View) bottomSheetView.getParent()).getHeight());
            }
        });

        itemRearPicSizes = findViewById(R.id.item_pic_sizes);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(DisplayUtils.dp2px(26), DisplayUtils.dp2px(14), DisplayUtils.dp2px(26), DisplayUtils.dp2px(14));

        for(int i = 0; i < UCamera.Resolution.picSizeList.length; i++){
            Size size = UCamera.Resolution.picSizeList[i];
            final CheckView checkView = new CheckView(getContext());

            int gcd = gcd(size.getWidth(),size.getHeight());

            checkView.setLayoutParams(params);
            checkView.setTitle(""+size.getWidth()+"x"+size.getHeight()+" - "+size.getWidth()/gcd+" : "+size.getHeight()/gcd);
            checkView.setCheckedIconId(R.drawable.ic_check);

            final int finalI = i;
            checkView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkView.setChecked(true);
                    UCamera.Resolution.picSizeIndex = finalI;
                    unCheckOthersView();
                    if(onSelectItemListener!=null){
                        onSelectItemListener.OnSelectItem(finalI);
                    }
                    dismiss();
                }
            });

            if(i == UCamera.Resolution.picSizeIndex){
                checkView.setChecked(true);
            }

            checkViews.add(checkView);

            itemRearPicSizes.addView(checkView);
        }
    }

    private void unCheckOthersView(){
        for(int i = 0; i < checkViews.size(); i++){
            if(i != UCamera.Resolution.picSizeIndex){
                checkViews.get(i).setChecked(false);
            }
        }
    }

    public void setOnSelectItemListener(OnSelectItemListener onSelectItemListener) {
        this.onSelectItemListener = onSelectItemListener;
    }

    public static int gcd(int x, int y){
        if(y == 0)
            return x;
        else
            return gcd(y,x%y);
    }
}
