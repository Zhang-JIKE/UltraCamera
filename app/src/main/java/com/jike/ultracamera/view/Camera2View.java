package com.jike.ultracamera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.jike.ultracamera.camera2.Camera2Controller;

public class Camera2View extends BaseCameraView {

  private Camera2Controller camera2Controller;

  public Camera2View(Context context) {
    super(context);
  }

  public Camera2View(Context context, AttributeSet attrs) {
    super(context, attrs);
    camera2Controller = Camera2Controller.getInstance();
  }

  public Camera2Controller getCamera2Controller() {
    return camera2Controller;
  }
}
