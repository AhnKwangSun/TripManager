package com.triper.jsilver.tripmanager.DataType;

import android.graphics.Bitmap;

import com.triper.jsilver.tripmanager.GlobalApplication;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JSilver on 2017-09-11.
 */

public class Member {
    private Long kakao_id;
    private String name;
    private Bitmap member_picture;
    private String phone;
    private String fcm_toekn;
    private Location location;

    public Member(Long kakao_id, String name, Bitmap member_picture, String phone, String fcm_toekn) {
        this.kakao_id = kakao_id;
        this.name = name;
        this.member_picture = member_picture;
        this.phone = phone;
        this.fcm_toekn = fcm_toekn;
        this.location = new Location();
    }

    public Member(Long kakao_id, String name, Bitmap member_picture, String phone) {
        this.kakao_id = kakao_id;
        this.name = name;
        this.member_picture = member_picture;
        this.phone = phone;
    }

    public Member(Long kakao_id, String name, Bitmap member_picture) {
        this.kakao_id = kakao_id;
        this.name = name;
        this.member_picture = member_picture;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setMember_picture(Bitmap member_picture) { this.member_picture = member_picture; }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getKakao_id() {
        return kakao_id;
    }
    public String getName() {
        return name;
    }
    public Bitmap getMember_picture() { return member_picture; }
    public String getPhone() {
        return phone;
    }
    public String getFcm_toekn() {
        return fcm_toekn;
    }
    public Location getLocation() {
        return location;
    }

    public JSONObject toJSONObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", kakao_id);
            data.put("name", name);
            // 여기 null이 입력이안됨
            data.put("member_picture", (member_picture == null) ? JSONObject.NULL : GlobalApplication.getInstance().getStringFromBitmap(member_picture));
            data.put("phone", phone);
            data.put("fcm_token", fcm_toekn);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return data;
    }

    public Member clone() {
        return new Member(kakao_id, name, member_picture, phone, fcm_toekn);
    }
}
