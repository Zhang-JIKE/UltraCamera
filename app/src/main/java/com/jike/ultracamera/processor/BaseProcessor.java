package com.jike.ultracamera.processor;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;


import com.jike.ultracamera.helper.BitmapHelper;
import com.jike.ultracamera.helper.ImageToByteArrayHelper;
import com.jike.ultracamera.image.YuvImage;
import com.jike.ultracamera.interfaces.CaptureListenerHelper;
import com.jike.ultracamera.utils.Camera2Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BaseProcessor {

    private static final int MSG_ON_ALG_STARTED = 0x0000010;
    private static final int MSG_ON_ALG_COMPLETED = 0x0000011;
    private static final int MSG_ON_IMAGE_TAKEN = 0x0000012;
    private static final int MSG_ON_IMAGE_ADDED = 0x0000013;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == MSG_ON_ALG_STARTED){
                onAlgorithmStarted();
                if(isNeedShutterIndicator()){
                    CaptureListenerHelper.getListener().onAlgorithmStarted();
                }
            }else if(msg.what == MSG_ON_ALG_COMPLETED){
                onAlgorithmCompleted();
                if(isNeedShutterIndicator()){
                    CaptureListenerHelper.getListener().onAlgorithmFinished();
                }
            }else if(msg.what == MSG_ON_IMAGE_TAKEN){
                String path = (String) msg.obj;
                onImageTaken(path,finishedFrameCount+1, frameCount);
                CaptureListenerHelper.getListener().onImageTaken(path,finishedFrameCount+1, frameCount);

                finishedFrameCount++;
                if(finishedFrameCount == frameCount){
                    if(isNeedShutterIndicator()){
                        CaptureListenerHelper.getListener().onAlgorithmFinished();
                    }
                    finishedFrameCount = 0;
                }

            }else if(msg.what == MSG_ON_IMAGE_ADDED){
                onFrameAdded(frameData);
            }
        }
    };

    private ThreadPoolExecutor executor;
    private LinkedBlockingQueue<Runnable> runnableQueue;

    private Image[] images;


    private int frameCount = 8;
    private int curFrame = 0;

    private volatile byte[] frameData;
    private volatile int finishedFrameCount = 0;

    public BaseProcessor(int frameCount){
        this.frameCount = frameCount;
        this.runnableQueue = new LinkedBlockingQueue<>();
        this.images = new Image[frameCount];

        executor = new ThreadPoolExecutor(3,8,100, TimeUnit.MILLISECONDS,runnableQueue);
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void addImage(final Image image){
        images[curFrame] = image;

        if(curFrame == frameCount) {
            for(int i = 0; i < images.length; i++) {
                final Image curImage = images[i];
                Runnable task = getRunnableTask(curImage);
                executor.execute(task);
            }
        }

        curFrame++;
        if(curFrame == frameCount) curFrame = 1;
    }

    public void takePicture(final Image image){
        Thread takeThread = new Thread(){
            @Override
            public void run() {
                int format = image.getFormat();
                String path = null;

                if(format == ImageFormat.YUV_420_888) {
                    path = saveYuv(image);
                }else if(format == ImageFormat.JPEG) {
                    path = saveJpeg(image);
                }
                image.close();

                Message message = Message.obtain();
                message.what = MSG_ON_IMAGE_TAKEN;
                message.obj = path;
                handler.sendMessage(message);
            }
        };
        takeThread.start();
    }

    private synchronized Runnable getRunnableTask(final Image curImage){
        Runnable task = new Runnable() {
            @Override
            public void run() {

                int format = curImage.getFormat();
                int frame = 0;
                frameData = new byte[0];
                int frame_len = 0;

                if (format == ImageFormat.JPEG) {
                    ByteBuffer jpeg = curImage.getPlanes()[0].getBuffer();
                    frame_len = jpeg.limit();
                    frameData = new byte[frame_len];
                    jpeg.get(frameData, 0, frame_len);
                } else if (format == ImageFormat.YUV_420_888) {
                    ByteBuffer Y = curImage.getPlanes()[0].getBuffer();
                    ByteBuffer U = curImage.getPlanes()[1].getBuffer();
                    ByteBuffer V = curImage.getPlanes()[2].getBuffer();

                    if ((!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect())) return;

                    YuvImage.CreateYUVImage(
                            Y, U, V,
                            curImage.getPlanes()[0].getPixelStride(),
                            curImage.getPlanes()[0].getRowStride(),
                            curImage.getPlanes()[1].getPixelStride(),
                            curImage.getPlanes()[1].getRowStride(),
                            curImage.getPlanes()[2].getPixelStride(),
                            curImage.getPlanes()[2].getRowStride(),
                            curImage.getWidth(),
                            curImage.getHeight());

                    frameData = YuvImage.GetByteFrame();
                    frame_len = curImage.getWidth() * curImage.getHeight() + curImage.getWidth() * ((curImage.getHeight() + 1) / 2);

                } else if (format == ImageFormat.RAW_SENSOR) {
                    ByteBuffer raw = curImage.getPlanes()[0].getBuffer();

                    frame_len = raw.limit();
                    frameData = new byte[frame_len];
                    raw.get(frameData, 0, frame_len);
                }

                curImage.close();

                Message message0 = Message.obtain();
                message0.what = MSG_ON_IMAGE_ADDED;
                handler.sendMessage(message0);

                finishedFrameCount++;
                if(finishedFrameCount >= frameCount) {
                    for(Image image : images){
                        image = null;
                    }
                    System.gc();


                    Message message1 = Message.obtain();
                    message1.what = MSG_ON_ALG_STARTED;
                    handler.sendMessage(message1);

                    processAlgorithm();

                    Message message2 = Message.obtain();
                    message2.what = MSG_ON_ALG_COMPLETED;
                    handler.sendMessage(message2);


                    finishedFrameCount = 0;
                }
            }
        };
        return task;
    }

    protected abstract void onFrameAdded(byte[] pixels);

    protected abstract void onImageTaken(String path, int idx, int total);

    protected abstract void processAlgorithm();

    protected abstract void onAlgorithmStarted();

    protected abstract void onAlgorithmCompleted();

    //是否需要指示器
    public boolean isNeedShutterIndicator(){
        return false;
    }

    //是否每张照片都保存
    public boolean isOneShotMode(){
        return true;
    }

    private String getPath(String title){
        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SS");
        //SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date=sTimeFormat.format(new Date());

        String finalP = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/Camera" + "/CPro-" + title + date + ".jpg";
        return finalP;
    }
    private String saveYuv(Image image){
        byte[] bytes = ImageToByteArrayHelper.getYuvByteArray(image);
        Bitmap bitmap = BitmapHelper.getBitmapFromYuv(bytes, image.getWidth(),image.getHeight());
        return BitmapHelper.saveBitmap("Yuv", bitmap);
    }
    private String saveJpeg(Image image){
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        String path = getPath("Jpg");
        try {
            File file = new File(path);
            output = new FileOutputStream(file);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Camera2Utils.galleryAddPic(path);
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return path;
    }
    /*private void saveRaw(Image image){
        DngCreator dngCreator = new DngCreator(characteristics, captureResult);
        FileOutputStream output = null;
        try {
            SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SS");
            String date = sTimeFormat.format(new Date());

            File file = new File("/sdcard/"+date+".dng");
            output = new FileOutputStream(file);
            dngCreator.writeImage(output, mImage);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
        }
    }*/
}
