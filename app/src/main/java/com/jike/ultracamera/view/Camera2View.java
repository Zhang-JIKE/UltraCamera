package com.jike.ultracamera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.jike.ultracamera.camera2.UCameraController;

public class Camera2View extends BaseCameraView {

  private UCameraController UCameraController;

  public Camera2View(Context context) {
    super(context);
  }

  public Camera2View(Context context, AttributeSet attrs) {
    super(context, attrs);
    UCameraController = UCameraController.getInstance();
  }

  public UCameraController getController() {
    return UCameraController;
  }
}
