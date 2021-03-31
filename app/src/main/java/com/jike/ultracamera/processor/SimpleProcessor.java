package com.jike.ultracamera.processor;

public class SimpleProcessor extends BaseProcessor {


    public SimpleProcessor(int frameCount) {
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
    public boolean isNeedShutterIndicator() {
        return false;
    }

    @Override
    public boolean isOneShotMode() {
        return true;
    }
}
