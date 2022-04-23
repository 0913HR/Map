package com.zzc.mapsassistant.listen;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Location;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.maps.model.Text;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.zzc.mapsassistant.R;
import com.zzc.mapsassistant.overlay.PoiOverlay;
import com.zzc.mapsassistant.utils.Constants;
import com.zzc.mapsassistant.utils.LogUtil;
import com.zzc.mapsassistant.utils.ToastUtils;

import java.util.List;

public class LocationListen implements AMap.OnMapTouchListener, AMap.OnMyLocationChangeListener,
        AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, PoiSearch.OnPoiSearchListener, GeocodeSearch.OnGeocodeSearchListener, AMap.OnPOIClickListener {
    private ProgressDialog progDialog = null;// 搜索时进度条
    private PoiSearch.Query query;// Poi查询条件类
    private LatLng latlng = null;
    private final AMap aMap;
    private boolean bludeMoveState = true;
    private final Activity activity;
    private final GeocodeSearch geocodeSearch;
    private TextView textView;

    public LocationListen(AMap aMap, Activity activity, GeocodeSearch geocodeSearch) {
        this.aMap = aMap;
        this.activity = activity;
        this.geocodeSearch = geocodeSearch;
    }

    public LocationListen(AMap aMap, Activity activity, GeocodeSearch geocodeSearch, TextView textView) {
        this.aMap = aMap;
        this.activity = activity;
        this.geocodeSearch = geocodeSearch;
        this.textView = textView;
    }

    /**
     *  地图触摸事件
     */
    @Override
    public void onTouch(MotionEvent motionEvent) {
        if (bludeMoveState) {
            bludeMoveState = false;
        }
    }

    /**
     * 当用户定位信息改变时回调此方法。
     */
    @Override
    public void onMyLocationChange(Location location) {
        if (bludeMoveState) {
            latlng = new LatLng(location.getLatitude(), location.getLongitude());
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(latlng));
            aMap.moveCamera( CameraUpdateFactory.zoomTo(16));
            LatLonPoint latLonPoint = new LatLonPoint(latlng.latitude, latlng.longitude);
            RegeocodeQuery queryCode = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(queryCode);
        }
    }

    /**
     * 显示小蓝点的位置
     */
    public void location() {
        if (latlng != null){
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(latlng));
        }
    }

    /**
     * 标记点击事件
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    /**
     * 获取窗口信息
     */
    @Override
    public View getInfoWindow(Marker marker) {
        if (marker.getTitle().equals("") || marker.getTitle() == null) return null;
        View view = activity.getLayoutInflater().inflate(R.layout.poikeywordsearch_uri, null);
        TextView title = view.findViewById(R.id.title);
        title.setText(marker.getTitle());
        textView.setText(marker.getTitle());
        TextView snippet = view.findViewById(R.id.snippet);
        snippet.setText(marker.getSnippet());
        return view;
    }

    /**
     * 获取标记文本内容
     */
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 开始进行poi搜索
     */
    public void doSearchQuery(String keywords) throws AMapException {
        //显示进度框
        showProgressDialog();
        int currentPage = 1;
        // 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query = new PoiSearch.Query(keywords, "", Constants.DEFAULT_CITY);
        // 设置每页最多返回多少条poiitem
        query.setPageSize(10);
        // 设置查第一页
        query.setPageNum(currentPage);
        // POI搜索
        PoiSearch poiSearch = new PoiSearch(activity, query);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }

    /**
     * POI信息查询回调方法
     */
    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        //隐藏对话框
        dismissProgressDialog();
        if (rCode == 1000) {
            //搜索poi的结果
            if (result != null && result.getQuery() != null) {
                //是否是同一条
                if (result.getQuery().equals(query)) {
                    // poi返回的结果
                    // 取得搜索到的poiitems有多少页
                    List<PoiItem> poiItems = result.getPois();// 取得第一页的poiitem数据，页数从数字0开始
                    List<SuggestionCity> suggestionCities = result
                            .getSearchSuggestionCitys();// 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息

                    if (poiItems != null && poiItems.size() > 0) {
                        aMap.clear();// 清理之前的图标
                        PoiOverlay poiOverlay = new PoiOverlay(aMap, poiItems);
                        poiOverlay.removeFromMap();
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                    } else if (suggestionCities != null
                            && suggestionCities.size() > 0) {
                        showSuggestCity(suggestionCities);
                    } else {
                        ToastUtils.showToastSafe(activity, "对不起，没有搜索到相关数据！");
                    }
                }
            } else {
                ToastUtils.showToastSafe(activity, "对不起，没有搜索到相关数据！");
            }
        } else {
            ToastUtils.showToastSafe(activity, "对不起，没有搜索到相关数据！");
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    /**
     * poi没有搜索到数据，返回一些推荐城市的信息
     */
    private void showSuggestCity(List<SuggestionCity> cities) {
        String information = "推荐城市\n";
        for (int i = 0; i < cities.size(); i++) {
            information += "城市名称：" + cities.get(i).getCityName()
                    + "城市区号：" + cities.get(i).getCityCode()
                    + "城市编码：" + cities.get(i).getAdCode() + "\n";
        }
        ToastUtils.showToastSafe(activity, information);
    }

    /**
     * 显示进度框
     */
    public void showProgressDialog() {
        if (progDialog == null) progDialog = new ProgressDialog(activity);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        // 要输入的poi搜索关键字
        String mKeyWords = "";
        progDialog.setMessage("正在搜索：\n" + mKeyWords);
        progDialog.show();
    }

    /**
     * 用marker展示输入提示list选中数据
     */
    public void addTipMarker(Tip tip) {
        if (tip == null) {
            return;
        }
        Marker mPoiMarker = aMap.addMarker(new MarkerOptions());
        LatLonPoint point = tip.getPoint();
        if (point != null) {
            LatLng markerPosition = new LatLng(point.getLatitude(), point.getLongitude());
            mPoiMarker.setPosition(markerPosition);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 17));
        }
        mPoiMarker.setTitle(tip.getName());
        mPoiMarker.setSnippet(tip.getAddress());
    }

    /**
     * 隐藏进度框
     */
    public void dismissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }


    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        if (i == AMapException.CODE_AMAP_SUCCESS) {
            // 更改默认城市
            Constants.DEFAULT_CITY = regeocodeResult.getRegeocodeAddress().getCity();
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public void onPOIClick(Poi poi) {
        ToastUtils.showToastSafe(activity, poi.getName());
    }
}
