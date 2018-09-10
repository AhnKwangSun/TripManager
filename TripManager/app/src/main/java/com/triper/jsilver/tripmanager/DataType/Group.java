package com.triper.jsilver.tripmanager.DataType;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-13.
 */

public class Group {
    private int id;
    private String name;
    private Bitmap group_picture;
    private String password;
    private Date start_date;
    private Date end_date;
    private Long leader;
    private ArrayList<Long> members;
    private ArrayList<Long> editors;
    private int radius;
    private boolean isTracing;

    public Group() {
        id = -1;
        name = "";
        group_picture = null;
        password = "";
        start_date = null;
        end_date = null;
    }

    public Group(int id, String name, Bitmap group_picture, Date start_date, Date end_date, Long leader, ArrayList<Long> members, int radius, boolean isTracing) {
        this.id = id;
        this.name = name;
        this.password = "";
        this.group_picture = group_picture;
        this.start_date = start_date;
        this.end_date = end_date;
        this.leader = leader;
        this.members = members;
        this.editors = new ArrayList<>();
        this.radius = radius;
        this.isTracing = isTracing;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setGroup_picture(Bitmap group_picture) { this.group_picture = group_picture; }

    public void setStart_date(Date start_date) {
        this.start_date = start_date;
    }

    public void setEnd_date(Date end_date) {
        this.end_date = end_date;
    }

    public void setLeader(Long leader) {
        this.leader = leader;
    }

    public void setMembers(ArrayList<Long> members) {
        this.members = members;
    }

    public void setEditors(ArrayList<Long> editors) {
        this.editors = editors;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setIsTracing(boolean tracing) {
        isTracing = tracing;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Bitmap getGroup_picture() { return group_picture; }

    public String getPassword() {
        return password;
    }

    public Date getStart_date() {
        return start_date;
    }

    public Date getEnd_date() {
        return end_date;
    }

    public Long getLeader() {
        return leader;
    }

    public ArrayList<Long> getMembers() {
        return members;
    }

    public ArrayList<Long> getEditors() {
        return editors;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isTracing() {
        return isTracing;
    }

    public void updateEditors(JSONArray array) {
        editors.clear();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                editors.add(obj.getLong("member_id"));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Group clone() {
        return new Group(id, name, group_picture, start_date, end_date, leader, members, radius, isTracing);
    }
}
