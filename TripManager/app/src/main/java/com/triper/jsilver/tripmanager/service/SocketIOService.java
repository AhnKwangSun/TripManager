package com.triper.jsilver.tripmanager.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.triper.jsilver.tripmanager.GlobalApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by JSilver on 2017-09-02.
 */

public class SocketIOService extends Service implements SocketIOEventListener.Listener {
    // 서버 주소
    private static final String url = "http://218.233.209.73:8080";

    // 에러 코드
    public static final int ERROR_TYPE_DISCONNECTED = 0;
    public static final int ERROR_TYPE_TIMEOUT = 1;

    // 서버와의 통신 시 이벤트 이름
    private static final String EVENT_MEMBER = "member";
    private static final String EVENT_GROUP = "group";
    private static final String EVENT_SCHEDULE = "schedule";
    private static final String EVENT_NOTIFICATION = "notification";
    private static final String EVENT_FOLLOWER = "follower";
    private static final String EVENT_APPLICATION = "application";

    // 이벤트 타입
    public static final int EVENT_TYPE_ERROR = 0;
    public static final int EVENT_TYPE_MEMBER = 1;
    public static final int EVENT_TYPE_GROUP = 2;
    public static final int EVENT_TYPE_SCHEDULE = 3;
    public static final int EVENT_TYPE_NOTIFICATION = 4;
    public static final int EVENT_TYPE_FOLLOWER = 5;
    public static final int EVENT_TYPE_APPLICATION = 6;

    // Intent 항목 이름
    public static final String EXTRA_EVENT_TYPE = "extra_event_type";
    public static final String EXTRA_SUB_EVENT = "extra_sub_event";
    public static final String EXTRA_DATA = "extra_data";

    private Socket mSocket;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mSocket = IO.socket(url);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        mSocket.on(Socket.EVENT_CONNECT, new SocketIOEventListener(Socket.EVENT_CONNECT, this));
        mSocket.on(Socket.EVENT_CONNECT_ERROR, new SocketIOEventListener(Socket.EVENT_CONNECT_ERROR, this));
        mSocket.on(Socket.EVENT_DISCONNECT, new SocketIOEventListener(Socket.EVENT_DISCONNECT, this));

        // 서버로부터 받을 이벤트를 등록
        mSocket.on(EVENT_MEMBER, new SocketIOEventListener(EVENT_MEMBER, this));
        mSocket.on(EVENT_GROUP, new SocketIOEventListener(EVENT_GROUP, this));
        mSocket.on(EVENT_SCHEDULE, new SocketIOEventListener(EVENT_SCHEDULE, this));
        mSocket.on(EVENT_NOTIFICATION, new SocketIOEventListener(EVENT_NOTIFICATION, this));
        mSocket.on(EVENT_FOLLOWER, new SocketIOEventListener(EVENT_FOLLOWER, this));
        mSocket.on(EVENT_APPLICATION, new SocketIOEventListener(EVENT_APPLICATION, this));

        mSocket.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
            sendMessage(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.off(Socket.EVENT_CONNECT);
        mSocket.off(Socket.EVENT_CONNECT_ERROR);
        mSocket.off(Socket.EVENT_DISCONNECT);

        // 서버로부터 받을 이벤트 제거
        mSocket.off(EVENT_MEMBER);
        mSocket.off(EVENT_GROUP);
        mSocket.off(EVENT_SCHEDULE);
        mSocket.off(EVENT_NOTIFICATION);
        mSocket.off(EVENT_FOLLOWER);
        mSocket.off(EVENT_APPLICATION);

        mSocket.disconnect();
        mSocket.close();
    }

    public void sendMessage(Intent intent) {
        int event_type = intent.getIntExtra(EXTRA_EVENT_TYPE, -1);
        if(event_type == -1)
            return;

        /* 서버가 연결이 안되있으면 disconnect 전송 */
        if(!mSocket.connected()) {
            /*
            Intent service = new Intent("SocketIOService");
            service.putExtra(EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_ERROR);

            JSONObject data = new JSONObject();
            try {
                data.put("code", 0);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            service.putExtra(EXTRA_DATA, data.toString());
            sendBroadcast(service);
            */
            return;
        }

        JSONObject data = new JSONObject();
        try {
            data.put("sub_event", intent.getStringExtra(EXTRA_SUB_EVENT));
            data.put("data", new JSONObject(intent.getStringExtra(EXTRA_DATA)));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        switch (event_type) {
            case EVENT_TYPE_MEMBER:
                mSocket.emit(EVENT_MEMBER, data.toString());
                break;
            case EVENT_TYPE_GROUP:
                mSocket.emit(EVENT_GROUP, data.toString());
                break;
            case EVENT_TYPE_SCHEDULE:
                mSocket.emit(EVENT_SCHEDULE, data.toString());
                break;
            case EVENT_TYPE_NOTIFICATION:
                mSocket.emit(EVENT_NOTIFICATION, data.toString());
                break;
            case EVENT_TYPE_FOLLOWER:
                mSocket.emit(EVENT_FOLLOWER, data.toString());
                break;
            case EVENT_TYPE_APPLICATION:
                mSocket.emit(EVENT_APPLICATION, data.toString());
                break;
        }
    }

    @Override
    public void onEventCall(String event, Object... args) {
        switch (event) {
            case Socket.EVENT_CONNECT:
                Log.e("[SOCKET]", "socket established.");
                break;
            case Socket.EVENT_CONNECT_ERROR:
                Log.e("[SOCKET]", "socket connect error occurred.");
                /*
                Intent service = new Intent("SocketIOService");
                service.putExtra(EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_ERROR);

                JSONObject data = new JSONObject();
                try {
                    data.put("code", 1);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                service.putExtra(EXTRA_DATA, data.toString());
                sendBroadcast(service);
                */
                break;
            case Socket.EVENT_DISCONNECT:
                Log.e("[SOCKET]", "socket disconnected.");
                break;
            default:
                onAppEventCall(event, args);
                break;
        }
    }

    public void onAppEventCall(String event, Object... args) {
        Intent service = new Intent("SocketIOService");

        JSONObject data = (JSONObject) args[0];
        try {
            service.putExtra(EXTRA_SUB_EVENT, data.getString("sub_event"));
            Object obj = data.get("data");
            if(obj instanceof JSONObject)
                service.putExtra(EXTRA_DATA, ((JSONObject)obj).toString());
            else
                service.putExtra(EXTRA_DATA, ((JSONArray)obj).toString());

            switch (event) {
                case EVENT_MEMBER:
                    service.putExtra(EXTRA_EVENT_TYPE, EVENT_TYPE_MEMBER);
                    break;
                case EVENT_GROUP:
                    service.putExtra(EXTRA_EVENT_TYPE, EVENT_TYPE_GROUP);
                    break;
                case EVENT_SCHEDULE:
                    service.putExtra(EXTRA_EVENT_TYPE, EVENT_TYPE_SCHEDULE);
                    break;
                case EVENT_NOTIFICATION:
                    service.putExtra(EXTRA_EVENT_TYPE, EVENT_TYPE_NOTIFICATION);
                    break;
                case EVENT_FOLLOWER:
                    service.putExtra(EXTRA_EVENT_TYPE, EVENT_TYPE_FOLLOWER);
                    break;
                case EVENT_APPLICATION:
                    service.putExtra(EXTRA_EVENT_TYPE, EVENT_TYPE_APPLICATION);
                    break;
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendBroadcast(service);
    }
}
