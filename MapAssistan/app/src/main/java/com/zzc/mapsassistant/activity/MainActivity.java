package com.zzc.mapsassistant.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.help.Tip;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.zzc.mapsassistant.R;
import com.zzc.mapsassistant.listen.CompassListen;
import com.zzc.mapsassistant.listen.LocationListen;
import com.zzc.mapsassistant.listen.MainActivityClickListen;
import com.zzc.mapsassistant.utils.Constants;

public class MainActivity extends BaseActivity {

    private final int PERMISSION_REQUEST = 0x0110;
    private MapView map_view;
    private AMap aMap;

    private BottomSheetBehavior<LinearLayout> behavior;
    private float scale ;
    MyLocationStyle myLocationStyle = null;
    UiSettings uiSettings = null;
    LocationListen listen;

    private final String[] NEED_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    private TextView mKeywordsTextView;
    private ImageView mCleanKeyWords;
    private Button basicmap, rsmap, nightmap;
    public static final int REQUEST_CODE = 100;
    public static final int RESULT_CODE_INPUTTIPS = 101;
    public static final int RESULT_CODE_KEYWORDS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getToolColorStatus();
        scale = getResources().getDisplayMetrics().density;
        //??????????????????
        ServiceSettings.updatePrivacyShow(this,true,true);
        ServiceSettings.updatePrivacyAgree(this,true);
        // ???????????????
        map_view = findViewById(R.id.map_view);
        map_view.onCreate(savedInstanceState);
        if(!checkPermissions(NEED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEED_PERMISSIONS, PERMISSION_REQUEST);
        } else {
            init();
        }
    }

    public void init() {
        // ????????????
        if (aMap == null) {
            aMap = map_view.getMap();
            uiSettings = aMap.getUiSettings();
            // ????????????
        }
        // ????????????
        modelBind();
        // ??????????????????????????????
        getPhoneDayOrNightState();
        // ????????????
        active();
        // ????????????
        uiSet();
        // bottomSheet
        btSheet();
    }

    /**
     * ??????????????????????????????
     */
    private void getPhoneDayOrNightState() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (mode == Configuration.UI_MODE_NIGHT_YES) {
            aMap.setMapType(AMap.MAP_TYPE_NIGHT);
            changeBtnBackground(3);
        } else {
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);
            changeBtnBackground(1);
        }
    }

    /**
     * ????????????????????????
     */
    public void changeBtnBackground(int index) {
        basicmap.setBackgroundResource(R.drawable.btn_shape);
        rsmap.setBackgroundResource(R.drawable.btn_shape);
        nightmap.setBackgroundResource(R.drawable.btn_shape);
        switch (index) {
            case 1:
                basicmap.setBackgroundResource(R.drawable.btn_select_shape);
                break;
            case 2:
                rsmap.setBackgroundResource(R.drawable.btn_select_shape);
                break;
            case 3:
                nightmap.setBackgroundResource(R.drawable.btn_select_shape);
                break;
        }
    }

    /**
     * bottomSheet
     */
    private void btSheet() {
        // ???????????????
        behavior.setHideable(false);
        uiSettings.setLogoBottomMargin((int) (80 * scale + 0.5f));
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            /**
             * ????????????
             */
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    uiSettings.setLogoBottomMargin((int) (80 * scale + 0.5f));
                } else if(newState == BottomSheetBehavior.STATE_EXPANDED) {
                    uiSettings.setLogoBottomMargin((int) (280 * scale + 0.5f));
                }
            }

            /**
             * ????????????????????????????????????????????????slideOffset;
             * ????????????logo???????????????????????????
             */
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset == 0) return;
                uiSettings.setLogoBottomMargin((int) ((80 * scale + 0.5f)+(200 * scale + 0.5f)*slideOffset));
            }
        });
    }

    /**
     * ????????????
     */
    private void modelBind() {
        mCleanKeyWords = findViewById(R.id.clean_keywords);
        mKeywordsTextView = findViewById(R.id.main_keywords);
        LinearLayout bottom_linear_model = findViewById(R.id.bottom_linear_model);
        behavior = BottomSheetBehavior.from(bottom_linear_model);
        // ????????????
        GeocodeSearch geocodeSearch = null;
        try {
            geocodeSearch = new GeocodeSearch(getApplicationContext());
            listen = new LocationListen(aMap, this, geocodeSearch, mKeywordsTextView);
            geocodeSearch.setOnGeocodeSearchListener(listen);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        MainActivityClickListen mainActivityClickListen = new MainActivityClickListen(behavior, listen, aMap);
        basicmap = findViewById(R.id.basicmap);
        basicmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);// ??????????????????
                changeBtnBackground(1);
            }
        });
        rsmap = findViewById(R.id.rsmap);
        rsmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                changeBtnBackground(2);
            }
        });
        nightmap = findViewById(R.id.nightmap);
        nightmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                changeBtnBackground(3);
            }
        });

        CheckBox trafficmap = findViewById(R.id.trafficmap);
        trafficmap.setOnClickListener(mainActivityClickListen);
        CheckBox mapinfo = findViewById(R.id.mapinfo);
        mapinfo.setOnClickListener(mainActivityClickListen);
        CheckBox threemap = findViewById(R.id.threemap);
        threemap.setOnClickListener(mainActivityClickListen);
        CheckBox leveshop = findViewById(R.id.leveshop);
        leveshop.setOnClickListener(mainActivityClickListen);

        Button myLocationBtn = findViewById(R.id.myLocationBtn);
        myLocationBtn.setOnClickListener(mainActivityClickListen);

        Button select_model = findViewById(R.id.select_model);
        select_model.setOnClickListener(mainActivityClickListen);

        mCleanKeyWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mKeywordsTextView.setText("");
                aMap.clear();
                mCleanKeyWords.setVisibility(View.GONE);
                aMap.moveCamera( CameraUpdateFactory.zoomTo(16));
                listen.location();
            }
        });
        mKeywordsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), InputTipsActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        Button route_plan = findViewById(R.id.route_plan);
        route_plan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RouteActivity.class);
                intent.putExtra("end", mKeywordsTextView.getText().toString().trim());
                startActivity(intent);
            }
        });
    }

    /**
     * ??????????????????
     */
    private void uiSet() {
        uiSettings.setMyLocationButtonEnabled(false);  //?????????????????????????????????????????????????????????
        uiSettings.setScaleControlsEnabled(true);    // ???????????????
        uiSettings.setZoomControlsEnabled(false);   // ??????????????????
        uiSettings.setCompassEnabled(false); // ???????????????

        // ???????????????
        ImageView iv_compass = findViewById(R.id.iv_compass);
        CompassListen compassListen = new CompassListen(iv_compass);
        aMap.setOnCameraChangeListener(compassListen);

        // ?????????????????????
        aMap.setOnMyLocationChangeListener(listen);
        aMap.setOnMapTouchListener(listen);
        // ??????????????????
        aMap.setOnMarkerClickListener(listen);
        aMap.setInfoWindowAdapter(listen);
        aMap.addOnPOIClickListener(listen);
    }

    /**
     * ????????????
     */
    private void active() {
        myLocationStyle = new MyLocationStyle();//??????????????????????????????myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????1???1???????????????????????????myLocationType????????????????????????????????????
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        myLocationStyle.interval(2000); //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.nav_block_gps);
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
        myLocationStyle.strokeColor(Color.BLUE);
        myLocationStyle.radiusFillColor(Color.argb(125, 200, 255, 255));// ???????????????????????????
        myLocationStyle.strokeWidth(1);
        aMap.setMyLocationStyle(myLocationStyle);//?????????????????????Style
        aMap.setMyLocationEnabled(true);// ?????????true?????????????????????????????????false??????????????????????????????????????????????????????false???
        listen.location();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ????????????
        map_view.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ????????????????????????
        map_view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ?????????????????????
        map_view.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // ????????????
        map_view.onSaveInstanceState(outState);
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == PERMISSION_REQUEST) {
            if(isAllGranted) {
                init();
            } else {
                showToast("??????????????????, ??????????????????...");
            }
        }
    }

    /**
     * ????????????activity??????????????????????????????
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE_INPUTTIPS && data
                != null) {
            aMap.clear();
            Tip tip = data.getParcelableExtra(Constants.EXTRA_TIP);
            if (tip.getPoiID() == null || tip.getPoiID().equals("")) {
                try {
                    listen.doSearchQuery(tip.getName());
                } catch (AMapException e) {
                    e.printStackTrace();
                }
            } else {
                listen.addTipMarker(tip);
            }
            mKeywordsTextView.setText(tip.getName());
            if (!tip.getName().equals("")) {
                mCleanKeyWords.setVisibility(View.VISIBLE);
            }
        } else if (resultCode == RESULT_CODE_KEYWORDS && data != null) {
            aMap.clear();
            String keywords = data.getStringExtra(Constants.KEY_WORDS_NAME);
            if (keywords != null && !keywords.equals("")) {
                try {
                    listen.doSearchQuery(keywords);
                } catch (AMapException e) {
                    e.printStackTrace();
                }
            }
            mKeywordsTextView.setText(keywords);
            if (!keywords.equals("")) {
                mCleanKeyWords.setVisibility(View.VISIBLE);
            }
        }
    }
}