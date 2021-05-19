package com.jike.ultracamera.processor;

import com.jike.ultracamera.algorithm.AlgorithmHDR;
import com.jike.ultracamera.camera2.UCameraManager;

public class HDRProcessor extends BaseProcessor{

    public HDRProcessor(int frameCount) {
        super(frameCount);
    }

    @Override
    protected void onFrameAdded(byte[] pixels) {
        AlgorithmHDR.HDRAddYUVFrames(pixels, getFrameCount(),
                UCameraManager.getPicSize().getWidth(),
                UCameraManager.getPicSize().getHeight());
    }

    @Override
    protected void onImageTaken(String path, int idx, int total) {

    }

    @Override
    protected void processAlgorithm() {
        int[] crop = new int[4];
        AlgorithmHDR.HDRProcess(
                UCameraManager.getPicSize().getWidth(),
                UCameraManager.getPicSize().getHeight(),
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
