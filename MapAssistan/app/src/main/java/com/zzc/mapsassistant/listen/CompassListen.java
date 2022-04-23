package com.zzc.mapsassistant.listen;

import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.CameraPosition;

public class CompassListen implements AMap.OnCameraChangeListener {
    private float lastBearing = 0;
    private RotateAnimation rotateAnimation;
    private ImageView imageView;
    public CompassListen(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        startIvCompass(cameraPosition.bearing);
    }

    private void startIvCompass(float bearing) {
            bearing = 360 - bearing;
            rotateAnimation = new RotateAnimation(lastBearing, bearing, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setFillAfter(true);
            imageView.startAnimation(rotateAnimation);
            lastBearing = bearing;
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {

    }
}
