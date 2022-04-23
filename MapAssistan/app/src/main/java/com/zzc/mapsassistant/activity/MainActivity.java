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
        //隐私政策合规
        ServiceSettings.updatePrivacyShow(this,true,true);
        ServiceSettings.updatePrivacyAgree(this,true);
        // 初始化控件
        map_view = findViewById(R.id.map_view);
        map_view.onCreate(savedInstanceState);
        if(!checkPermissions(NEED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEED_PERMISSIONS, PERMISSION_REQUEST);
        } else {
            init();
        }
    }

    public void init() {
        // 显示地图
        if (aMap == null) {
            aMap = map_view.getMap();
            uiSettings = aMap.getUiSettings();
            // 设置监听
        }
        // 控件绑定
        modelBind();
        // 获取白天或者夜间模式
        getPhoneDayOrNightState();
        // 激活定位
        active();
        // 界面设置
        uiSet();
        // bottomSheet
        btSheet();
    }

    /**
     * 获取白天或者夜间模式
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
     * 选择当前按钮背景
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
        // 防止被隐藏
        behavior.setHideable(false);
        uiSettings.setLogoBottomMargin((int) (80 * scale + 0.5f));
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            /**
             * 状态改变
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
             * 下列函数在滑动的时候回调，会修改slideOffset;
             * 高德地图logo随着弹窗下降而下降
             */
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset == 0) return;
                uiSettings.setLogoBottomMargin((int) ((80 * scale + 0.5f)+(200 * scale + 0.5f)*slideOffset));
            }
        });
    }

    /**
     * 控件绑定
     */
    private void modelBind() {
        mCleanKeyWords = findViewById(R.id.clean_keywords);
        mKeywordsTextView = findViewById(R.id.main_keywords);
        LinearLayout bottom_linear_model = findViewById(R.id.bottom_linear_model);
        behavior = BottomSheetBehavior.from(bottom_linear_model);
        // 地点查询
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
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
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
     * 地图界面设置
     */
    private void uiSet() {
        uiSettings.setMyLocationButtonEnabled(false);  //设置默认定位按钮是否显示，非必需设置。
        uiSettings.setScaleControlsEnabled(true);    // 比例尺显示
        uiSettings.setZoomControlsEnabled(false);   // 缩放按钮禁用
        uiSettings.setCompassEnabled(false); // 指南针显示

        // 自定义罗盘
        ImageView iv_compass = findViewById(R.id.iv_compass);
        CompassListen compassListen = new CompassListen(iv_compass);
        aMap.setOnCameraChangeListener(compassListen);

        // 自定义定位监听
        aMap.setOnMyLocationChangeListener(listen);
        aMap.setOnMapTouchListener(listen);
        // 获取定位信息
        aMap.setOnMarkerClickListener(listen);
        aMap.setInfoWindowAdapter(listen);
        aMap.addOnPOIClickListener(listen);
    }

    /**
     * 激活定位
     */
    private void active() {
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.nav_block_gps);
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
        myLocationStyle.strokeColor(Color.BLUE);
        myLocationStyle.radiusFillColor(Color.argb(125, 200, 255, 255));// 设置圆形的填充颜色
        myLocationStyle.strokeWidth(1);
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        listen.location();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁地图
        map_view.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新绘制加载地图
        map_view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停地图的绘制
        map_view.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存地图
        map_view.onSaveInstanceState(outState);
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == PERMISSION_REQUEST) {
            if(isAllGranted) {
                init();
            } else {
                showToast("获取权限失败, 地图无法使用...");
            }
        }
    }

    /**
     * 输入提示activity选择结果后的处理逻辑
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