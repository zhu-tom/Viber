package com.tomzhu.viber;

import android.util.Log;

import org.webrtc.CameraVideoCapturer;

public class CameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {
    private static final String TAG = "CameraEventsHandler";
    @Override
    public void onCameraError(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void onCameraDisconnected() {
        Log.i(TAG, "Disconnected");
    }

    @Override
    public void onCameraFreezed(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void onCameraOpening(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void onFirstFrameAvailable() {
        Log.i(TAG, "First frame available");
    }

    @Override
    public void onCameraClosed() {
        Log.i(TAG,"Closed");
    }
}
