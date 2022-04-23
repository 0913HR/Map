package com.zzc.mapsassistant.listen;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.CheckBox;

import com.amap.api.maps.AMap;
import com.amap.api.maps.UiSettings;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.zzc.mapsassistant.R;
import com.zzc.mapsassistant.activity.MainActivity;

public class MainActivityClickListen implements View.OnClickListener {

    private final AMap aMap;
    private final LocationListen listen;
    private final BottomSheetBehavior behavior;

    public MainActivityClickListen(BottomSheetBehavior behavior, LocationListen listen, AMap aMap) {
        this.behavior = behavior;
        this.aMap = aMap;
        this.listen = listen;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.myLocationBtn:
                listen.location();
                break;
            case R.id.trafficmap:
                // 显示实时交通状况
                aMap.setTrafficEnabled(((CheckBox)v).isChecked());
                break;
            case R.id.threemap:
                // 显示3D 楼块
                aMap.showBuildings(((CheckBox)v).isChecked());
                break;
            case R.id.mapinfo:
                // 显示底图文字
                aMap.showMapText(((CheckBox)v).isChecked());
                break;
            case R.id.leveshop:
                aMap.showIndoorMap(((CheckBox)v).isChecked());
                break;
            case R.id.select_model:
                if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                break;
        }
    }
}
