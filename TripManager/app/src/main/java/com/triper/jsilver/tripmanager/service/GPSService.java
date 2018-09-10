package com.triper.jsilver.tripmanager.service;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.kakao.usermgmt.response.model.UserProfile;
import com.triper.jsilver.tripmanager.GlobalApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by JSilver on 2017-11-03.
 */

public class GPSService extends Service {
    public static final double EARTH_RADIUS = 6371000.0;
    public static final double RADIUS = Math.PI / 180;

    private final int MIN_GPS_INTERVAL = 1 * 60 * 1000;
    private final int MIN_GPS_DISTANCE = 10;

    private ArrayList<LatLng> current;
    private int index;

    private LocationListener locationListener;

    private BroadcastReceiver receiver;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        current = new ArrayList<>();
        index = 0;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(current.size() == 3) {
                    current.set(index, new LatLng(location.getLatitude(), location.getLongitude()));
                    index = (index + 1) % 3;
                }
                else
                    current.add(new LatLng(location.getLatitude(), location.getLongitude()));

                requestFollowerTrace(location.getLatitude(), location.getLongitude());
                requestFollowerGetLatlng();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        /* 통신 리시버 등록 */
        IntentFilter filter = new IntentFilter("SocketIOService");
        receiver = new GPSSocketIOReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getIntExtra("boost", 0) == 1)
            startLocationService(3000, 0);
        else
            startLocationService(MIN_GPS_INTERVAL, MIN_GPS_DISTANCE);

        return super.onStartCommand(intent, flags, startId);
    }

    private void startLocationService(int interval, int distance) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        List<String> m_lstProviders = locationManager.getProviders(false);

        locationManager.removeUpdates(locationListener);
        for (String name : m_lstProviders) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            locationManager.requestLocationUpdates(name, interval, distance, locationListener);
        }
    }

    private Long loadKakao_id() {
        Long kakao_id = Long.valueOf(-1);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getFilesDir(), "user.dat"))));

            JSONObject data = new JSONObject(reader.readLine());
            kakao_id = data.getLong("kakao_id");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return kakao_id;
    }

    public static double calcDistance(double baseLat, double baseLng, double targetLat, double targetLng) {
        double radLat1 = GPSService.RADIUS * baseLat;
        double radLat2 = GPSService.RADIUS * targetLat;
        double radDist = GPSService.RADIUS * (baseLng - targetLng);

        double distance =  (Math.sin(radLat1) * Math.sin(radLat2)) + (Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radDist));
        return GPSService.EARTH_RADIUS * Math.acos(distance);
    }

    private void requestFollowerTrace(double latitude, double longitude) {
        Intent service = new Intent(getApplicationContext(), SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "trace");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", UserProfile.loadFromCache().getId());
            data.put("latitude", latitude);
            data.put("longitude", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        startService(service);
    }

    private void requestFollowerGetLatlng() {
        Intent service = new Intent(getApplicationContext(), SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "getlatlng");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", UserProfile.loadFromCache().getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        startService(service);
    }

    private void requestFollowerAlram(int group_id) {
        Intent service = new Intent(getApplicationContext(), SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "alram");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", UserProfile.loadFromCache().getId());
            data.put("group_id", group_id);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        startService(service);
    }

    private void onGetLatlngReceived(String data) {
        try {
            JSONArray array = new JSONArray(data);

            GlobalApplication application = GlobalApplication.getInstance();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            for(int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                if (application.checkIsTracing(format.parse(obj.getString("start_date")), format.parse(obj.getString("end_date"))) && obj.getInt("isTracing") == 1) {
                    boolean isOutOfRange = true;
                    for(LatLng latLng : current) {
                        double distance = calcDistance(obj.getDouble("latitude"), obj.getDouble("longitude"), latLng.latitude, latLng.longitude);
                        if (distance < obj.getInt("radius"))
                            isOutOfRange = false;
                    }

                    if(isOutOfRange)
                        requestFollowerAlram(obj.getInt("id"));
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public class GPSSocketIOReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int event_type = intent.getIntExtra(SocketIOService.EXTRA_EVENT_TYPE, -1);
            if (event_type == -1)
                return;

            /* 그룹 액티비티에서는 통신에러, 회원, 그룹관련 이벤트만 처리 */
            switch (event_type) {
                case SocketIOService.EVENT_TYPE_ERROR:
                    onErrorReceived(intent);
                    break;
                case SocketIOService.EVENT_TYPE_FOLLOWER:
                    onFollowerReceived(intent);
                    break;
            }
        }

        private void onErrorReceived(Intent intent) {
            try {
                JSONObject data = new JSONObject(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                GlobalApplication.getInstance().progressOff();

                switch (data.getInt("code")) {
                    case 0:
                        Toast.makeText(getApplicationContext(), "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(getApplicationContext(), "서버가 응답하지 않습니다.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void onFollowerReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);
            switch (sub_event) {
                case "getlatlng":
                    onGetLatlngReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }
    }
}
