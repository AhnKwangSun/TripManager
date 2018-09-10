package com.triper.jsilver.tripmanager.fcm;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JSilver on 2017-09-05.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    /**
     * FCM 서버로부터 기기 고유 토큰값을 받아옴.
     */
    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();
    }
}
