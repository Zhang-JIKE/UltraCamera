package com.jike.ultracamera.processor;

public class HDRProcessor extends BaseProcessor{

    public HDRProcessor(int frameCount) {
        super(frameCount);
    }

    @Override
    protected void onFrameAdded(byte[] pixels) {

    }

    @Override
    protected void onImageTaken(String path, int idx, int total) {

    }

    @Override
    protected void processAlgorithm() {

    }

    @Override
    protected void onAlgorithmStarted() {

    }

    @Override
    protected void onAlgorithmCompleted() {

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
