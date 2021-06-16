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

import com.daily.flexui.util.AppContextUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class UCameraProxy {

    public static int imageFormat = ImageFormat.JPEG;
    public static CameraManager manager;
    public static CameraCharacteristics characteristics;

    private static boolean isFirstOpen = true;
    public static boolean isOpenedFrontCamera = false;

    public static UCamera[] UCameras;

    public static UCamera getCameraObject() {
        return UCameras[curCameraObjectIndex];
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

        public static String getRatio(Size size){
            int gcd = gcd(size.getWidth(),size.getHeight());
            return size.getWidth() / gcd + ":" + size.getHeight() / gcd;
        }

        public static String getMegaPixel(Size size){
            int res = size.getHeight() * size.getWidth();
            return res / 1000000 + "MP";
        }

        public static int gcd(int x, int y){
            if(y == 0)
                return x;
            else
                return gcd(y,x%y);
        }
    }

    public static void setIsOpenedFrontCamera(boolean isOpenedFrontCamera) {
        UCameraProxy.isOpenedFrontCamera = isOpenedFrontCamera;
        if(isOpenedFrontCamera){
            curCameraObjectIndex = frontCameraObjectIndex;
        }else {
            curCameraObjectIndex = backCameraObjectIndex;
        }
    }

    public static void initCamera(CameraManager manager) {
        UCameraProxy.manager = manager;
        if(isFirstOpen) {
            try {
                //获取逻辑摄像头列表
                String[] logicIds = manager.getCameraIdList();
                UCameras = new UCamera[logicIds.length];

                //第一次打开，获取前后逻辑摄像头的id
                for (int i = 0; i < logicIds.length; i++) {
                    String camId = logicIds[i];
                    UCameras[i] = new UCamera();
                    UCameras[i].setLogicId(camId);

                    characteristics = manager.getCameraCharacteristics(camId);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT) {
                        frontCameraObjectIndex = i;
                        UCameras[i].setFacingFront(true);
                    } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                        backCameraObjectIndex = i;
                        UCameras[i].setFacingFront(false);
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
                    UCameras[i].setPhysicIds(phyIdList);
                    UCameras[i].setAngleList(angleList);
                    UCameras[i].setTitleList(titleList);

                    if(objects.length > 0) {
                        UCameras[i].setMainPhysicId(phyIdList[0]);
                        UCameras[i].setCurPhysicId(phyIdList[0]);
                    }
                }

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            isFirstOpen = false;
        }
    }

    public static void readCamera(){
        try {
            UCamera UCamera;
            if (isOpenedFrontCamera) {
                //设置当前逻辑摄像头ID为前置逻辑摄像头ID
                curCameraObjectIndex = frontCameraObjectIndex;
            } else {
                //设置当前逻辑摄像头ID为后置逻辑摄像头ID
                curCameraObjectIndex = backCameraObjectIndex;
            }
            UCamera = UCameras[curCameraObjectIndex];
            characteristics = manager.getCameraCharacteristics(UCamera.getLogicId());

            if(UCamera.isHasPhysicalCamera()){
                characteristics = manager.getCameraCharacteristics(UCamera.getCurPhysicId());
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

        ArrayList<Size> sizeArrayList = new ArrayList<>();
        int r43 = 0;
        int r169 = 0;
        int rfull = 0;
        for(int i = 0; i < sizes.length; i++){
            Size size = sizes[i];
            if(Resolution.getRatio(size).equals("4:3")){
                if(r43 <= 1){
                    sizeArrayList.add(size);
                    r43++;
                }

            }else if(Resolution.getRatio(size).equals("16:9")){
                if(r169 == 0){
                    sizeArrayList.add(size);
                    r169++;
                }
            }else if(Math.abs((size.getWidth() / size.getHeight())
                    - (AppContextUtils.getAppContext().getDisplay().getHeight()
                    / AppContextUtils.getAppContext().getDisplay().getWidth())) < 0.5){
                if(rfull == 0){
                    sizeArrayList.add(size);
                    rfull++;
                }
            }
        }

        Size[] nSizes = new Size[sizeArrayList.size()];
        for(int i = 0; i < sizeArrayList.size(); i++){
            nSizes[i] = sizeArrayList.get(i);
        }
        Resolution.picSizeList = nSizes;
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
