package com.jike.ultracamera.camera2;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.util.Size;
import android.util.SizeF;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class UCameraManager {

    public static int imageFormat = ImageFormat.JPEG;
    public static CameraManager manager;
    public static CameraCharacteristics characteristics;

    private static boolean isFirstOpen = true;
    public static boolean isOpenedFrontCamera = false;

    public static UCameraObject[] UCameraObjects;

    public static UCameraObject getCameraObject() {
        return UCameraObjects[curCameraObjectIndex];
    }

    public static int curCameraObjectIndex = 0;
    public static int frontCameraObjectIndex = 0;
    public static int backCameraObjectIndex = 0;

    public static class SupportInfo{
        public static boolean isSupportedFrontPhysicalCamera = false;
        public static boolean isSupportedBackPhysicalCamera = false;

        public static boolean isSupported_LENS_STABILIZATION_OIS = false;
        public static boolean isSupported_LENS_STABILIZATION_VIDEO = false;
        public static boolean isSupported_ImagegFormat_RAW = false;
        public static boolean isSupported_ImageFormat_YUV = false;
        public static boolean isSupported_ImageFormat_JPEG = false;
    }

    public static class Resolution {
        public static int picSizeIndex = 0;
        public static int videoSizeIndex = 0;
        public static Size[] picSizeList;
        public static Size[] videoSizeList;
    }

    public static void setIsOpenedFrontCamera(boolean isOpenedFrontCamera) {
        UCameraManager.isOpenedFrontCamera = isOpenedFrontCamera;
        if(isOpenedFrontCamera){
            curCameraObjectIndex = frontCameraObjectIndex;
        }else {
            curCameraObjectIndex = backCameraObjectIndex;
        }
    }

    public static void initCamera(CameraManager manager) {
        UCameraManager.manager = manager;
        if(isFirstOpen) {
            try {
                //获取逻辑摄像头列表
                String[] logicIds = manager.getCameraIdList();
                UCameraObjects = new UCameraObject[logicIds.length];

                //第一次打开，获取前后逻辑摄像头的id
                for (int i = 0; i < logicIds.length; i++) {
                    String camId = logicIds[i];
                    UCameraObjects[i] = new UCameraObject();
                    UCameraObjects[i].setLogicId(camId);

                    characteristics = manager.getCameraCharacteristics(camId);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT) {
                        frontCameraObjectIndex = i;
                        UCameraObjects[i].setFacingFront(true);
                    } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                        backCameraObjectIndex = i;
                        UCameraObjects[i].setFacingFront(false);
                    }

                    //物理摄像头读取
                    Object[] objects = characteristics.getPhysicalCameraIds().toArray();

                        String[] phyIdList = new String[objects.length];
                        String[] titleList = new String[objects.length];
                        double[] angleList = new double[objects.length];

                        for (int j = 0; j < objects.length; j++) {
                            phyIdList[j] = (String) objects[j];
                            double fovy = getFOVY(manager.getCameraCharacteristics(phyIdList[j]));
                            //double fovx = getFOVX(manager.getCameraCharacteristics(phyIdList[j]));
                            angleList[j] = 1.0f / fovy;
                            NumberFormat formatter = new DecimalFormat("0.0");
                            titleList[j] = formatter.format(angleList[j]) + "x";
                        }
                    UCameraObjects[i].setPhysicIds(phyIdList);
                    UCameraObjects[i].setAngleList(angleList);
                    UCameraObjects[i].setTitleList(titleList);

                    if(objects.length > 0) {
                        UCameraObjects[i].setMainPhysicId(phyIdList[0]);
                        UCameraObjects[i].setCurPhysicId(phyIdList[0]);
                    }
                }

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            isFirstOpen = false;
        }
    }

    public static void updateCamera(){
        try {
            UCameraObject UCameraObject;
            if (isOpenedFrontCamera) {
                //设置当前逻辑摄像头ID为前置逻辑摄像头ID
                curCameraObjectIndex = frontCameraObjectIndex;
            } else {
                //设置当前逻辑摄像头ID为后置逻辑摄像头ID
                curCameraObjectIndex = backCameraObjectIndex;
            }
            UCameraObject = UCameraObjects[curCameraObjectIndex];
            characteristics = manager.getCameraCharacteristics(UCameraObject.getLogicId());

            if(UCameraObject.isHasPhysicalCamera()){
                characteristics = manager.getCameraCharacteristics(UCameraObject.getCurPhysicId());
            }

            initPicSizeList();
            initVideoSizeList();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static double getFOVX(CameraCharacteristics characteristics){
        SizeF ps = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        float[] focal = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        float output_physical_width = ps.getWidth();
        return 2 * Math.atan(output_physical_width / 2 / focal[0]);
    }
    public static double getFOVY(CameraCharacteristics characteristics){
        SizeF ps = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        float[] focal = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        float output_physical_height = ps.getHeight();
        return 2 * Math.atan(output_physical_height / 2 / focal[0]);
    }

    public static double getFocalLenth(){
        float[] focal = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        return focal[0];
    }

    public static Size getPicSize(){
        if(Resolution.picSizeIndex >= Resolution.picSizeList.length){
            Resolution.picSizeIndex = 0;
        }
        return Resolution.picSizeList[Resolution.picSizeIndex];
    }

    public static Size getVideoSize(){
        if(Resolution.videoSizeIndex >= Resolution.videoSizeList.length){
            Resolution.videoSizeIndex = 0;
        }
        return Resolution.videoSizeList[Resolution.videoSizeIndex];
    }

    public static Size[] initPicSizeList() {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(imageFormat);
        CameraUtils.sortSizes(sizes);
        Resolution.picSizeList = sizes;
        return Resolution.picSizeList;
    }

    public static Size[] initVideoSizeList() {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(MediaRecorder.class);
        CameraUtils.sortSizes(sizes);
        Resolution.videoSizeList = sizes;
        return Resolution.videoSizeList;
    }

}
