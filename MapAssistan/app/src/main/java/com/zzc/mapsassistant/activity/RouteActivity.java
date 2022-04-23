package com.zzc.mapsassistant.activity;

import static com.zzc.mapsassistant.utils.AMapUtil.convertToLatLng;
import static com.zzc.mapsassistant.utils.AMapUtil.convertToLatLonPoint;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.zzc.mapsassistant.R;
import com.zzc.mapsassistant.overlay.BusRouteOverlay;
import com.zzc.mapsassistant.overlay.DrivingRouteOverlay;
import com.zzc.mapsassistant.overlay.RideRouteOverlay;
import com.zzc.mapsassistant.overlay.WalkRouteOverlay;
import com.zzc.mapsassistant.utils.AMapUtil;
import com.zzc.mapsassistant.utils.ToastUtils;

import java.util.List;

public class RouteActivity extends BaseActivity implements
        AMap.OnMapClickListener, RouteSearch.OnRouteSearchListener,
        View.OnKeyListener, AMapLocationListener, LocationSource,
        GeocodeSearch.OnGeocodeSearchListener {

    private MapView mapView;
    private AMap aMap;
    //路线规划详情
    private RelativeLayout bottomLayout;
    //花费时间
    private TextView tvTime;

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //位置更改监听
    private OnLocationChangedListener mListener;

    //出行方式数组
    private static final String[] travelModeArray = {"步行出行", "骑行出行", "驾车出行", "公交出行"};
    //出行方式值
    private static int TRAVEL_MODE = 0;
    //数组适配器
    private ArrayAdapter<String> arrayAdapter;

    //起点
    private LatLonPoint mStartPoint;
    //终点
    private LatLonPoint mEndPoint;
    //城市
    private String city;
    //起点、终点
    private EditText etStartAddress, etEndAddress;

    //路线搜索对象
    private RouteSearch routeSearch;

    //地理编码搜索
    private GeocodeSearch geocodeSearch;
    //解析成功标识码
    private static final int PARSE_SUCCESS_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        getToolColorStatus();
        //初始化定位
        initLocation();

        init();

        //启动定位
        mLocationClient.startLocation();

        //初始化路线
        initRoute();

        //初始化出行方式
        initTravelMode();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mLocationClient.setLocationListener(this);
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setOnceLocationLatest(true);
        mLocationOption.setNeedAddress(true);
        mLocationOption.setHttpTimeOut(20000);
        mLocationOption.setLocationCacheEnable(false);
        mLocationClient.setLocationOption(mLocationOption);
    }

    /**
     * 初始化出行方式
     */
    private void initTravelMode() {
        Spinner spinner = findViewById(R.id.spinner);

        bottomLayout = findViewById(R.id.bottom_layout);
        tvTime = findViewById(R.id.tv_time);

        etStartAddress = findViewById(R.id.et_start_address);

        etEndAddress = findViewById(R.id.et_end_address);
        Intent intent = getIntent();
        String end = intent.getStringExtra("end");
        etEndAddress.setText(end);
        //键盘按键监听
        etEndAddress.setOnKeyListener(this);

        //将可选内容与ArrayAdapter连接起来
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, travelModeArray);
        //设置下拉列表风格
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将arrryAdapter添加到spinner中
        spinner.setAdapter(arrayAdapter);
        //添加事件Spinner事件监听
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TRAVEL_MODE = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 初始化路线
     */
    private void initRoute() {
        try {
            routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        //地图的缩放级别一共分为 17 级，从 3 到 19。数字越大，展示的图面信息越精细。
        aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
    }

    private void setUpMap() {
        //初始化定位蓝点样式类
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        //设置定位标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.nav_block_gps));
        myLocationStyle.strokeColor(0);
        myLocationStyle.radiusFillColor(0);
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        //设置默认定位按钮是否显示
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        //开启室内地图
        aMap.showIndoorMap(true);

        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.setMyLocationEnabled(true);

        //地图点击监听
        aMap.setOnMapClickListener(this);

        //构造 GeocodeSearch 对象
        try {
            geocodeSearch = new GeocodeSearch(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        //设置监听
        geocodeSearch.setOnGeocodeSearchListener(this);
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //地址
                String address = aMapLocation.getAddress();
                //设置当前所在地
                etStartAddress.setText(address);
                etStartAddress.setEnabled(false);//禁用输入

                //获取城市
                city = aMapLocation.getCity();

                //获取纬度
                double latitude = aMapLocation.getLatitude();
                //获取经度
                double longitude = aMapLocation.getLongitude();

                //设置起点
                mStartPoint = convertToLatLonPoint(new LatLng(latitude, longitude));

                //停止定位后，本地定位服务并不会被销毁
                mLocationClient.stopLocation();

                //显示地图定位结果
                if (mListener != null) {
                    // 显示系统图标
                    mListener.onLocationChanged(aMapLocation);
                }

            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁定位客户端，同时销毁本地定位服务。
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
    }

    /**
     * 点击地图
     */
    @Override
    public void onMapClick(LatLng latLng) {
        //终点
        mEndPoint = convertToLatLonPoint(latLng);

        //开始路线搜索
        startRouteSearch();
    }

    /**
     * 开始路线搜索
     */
    private void startRouteSearch() {
        //在地图上添加起点Marker
        aMap.addMarker(new MarkerOptions().position(convertToLatLng(mStartPoint)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
        //在地图上添加终点Marker
        aMap.addMarker(new MarkerOptions().position(convertToLatLng(mEndPoint)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));

        //搜索路径  构建路径的起终点
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(mStartPoint, mEndPoint);

        //出行方式判断
        switch (TRAVEL_MODE) {
            case 0://步行（已过时，步行不再提供模式相关设置）
                //构建步行路线搜索对象
                //WALK_DEFAULT——路径为步行模式。只提供一条步行方案
                RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WALK_DEFAULT);
                //异步路径规划步行模式查询
                routeSearch.calculateWalkRouteAsyn(query);
                break;
            case 1://骑行（已过时，骑行不再提供模式相关设置）
                //构建骑行路线搜索对象
                //RIDING_DEFAULT——骑行推荐路线及最快路线综合
                RouteSearch.RideRouteQuery rideQuery = new RouteSearch.RideRouteQuery(fromAndTo, RouteSearch.RIDING_DEFAULT);
                //骑行规划路径计算
                routeSearch.calculateRideRouteAsyn(rideQuery);
                break;
            case 2://驾车
                //构建驾车路线搜索对象  剩余三个参数分别是：途经点、避让区域、避让道路
                //DRIVING_SINGLE_DEFAULT——速度优先
                //DRIVING_SINGLE_SHORTEST——距离优先
                //DRIVING_SINGLE_AVOID_CONGESTION——避免拥堵
                //DRIVING_MULTI_STRATEGY_FASTEST_SAVE_MONEY_SHORTEST——同时使用速度优先、费用优先、距离优先三个策略计算路径
                RouteSearch.DriveRouteQuery driveRouteQuery = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DRIVING_SINGLE_DEFAULT, null, null, "");
                //驾车规划路径计算
                routeSearch.calculateDriveRouteAsyn(driveRouteQuery);
                break;
            case 3://公交
                //构建驾车路线搜索对象 第三个参数表示公交查询城市区号，第四个参数表示是否计算夜班车，0表示不计算,1表示计算
                //BUS_DEFAULT——最快捷模式     BUS_LEASE_CHANGE——最少换乘
                //BUS_LEASE_WALK——最少步行    BUS_SAVE_MONEY——最经济模式
                RouteSearch.BusRouteQuery busRouteQuery = new RouteSearch.BusRouteQuery(fromAndTo, RouteSearch.BUS_LEASE_WALK, city, 0);//公交规划路径计算
                routeSearch.calculateBusRouteAsyn(busRouteQuery);
                break;
            default:
                break;
        }

    }

    /*
     * 公交规划路径结果
     *
     * @param busRouteResult   结果
     * @param code             结果码
     */
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int code) {
        //清理地图上的所有覆盖物
        aMap.clear();
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (busRouteResult != null && busRouteResult.getPaths() != null) {
                if (busRouteResult.getPaths().size() > 0) {
                    final BusPath busPath=busRouteResult.getPaths().get(0) ;
                    if (busPath == null) {
                        return;
                    }
                    //绘制路线
                    BusRouteOverlay busRouteOverlay = new BusRouteOverlay(this,aMap,busPath,busRouteResult.getStartPos(),busRouteResult.getTargetPos());
                    busRouteOverlay.removeFromMap();
                    busRouteOverlay.addToMap();
                    busRouteOverlay.zoomToSpan();

                    int dis = (int) busPath.getDistance();
                    int dur = (int) busPath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur) + "(" + AMapUtil.getFriendlyLength(dis) + ")";

                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent=new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type",3);
                            intent.putExtra("path",busPath);
                            startActivity(intent);
                        }
                    });
                } else if (busRouteResult.getPaths() == null) {
                    ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
                }
            } else {
                ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtils.showerror(this.getApplicationContext(), code);
        }
    }

    /**
     * 驾车规划路径结果
     *
     * @param driveRouteResult 结果
     * @param code             结果码
     */
    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int code) {
        //清理地图上的所有覆盖物
        aMap.clear();
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                if (driveRouteResult.getPaths().size() > 0) {
                    final DrivePath drivePath = driveRouteResult.getPaths().get(0);
                    if (drivePath == null) {
                        return;
                    }
                    //绘制路线
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(this, aMap, drivePath, driveRouteResult.getStartPos(), driveRouteResult.getTargetPos(), null);
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();

                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur) + "(" + AMapUtil.getFriendlyLength(dis) + ")";

                    //显示驾车花费时间
                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    //跳转到路线详情页面
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 2);
                            intent.putExtra("path", drivePath);
                            startActivity(intent);
                        }
                    });
                } else if (driveRouteResult.getPaths() == null) {
                    ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
                }
            } else {
                ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtils.showerror(this.getApplicationContext(), code);
        }
    }

    /**
     * 步行规划路径结果
     *
     * @param walkRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int code) {
        //清理地图上的所有覆盖物
        aMap.clear();
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (walkRouteResult != null && walkRouteResult.getPaths() != null) {
                if (walkRouteResult.getPaths().size() > 0) {
                    final WalkPath walkPath = walkRouteResult.getPaths().get(0);
                    if (walkPath == null) {
                        return;
                    }
                    //绘制路线
                    WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(this, aMap, walkPath, walkRouteResult.getStartPos(), walkRouteResult.getTargetPos());
                    walkRouteOverlay.removeFromMap();
                    walkRouteOverlay.addToMap();
                    walkRouteOverlay.zoomToSpan();

                    int dis = (int) walkPath.getDistance();
                    int dur = (int) walkPath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur) + "(" + AMapUtil.getFriendlyLength(dis) + ")";

                    //显示步行花费时间
                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    //跳转到路线详情页面
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 0);
                            intent.putExtra("path", walkPath);
                            startActivity(intent);
                        }
                    });
                } else if (walkRouteResult.getPaths() == null) {
                    ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
                }
            } else {
                ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtils.showerror(this.getApplicationContext(), code);
        }
    }

    /**
     * 骑行规划路径结果
     *
     * @param rideRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int code) {
        //清理地图上的所有覆盖物
        aMap.clear();
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (rideRouteResult != null && rideRouteResult.getPaths() != null) {
                if (rideRouteResult.getPaths().size() > 0) {
                    final RidePath ridePath = rideRouteResult.getPaths().get(0);
                    if (ridePath == null) {
                        return;
                    }
                    RideRouteOverlay rideRouteOverlay = new RideRouteOverlay(this, aMap, ridePath,
                            rideRouteResult.getStartPos(), rideRouteResult.getTargetPos());
                    rideRouteOverlay.removeFromMap();
                    rideRouteOverlay.addToMap();
                    rideRouteOverlay.zoomToSpan();

                    int dis = (int) ridePath.getDistance();
                    int dur = (int) ridePath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur) + "(" + AMapUtil.getFriendlyLength(dis) + ")";

                    //显示骑行花费时间
                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    //跳转到路线详情页面
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 1);
                            intent.putExtra("path", ridePath);
                            startActivity(intent);
                        }
                    });
                } else if (rideRouteResult.getPaths() == null) {
                    ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
                }
            } else {
                ToastUtils.showToastSafe(this, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtils.showerror(this.getApplicationContext(), code);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
            //获取输入框的值
            String endAddress = etEndAddress.getText().toString().trim();
            if (endAddress.isEmpty()) {
                ToastUtils.showToastSafe(this,"请输入要前往的目的地");
            } else {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //隐藏软键盘
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                //通过输入的目的地转为经纬度，然后进行地图上添加标点，最后计算出行路线规划

                // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
                GeocodeQuery query = new GeocodeQuery(endAddress, city);
                geocodeSearch.getFromLocationNameAsyn(query);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    /**
     * 地址转坐标
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
        if (rCode == PARSE_SUCCESS_CODE) {
            List<GeocodeAddress> geocodeAddressList = geocodeResult.getGeocodeAddressList();
            if (geocodeAddressList != null && geocodeAddressList.size() > 0) {
                //终点
                mEndPoint = geocodeAddressList.get(0).getLatLonPoint();
                //开始路线搜索
                startRouteSearch();
            }

        } else {
            ToastUtils.showToastSafe(this,"获取坐标失败");
        }
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {

    }
}
