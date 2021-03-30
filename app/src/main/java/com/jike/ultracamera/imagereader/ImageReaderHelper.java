package com.jike.ultracamera.imagereader;

import android.media.Image;
import android.media.ImageReader;

import com.jike.ultracamera.processor.BaseProcessor;
import com.jike.ultracamera.processor.FusionProcessor;
import com.jike.ultracamera.processor.HDRProcessor;
import com.jike.ultracamera.processor.NightProcessor;
import com.jike.ultracamera.processor.SimpleProcessor;

public class ImageReaderHelper {

    private BaseProcessor processor;

    private ImageReader imageReader;

    public ImageReaderHelper(int w, int h, int format){
        imageReader = ImageReader.newInstance(w, h, format,10);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if(processor != null){
                    Image image = reader.acquireNextImage();
                    if(processor.isOneShotMode()){
                        processor.takePicture(image);
                    }else {
                        processor.addImage(image);
                    }
                }
            }
        }, null);
    }

    public void setFusionProcessor(){ processor = new FusionProcessor(1); }

    public void setSimpleProcessor(){ processor = new SimpleProcessor(1);}

    public void setHdrProcessor(){
        processor = new HDRProcessor(3);
    }

    public void setNightProcessor(){
        processor = new NightProcessor(7);
    }

    public BaseProcessor getProcessor(){
        return processor;
    }

    public ImageReader getImageReader() {
        return imageReader;
    }
}
