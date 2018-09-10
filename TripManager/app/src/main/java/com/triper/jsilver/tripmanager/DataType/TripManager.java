package com.triper.jsilver.tripmanager.DataType;

import android.graphics.Bitmap;

import com.triper.jsilver.tripmanager.GlobalApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-14.
 */

public class TripManager {
    private Member user;
    private ArrayList<Trip> trips;

    public TripManager() {
        trips = new ArrayList<>();
    }

    public Member getUser() {
        return user;
    }

    public ArrayList<Trip> getTrips() {
        return trips;
    }

    public void setUser(Member user) {
        this.user = user;
    }

    public void saveTripData(File dir, String f_name, JSONArray array) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(dir, f_name));
            PrintWriter writer = new PrintWriter(fos);

            writer.print(array.toString());
            writer.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void loadTripData(File dir, String f_name) {
        try {
            File file = new File(dir, f_name);
            if (!file.exists())
                return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            JSONArray array = new JSONArray(reader.readLine());
            reader.close();

            loadTrip(array);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadTrip(JSONArray array) {
        trips.clear();

        try {
            for(int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int id = obj.getInt("id");
                String name = obj.getString("name");
                Bitmap group_picture = GlobalApplication.getInstance().getBitmapFromString(obj.getString("group_picture"));
                Date start_date = new SimpleDateFormat("yyyy-MM-dd").parse(obj.getString("start_date"));
                Date end_date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(obj.getString("end_date"));
                Long leader = obj.getLong("leader");

                ArrayList<Long> members = new ArrayList<>();
                String[] memberList = obj.getString("members").split(",");
                for(int j = 0; j < memberList.length; j++)
                    members.add(Long.parseLong(memberList[j]));

                int radius = obj.getInt("radius");
                boolean isTracing = (obj.getInt("isTracing") == 1) ? true : false;

                trips.add(new Trip(new Group(id, name, group_picture, start_date, end_date, leader, members, radius, isTracing)));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void saveUserData(File dir, String f_name) {
        /* 내부 저장소에 프로필 정보 저장 */
        try {
            FileOutputStream fos = new FileOutputStream(new File(dir, f_name));
            PrintWriter writer = new PrintWriter(fos);

            writer.print(user.toJSONObject().toString());
            writer.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean loadUserData(File dir, String f_name) {
        try {
            File file = new File(dir, f_name);
            if (!file.exists())
                return false;

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            JSONObject data = new JSONObject(reader.readLine());
            Long kakao_id = data.getLong("kakao_id");
            String name = data.getString("name");
            Bitmap member_picture = GlobalApplication.getInstance().getBitmapFromString(data.getString("member_picture"));
            String phone = data.getString("phone");
            String fcm_token = data.getString("fcm_token");

            setUser(new Member(kakao_id, name, member_picture, phone, fcm_token));

            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
