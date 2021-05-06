package com.jike.ultracamera.processor;

import com.jike.ultracamera.algorithm.AlgorithmHDR;
import com.jike.ultracamera.cameradata.CameraParameter;
import com.jike.ultracamera.cameradata.CameraResolution;

public class HDRProcessor extends BaseProcessor{

    public HDRProcessor(int frameCount) {
        super(frameCount);
    }

    @Override
    protected void onFrameAdded(byte[] pixels) {
        AlgorithmHDR.HDRAddYUVFrames(pixels, getFrameCount(),
                CameraResolution.getInstance().getPicSize().getWidth(),
                CameraResolution.getInstance().getPicSize().getHeight());
    }

    @Override
    protected void onImageTaken(String path, int idx, int total) {

    }

    @Override
    protected void processAlgorithm() {
        int[] crop = new int[4];
        AlgorithmHDR.HDRProcess(CameraResolution.getInstance().getPicSize().getWidth(),
                CameraResolution.getInstance().getPicSize().getHeight(),
                crop, 0, false);
    }

    @Override
    public boolean isOneShotMode() {
        return false;
    }

    @Override
    public boolean isNeedShutterIndicator() {
        return false;
    }
}
