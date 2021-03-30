package com.jike.ultracamera.cameradata;

public class CameraParameter {

    private CameraParameter(){}

    private static CameraParameter cameraParameter;

    public static CameraParameter getInstance(){
        if(cameraParameter == null){
            cameraParameter = new CameraParameter();
        }
        return cameraParameter;
    }

    public static final long ONE_SECOND = 1000000000;

    public static final long ONE_SECOND_DIV_8 = 250000000;

    public long exposureTime;
    public int iso;
    public int isoBoost;
    public int brightNess;
}
