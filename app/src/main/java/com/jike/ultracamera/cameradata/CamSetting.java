package com.jike.ultracamera.cameradata;

import android.content.SharedPreferences;

import com.daily.flexui.util.AppContextUtils;

import static android.content.Context.MODE_PRIVATE;
import static com.jike.ultracamera.utils.DataUtils.saveSettings;

public class CamSetting {

    public static final String IS_CLICK_SOUND_OPENED ="IS_CLICK_SOUND_OPENED";
    public static final String IS_GEO_OPENED ="IS_GEO_OPENED";
    public static final String IS_MIRROR_OPENED ="IS_MIRROR_OPENED";
    public static final String IS_LINE_OPENED ="IN_LINE_OPENED";
    public static final String IS_FACE_DETECT_OPENED ="IN_FACE_DETECT_OPENED";
    public static final String IS_AI_SCENE_OPENED ="IS_AI_SCENE_OPENED";
    public static final String IS_RAW ="IS_RAW";
    public static final String IS_ACCEL = "IS_ACCEL";
    public static final String IS_DENOISE_OPENED = "IS_DENOISE_OPENED";
    public static final String IS_NIGHT_OPENED = "IS_NIGHT_OPENED";

    public static boolean isClickSoundsOpened = true;
    public static boolean isGeoOpened = true;
    public static boolean isMirrorOpend = true;
    public static boolean isAccel = false;

    public static boolean isLineOpend = true;
    public static boolean isFaceDetectOpend = false;
    public static boolean isAiSceneOpend = false;
    public static boolean isRaw = false;
    public static boolean mFlashSupported = false;
    public static boolean isDenoiseOpened = false;
    public static boolean isNightOpened = false;

    public static void initSettings(){
        SharedPreferences mSpf = AppContextUtils.getAppContext().getSharedPreferences("Settings",MODE_PRIVATE);
        isClickSoundsOpened = mSpf.getBoolean(IS_CLICK_SOUND_OPENED, true);
        isGeoOpened = mSpf.getBoolean(IS_GEO_OPENED, true);
        isMirrorOpend = mSpf.getBoolean(IS_MIRROR_OPENED, true);

        isLineOpend = mSpf.getBoolean(IS_LINE_OPENED, true);
        isFaceDetectOpend = mSpf.getBoolean(IS_FACE_DETECT_OPENED, false);
        isAiSceneOpend = mSpf.getBoolean(IS_AI_SCENE_OPENED, false);
        isRaw = mSpf.getBoolean(IS_RAW, false);
        isAccel = mSpf.getBoolean(IS_ACCEL, false);

        isDenoiseOpened = mSpf.getBoolean(IS_DENOISE_OPENED, false);
        isNightOpened = mSpf.getBoolean(IS_NIGHT_OPENED, false);
    }

    public static void setIsDenoiseOpened(boolean isDenoiseOpened) {
        CamSetting.isDenoiseOpened = isDenoiseOpened;
        saveSettings(IS_DENOISE_OPENED, isDenoiseOpened);
    }

    public static void setIsNightOpened(boolean isNightOpened){
        CamSetting.isNightOpened = isNightOpened;
        saveSettings(IS_NIGHT_OPENED, isNightOpened);
    }

    public static void setIsClickSoundsOpened(boolean isClickSoundsOpened) {
        CamSetting.isClickSoundsOpened = isClickSoundsOpened;
        saveSettings(IS_CLICK_SOUND_OPENED, isClickSoundsOpened);
    }

    public static void setIsGeoOpened(boolean isGeoOpened) {
        CamSetting.isGeoOpened = isGeoOpened;
        saveSettings(IS_GEO_OPENED, isGeoOpened);
    }

    public static void setIsMirrorOpend(boolean isMirrorOpend) {
        CamSetting.isMirrorOpend = isMirrorOpend;
        saveSettings(IS_MIRROR_OPENED, isMirrorOpend);
    }

    public static void setIsLineOpend(boolean isLineOpend) {
        CamSetting.isLineOpend = isLineOpend;
        saveSettings(IS_LINE_OPENED, isLineOpend);
    }

    public static void setIsFaceDetectOpend(boolean isFaceDetectOpend) {
        CamSetting.isFaceDetectOpend = isFaceDetectOpend;
        saveSettings(IS_FACE_DETECT_OPENED, isFaceDetectOpend);
    }

    public static void setIsAiSceneOpend(boolean isAiSceneOpend) {
        CamSetting.isAiSceneOpend = isAiSceneOpend;
        saveSettings(IS_AI_SCENE_OPENED, isAiSceneOpend);
        if(!isAiSceneOpend){
            CamMode.mode = CamMode.Mode.NORMAL;
        }
    }

    public static void setIsRaw(boolean isRaw){
        CamSetting.isRaw = isRaw;
        saveSettings(IS_RAW, isRaw);
    }

    public static void setIsAccel(boolean isAccel){
        CamSetting.isAccel = isAccel;
        saveSettings(IS_ACCEL, isAccel);
    }

}
