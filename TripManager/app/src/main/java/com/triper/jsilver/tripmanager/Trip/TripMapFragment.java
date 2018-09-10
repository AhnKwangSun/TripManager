package com.triper.jsilver.tripmanager.Trip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kakao.usermgmt.response.model.UserProfile;
import com.triper.jsilver.tripmanager.DataType.Trip;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.GPSService;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Laser on 2017-10-10.
 */

public class TripMapFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final int LOCATION_LEADER = 1;
    private final int LOCATION_FOLLOWER = 2;

    private TripActivity parent;
    private Trip trip;

    private GoogleMap map;
    private GoogleApiClient googleApiClient;

    private Location location;
    private Marker current;
    private Circle radius;

    private boolean isTracing;
    private ArrayList<Marker> followers;

    private Timer timer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (TripActivity) getActivity();
        trip = GlobalApplication.getInstance().getTripManager().getTrips().get(parent.getIntent().getIntExtra("index", -1));

        isTracing = GlobalApplication.getInstance().checkIsTracing(trip.getGroup().getStart_date(), trip.getGroup().getEnd_date());
        followers = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        current = null;
        if(map != null)
            map.clear();

        if (isTracing) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    requestFollowerLocation(parent.isLeader() ? LOCATION_LEADER : LOCATION_FOLLOWER);
                }
            }, 0, 5000);
            requestFollowerGPSBoost(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isTracing) {
            timer.cancel();
            requestFollowerGPSBoost(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.clear();

        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        if (ActivityCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.setMyLocationEnabled(true);

        if (googleApiClient == null)
            buildGoogleApiClient();
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);

        if (ActivityCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                requestFollowerTrace(location.getLatitude(), location.getLongitude());
                updateCurrentLocation(location);
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void updateCurrentLocation(Location location) {
        this.location = location;
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

        if (current == null)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        else
            current.remove();

        if(radius != null)
            radius.remove();

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .draggable(true)
                .visible(false);

        CircleOptions circleOptions = new CircleOptions()
                .center(position)
                .radius(trip.getGroup().getRadius())
                .strokeWidth(0f)
                .fillColor(Color.parseColor("#556E7783"));

        current = map.addMarker(markerOptions);

        if (parent.isLeader())
            radius = map.addCircle(circleOptions);
    }

    private void updateMemberLoaction(JSONArray array) {
        if(!isAdded())
            return;

        for(Marker marker : followers)
            marker.remove();
        followers.clear();

        Bitmap marker_in = resizeBitmap("icon_marker_in", 100, 100);
        Bitmap marker_out = resizeBitmap("icon_marker_out", 100, 100);

        try {
            for(int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if(parent.isLeader() && obj.getLong("kakao_id") == trip.getGroup().getLeader())
                    continue;

                LatLng position = new LatLng(obj.getDouble("latitude"), obj.getDouble("longitude"));

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .draggable(true)
                        .title(obj.getString("name"))
                        .snippet(obj.getString("phone"))
                        .icon(BitmapDescriptorFactory.fromBitmap(marker_in));

                double distance = GPSService.calcDistance(location.getLatitude(), location.getLongitude(), position.latitude, position.longitude);
                if(parent.isLeader() && distance > trip.getGroup().getRadius())
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(marker_out));

                followers.add(map.addMarker(markerOptions));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap resizeBitmap(String resource, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(resource, "drawable", parent.getPackageName()));
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(getActivity(), this)
                .build();
        googleApiClient.connect();
    }

    private void requestFollowerTrace(double latitude, double longitude) {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "trace");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
            data.put("latitude", latitude);
            data.put("longitude", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);
    }

    private void requestFollowerLocation(int flag) {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "location");

        JSONObject data = new JSONObject();
        try {
            data.put("group_id", parent.getTrip().getGroup().getId());
            data.put("flag", flag);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);
    }

    private void requestFollowerGPSBoost(boolean boost) {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "gpsboost");

        JSONObject data = new JSONObject();
        try {
            data.put("member_id", trip.getGroup().getLeader());
            data.put("group_id", parent.getTrip().getGroup().getId());
            data.put("boost", boost);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);
    }

    public void onLocationReceived(String data) {
        try {
            if(location == null)
                return;

            JSONArray array = new JSONArray(data);
            updateMemberLoaction(array);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
