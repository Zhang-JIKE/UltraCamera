package com.jike.ultracamera.cameradata;

import android.content.SharedPreferences;
import android.util.Size;

import com.daily.flexui.util.AppContextUtils;
import com.jike.ultracamera.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class CameraResolution {

    private CameraResolution(){}

    private static CameraResolution cameraResolution;

    public static CameraResolution getInstance(){
        if(cameraResolution == null){
            cameraResolution = new CameraResolution();
        }
        return cameraResolution;
    }

    public static final String PIC_INDEX = "REAR_PIC_SIZE_POS";
    public static final String VID_INDEX = "REAR_VIDEO_SIZE_POS";

    public int picIndex = 0;
    //后置拍照所有尺寸
    private List<Size> picSizes = new ArrayList<>();

    public List<Size> getPicSizes() {
        List<Size> sizes = new ArrayList<>();
        for(Size size:picSizes){
            if(size.getWidth() >= getMaxSize(picSizes).getWidth()/2) {
                if (size.getWidth() / size.getHeight() == 4 / 3 ||
                        size.getWidth() / size.getHeight() == 16 / 9 ||
                        size.getWidth() / size.getHeight() == 18 / 9 ||
                        size.getWidth() / size.getHeight() == 4 / 3) {
                    sizes.add(size);
                }
            }
        }
        return picSizes;
    }

    //后置录像设置角标
    public int videoIndex = 0;
    //后置录像所有尺寸
    private List<Size> videoSizes = new ArrayList<>();

    public List<Size> getVideoSizes() {
        List<Size> sizes = new ArrayList<>();
        for(Size size : videoSizes){
            if(size.getHeight() >= 1080 && size.getWidth() >= 1920) {
                if ((float)size.getWidth() / size.getHeight() == (float)16 / 9) {
                    sizes.add(size);
                }
            }
        }
        return sizes;
    }

    //直取后置拍照尺寸
    public Size getPicSize(){
        return getPicSizes().get(picIndex);
    }

    public float getPicRatio(){
        return (float) getPicSize().getWidth()/(float) getPicSize().getHeight();
    }
    //直取后置录像尺寸
    public Size getRearVideoSize(){
        return getVideoSizes().get(videoIndex);
    }

    private Size getMaxSize(List<Size> sizes){
        Size maxSize = sizes.get(0);
        for(Size size:sizes){
            if(size.getWidth()+size.getHeight()>maxSize.getWidth()+maxSize.getHeight()){
                maxSize = size;
            }
        }
        return maxSize;
    }

    public void setPicSizes(List<Size> rearPicSizes) {
        picSizes = rearPicSizes;
    }

    public void setVideoSizes(List<Size> rearVideoSizes) {
        videoSizes = rearVideoSizes;
    }

    public void setPicIndex(int index) {
        picIndex = index;
        DataUtils.saveSettings(PIC_INDEX, index);
    }
    public void setVideoIndex(int index) {
        videoIndex = index;
        DataUtils.saveSettings(VID_INDEX, index);
    }

    public void initSettings(){
        SharedPreferences mSpf = AppContextUtils.getAppContext().getSharedPreferences("Settings",MODE_PRIVATE);
        picIndex = mSpf.getInt(PIC_INDEX, 0);
        videoIndex = mSpf.getInt(VID_INDEX, 0);
    }
}
