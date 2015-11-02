package com.example.hogle.googlemap;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    LocationManager locationManager = null;

    LocationListener locationListener = null;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    EditText location_tf = null;
    Button btn_location = null;
    Button btn_setting = null;

    EditText et_lat, et_lng;

    TextView tv_address, tv_dis;

    double currentLat = 0;
    double currentLng = 0;

    double goalLat = 0;
    double goalLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        initLayout();


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    et_lat.setText(location.getLatitude() + "");
                    et_lng.setText(location.getLongitude() + "");

                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();

                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        // 요청 위치 업데이트
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void initLayout() {

        // 두번째 행 정보 초기화
        et_lat = (EditText) findViewById(R.id.et_lat);
        et_lng = (EditText) findViewById(R.id.et_lng);


        // 첫번째 행 정보 초기화
        location_tf = (EditText) findViewById(R.id.et_location);
        location_tf.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {

                if (keyEvent.getAction() == keyEvent.KEYCODE_ENTER) {
                    onSearch(view);
                }

                return false;
            }
        });

        btn_setting = (Button) findViewById(R.id.btn_one);
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goGspSetting();
            }
        });

        btn_location = (Button) findViewById(R.id.btn_two);
        btn_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearch(view);
            }
        });

        tv_address = (TextView) findViewById(R.id.tv_address);
        tv_dis = (TextView) findViewById(R.id.tv_dis);
    }

    private void locationDistance() {

        float[] distance = new float[3];

        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 시스템에서 제공하는 위치 정보 서비스를 받아와서 그를 LocationManager로 캐스팅
        GPSProvider gps = new GPSProvider(mlocManager); //오브젝트 생성

//        double currentLat = gps.getLongitude(); //이건 아시죠?^^
//        double currentLng = gps.getLatitude();

        Location.distanceBetween(currentLat, currentLng, goalLat, goalLng, distance);

        tv_dis.setText("거리 : " + distance[0]/1000 + "km");

    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        try{
            String locProv = locationManager.getBestProvider(getCriteria(), true);
            locationManager.requestLocationUpdates(locProv, 3000, 3, locationListener);
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        return criteria;
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

                    Log.i("구글맵", addressList.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Address address = addressList.get(0);

                goalLat = address.getLatitude();
                goalLng = address.getLongitude();

                et_lat.setText(goalLat + "");
                et_lng.setText(goalLng + "");

                tv_address.setText(address.getAddressLine(0));

                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                locationDistance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initText() {
        et_lat.setText("");
        et_lng.setText("");
        tv_address.setText("");
    }

    public void onZoom(View v) {

    }

    public void goGspSetting() {
        Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionsIntent);
    }


    private void setUpMapIfNeeded() {

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        mMap.setMyLocationEnabled(true);
        /*LocationServices mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        }*/
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(41.889, -87.622), 16));

        // You can customize the marker image using images bundled with
        // your app, or dynamically generated bitmaps.
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.flag))
                .anchor(0.0f, 1.0f) // Anchors the marker on the bottom left
                .position(new LatLng(41.889, -87.622)));
    }
}
