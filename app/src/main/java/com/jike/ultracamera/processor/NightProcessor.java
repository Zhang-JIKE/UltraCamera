package com.jike.ultracamera.processor;

public class NightProcessor extends BaseProcessor{

    public NightProcessor(int frameCount) {
        super(frameCount);
    }

    @Override
    public void onFrameAdded(byte[] pixels) {

    }

    @Override
    protected void onImageTaken(String path, int idx, int total) {

    }

    @Override
    public void processAlgorithm() {

    }

    @Override
    public boolean isNeedShutterIndicator() {
        return true;
    }


    @Override
    public boolean isOneShotMode() {
        return true;
    }
}
