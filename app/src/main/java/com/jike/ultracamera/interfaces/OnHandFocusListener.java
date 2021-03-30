package com.jike.ultracamera.interfaces;

import android.graphics.Point;

public interface OnHandFocusListener {
    void onHandFocus(Point point);
    void onFocusFallBack();
}
