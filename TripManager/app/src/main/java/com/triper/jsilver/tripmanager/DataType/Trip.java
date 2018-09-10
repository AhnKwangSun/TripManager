package com.triper.jsilver.tripmanager.DataType;

import com.triper.jsilver.tripmanager.GlobalApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-14.
 */

public class Trip {
    public static final int MILIISEC_OF_DAY = 86400000;
    public static final int MILIISEC_OF_TIMESCALE = 32400000;

    private Group group;
    private ArrayList<Schedule> schedules;
    private ArrayList<Notification> notifications;

    public Trip(Group group) {
        this.group = group;
        schedules = new ArrayList<Schedule>();
        notifications = new ArrayList<Notification>();

        long start = group.getStart_date().getTime() / MILIISEC_OF_DAY;
        long end = group.getEnd_date().getTime() / MILIISEC_OF_DAY;
        long day = end - start;

        for (int i = 0; i < day; i++)
            schedules.add(new Schedule(new Date(group.getStart_date().getTime() + (i * (long)MILIISEC_OF_DAY))));
    }

    public Group getGroup() {
        return group;
    }

    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }

    public ArrayList<Notification> getNotifications() {
        return notifications;
    }

    public void saveScheduleData(File dir, String f_name, JSONArray array, int group_id) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("group_id", group_id);
            obj.put("schedule", array);

            File file = new File(dir, f_name);
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                PrintWriter writer = new PrintWriter(fos);

                JSONArray arr = new JSONArray();
                arr.put(0, obj);

                writer.print(arr.toString());
                writer.close();
            }
            else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                JSONArray arr = new JSONArray(reader.readLine());
                reader.close();

                FileOutputStream fos = new FileOutputStream(file);
                PrintWriter writer = new PrintWriter(fos);

                boolean isExist = false;
                int index = 0;
                for(; index < arr.length(); index++) {
                    if(arr.getJSONObject(index).getInt("group_id") == group_id) {
                        isExist = true;
                        break;
                    }
                }

                if(isExist)
                    arr.put(index, obj);
                else
                    arr.put(arr.length(), obj);

                writer.print(arr.toString());
                writer.close();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadScheduleData(File dir, String f_name, int group_id) {
        try {
            File file = new File(dir, f_name);
            if (!file.exists())
                return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            JSONArray array = new JSONArray(reader.readLine());
            reader.close();
            for(int i = 0; i < array.length(); i++) {
                if(array.getJSONObject(i).getInt("group_id") == group_id) {
                    loadSchedule(array.getJSONObject(i).getJSONArray("schedule"));
                    break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSchedule(JSONArray array) {
        for (int i = 0; i < schedules.size(); i++)
            schedules.get(i).getHourlySchedules().clear();

        try {
            long start = group.getStart_date().getTime();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int id = obj.getInt("id");
                Date date = new SimpleDateFormat("yy-MM-dd hh:mm").parse(obj.getString("date"));
                String content = obj.getString("content");

                int index = (int)(date.getTime() - start) / MILIISEC_OF_DAY;
                if(index >= 0 && index < schedules.size()) {
                    Schedule schedule = schedules.get(index);
                    schedule.createHourlySchedule(id, date, content);
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

    public void loadNotification(JSONArray array) {
        notifications.clear();

        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                int id = obj.getInt("id");
                Date date = new SimpleDateFormat("yy-MM-dd HH:mm").parse(obj.getString("date"));
                String content = obj.getString("content");
                /* 여기 코드 조금만 수정할 수 있으면 하기 */
                Member writer;
                if(!obj.isNull("kakao_id"))
                    writer = new Member(obj.getLong("kakao_id"), obj.getString("name"), GlobalApplication.getInstance().getBitmapFromString(obj.getString("member_picture")));
                else
                    writer = new Member(new Long(-1), "알수없음", null);

                notifications.add(new Notification(id, date, content, writer));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
