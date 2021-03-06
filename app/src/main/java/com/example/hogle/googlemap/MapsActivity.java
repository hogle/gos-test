package com.example.hogle.googlemap;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GPSInfo gpsInfo;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private Polyline polyline;

    private Circle mCircle;
    private Marker mMarker;


    EditText location_tf = null;
    Button btn_location = null;
    Button btn_setting = null;

    EditText et_lat_current, et_lng_current, et_lat_goal, et_lng_goal;

    TextView tv_address, tv_dis;

    public double currentLat = 0;
    public double currentLng = 0;

    double goalLat = 0;
    double goalLng = 0;

    int currentDistance = -1;

    // RadioGroup
    private RadioGroup rg_dis_group;
    private RadioButton rb_dis;

    private boolean start_check = false;
    private boolean end_check = false;

    // 출퇴근 처리
    private Button btn_check_start;
    private Button btn_check_end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        initLayout();

        // 요청 위치 업데이트
        try {

            // GPS Class 인스턴스 생성
            gpsInfo = new GPSInfo(this);

            if(gpsInfo.isGetLocation()){

                currentLat = gpsInfo.getLatitude();
                currentLng = gpsInfo.getLongitude();

                et_lat_current.setText(currentLat + "");
                et_lng_current.setText(currentLng + "");

                LatLng latLng = new LatLng(currentLat, currentLng);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                mMap.animateCamera(cameraUpdate);

            } else {
                gpsInfo.showSettingsAlert();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initLayout() {

        // 두번째 행 정보 초기화
        et_lat_current = (EditText) findViewById(R.id.et_lat_current);
        et_lng_current = (EditText) findViewById(R.id.et_lng_current);
        et_lat_goal = (EditText) findViewById(R.id.et_lat_goal);
        et_lng_goal = (EditText) findViewById(R.id.et_lng_goal);

        // 첫번째 행 정보 초기화
        location_tf = (EditText) findViewById(R.id.et_location);
        location_tf.setOnKeyListener(new View.OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER))
                {
                    // Perform action on Enter key press
                    onSearch(v);
                    return true;
                }
                return false;
            }
        });

        // 설정 버튼
        btn_setting = (Button) findViewById(R.id.btn_one);
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goGpsSetting();
            }
        });

        // 검색 버튼
        btn_location = (Button) findViewById(R.id.btn_two);
        btn_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearch(view);
            }
        });

        tv_address = (TextView) findViewById(R.id.tv_address);
        tv_dis = (TextView) findViewById(R.id.tv_dis);


        // 반경 출퇴근 버튼
        rg_dis_group = (RadioGroup) findViewById(R.id.rg_dis_group);
        rg_dis_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                locationDistance();
                radiusDistanceDisplay();
            }
        });

        // 출근 버튼
        btn_check_start = (Button) findViewById(R.id.btn_check_start);
        btn_check_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getBaseContext(), "출근 처리 되었습니다!", Toast.LENGTH_LONG).show();

                // 출근함
                start_check = true;
                btn_check_start.setEnabled(false);
                btn_check_end.setVisibility(View.VISIBLE);

                locationDistance();
            }
        });

        // 퇴근 버튼
        btn_check_end = (Button) findViewById(R.id.btn_check_end);
        btn_check_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getBaseContext(), "퇴근 처리 되었습니다! 수고하셨습니다!", Toast.LENGTH_LONG).show();
                // 퇴근함

                end_check = true;
                btn_check_end.setEnabled(false);

            }
        });



    }

    // 반경 설정
    public int radiusDistanceSetting() {

            int selectedId = rg_dis_group.getCheckedRadioButtonId();
            rb_dis = (RadioButton) findViewById(selectedId);

            String dis_meter = rb_dis.getText().toString();

            String number =  dis_meter.replaceAll("[^0-9]", "");

            if(number != ""){
                return Integer.valueOf(number);
            }else {
                return 999999;
            }
    }

    public void radiusDistanceDisplay() {
        LatLng goalLatLng = new LatLng(goalLat, goalLng);
        if(mCircle == null || mMarker == null){
            drawMarkerWithCircle(goalLatLng);
        }else{
            updateMarkerWithCircle(goalLatLng);
        }
    }

    // 거리 측정
    public void locationDistance() {

        et_lat_current.setText(currentLat+"");
        et_lng_current.setText(currentLng+"");

        if( currentLat != 0 && currentLng != 0 && tv_address.getText().toString() != "") {

            float[] distance = new float[3];

            Location.distanceBetween(currentLat, currentLng, goalLat, goalLng, distance);

            currentDistance = (int)distance[0];

            Integer dis_c = (int) distance[0] - radiusDistanceSetting();

            if(dis_c <= 0) {
                dis_c = 0;
            }

            String display_dis = "";


            // km 표시
            if( currentDistance >= 1000 ) {
                Integer dis_km = currentDistance - (((int) distance[0] / 1000) * 1000);
                display_dis = (int) distance[0] / 1000 + "." + dis_km + "km";
            } else {
                // m 표시
                display_dis = currentDistance + "m";
            }

            tv_dis.setText(display_dis + " (반경안까지 " + dis_c + "m 남음)" );

            locationCheck();

            if(polyline != null){
                polyline.remove();
            }

            PolylineOptions polylineOptions = new PolylineOptions();
            ArrayList<LatLng> arrayPoints = new ArrayList<>();


            polylineOptions.geodesic(true);
            arrayPoints.add(new LatLng(currentLat, currentLng));
            arrayPoints.add(new LatLng(goalLat, goalLng));

            polylineOptions.addAll(arrayPoints);
            polyline = mMap.addPolyline(polylineOptions);

        }
    }

    public void locationCheck(){

        Integer radiusDisSet = radiusDistanceSetting();

        if( currentDistance != -1 ) {
            if(currentDistance <= radiusDisSet){

                tv_dis.setTextColor(Color.BLACK);

                if( start_check == false ) {
                    btn_check_start.setEnabled(true);
                }

                if( start_check == true && end_check == false) {
                    btn_check_end.setEnabled(true);
                }

            }else {

                tv_dis.setTextColor(Color.RED);

                btn_check_start.setEnabled(false);
                btn_check_end.setEnabled(false);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        try{
            gpsInfo.getLocation();
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onSearch(View v) {

        initText();

        String location = location_tf.getText().toString();
        List<Address> addressList = null;

        try {

            if (location != null || !location.equals("")) {
                Geocoder geocoder = new Geocoder(this);

                try {
                    addressList = geocoder.getFromLocationName(location, 1);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Address address = addressList.get(0);

                goalLat = address.getLatitude();
                goalLng = address.getLongitude();

                et_lat_goal.setText(goalLat + "");
                et_lng_goal.setText(goalLng + "");

                tv_address.setText(address.getAddressLine(0));

                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                //mMap.addMarker(new MarkerOptions().flat(false).position(latLng).title("도착지"));
                // title 표시
/*                Marker melbourne = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(address.getAddressLine(0)));
                melbourne.showInfoWindow();*/

                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                // 거리 표시
                locationDistance();

                // 반경 화면 표시
                radiusDistanceDisplay();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TextView init
    public void initText() {
        et_lat_goal.setText("");
        et_lng_goal.setText("");
        tv_address.setText("");
        tv_dis.setText("");
    }

    // GPS 환경 설정으로 이동
    public void goGpsSetting() {
        Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionsIntent);
    }


    private void setUpMapIfNeeded() {

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            // Zoom Option Enable
            mMap.getUiSettings().setZoomControlsEnabled(true);

            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                new LatLng(41.889, -87.622), 16));
//
//        // You can customize the marker image using images bundled with
//        // your app, or dynamically generated bitmaps.
//        mMap.addMarker(new MarkerOptions()
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.flag))
//                .anchor(0.0f, 1.0f) // Anchors the marker on the bottom left
//                .position(new LatLng(41.889, -87.622)));
    }

    private void updateMarkerWithCircle(LatLng position) {
        mCircle.setRadius((double) radiusDistanceSetting());
        mCircle.setCenter(position);
        mMarker.setPosition(position);
    }

    private void drawMarkerWithCircle(LatLng position){

        double radiusInMeters = (double) radiusDistanceSetting();

        int strokeColor = 0x990000FF; //red outline
        int shadeColor = 0x110000FF; //opaque red fill

        CircleOptions circleOptions = new CircleOptions().center(position).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(3);
        mCircle = mMap.addCircle(circleOptions);

        MarkerOptions markerOptions = new MarkerOptions().position(position);
        //.flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker).).anchor(0.5f, 0.9f);
                //.flat(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)).anchor(0.5f, 0.8f);
        mMarker = mMap.addMarker(markerOptions);
    }
}