package com.triper.jsilver.tripmanager.Trip;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.labo.kaji.fragmentanimations.MoveAnimation;
import com.triper.jsilver.tripmanager.DataType.Member;
import com.triper.jsilver.tripmanager.DataType.Trip;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by JSilver on 2017-10-03.
 */

public class TripFollowerFragment extends Fragment {
    private TripActivity parent;

    private ListView list_follower;
    private FollowerAdapter adapter;

    private EditText edit_name;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (TripActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_follower, container, false);

        list_follower = (ListView)view.findViewById(R.id.list_follower);
        adapter = new FollowerAdapter(parent, parent.getTrip().getGroup());
        list_follower.setAdapter(adapter);

        edit_name = (EditText) view.findViewById(R.id.edit_name);

        Button btn_find = (Button) view.findViewById(R.id.btn_find);
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestFollowerFind();
            }
        });

        requestFollowerDisplay();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        edit_name.setText("");
    }

    private boolean validateInput() {
        if (edit_name.getText().toString().length() == 0) {
            Toast.makeText(parent, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void requestFollowerFind() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_name.getWindowToken(), 0);

        if(!validateInput())
            return;

        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "find");

        JSONObject data = new JSONObject();
        try {
            data.put("id", parent.getTrip().getGroup().getId());
            data.put("name", edit_name.getText().toString());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    private void requestFollowerDisplay() {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "display");

        JSONObject data = new JSONObject();
        try {
            data.put("id", parent.getTrip().getGroup().getId());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    public void onFindReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONArray array = new JSONArray(data);

            adapter.clear();
            for(int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                adapter.add(new Member(obj.getLong("kakao_id"), obj.getString("name"), GlobalApplication.getInstance().getBitmapFromString(obj.getString("member_picture")), obj.getString("phone")));
            }
            adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onDisplayReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);

            /* 권한 정보 갱신 */
            JSONArray editors = json.getJSONArray("editors");
            parent.getTrip().getGroup().updateEditors(editors);

            JSONArray members = json.getJSONArray("members");
            adapter.clear();
            for(int i = 0; i < members.length(); i++) {
                JSONObject obj = members.getJSONObject(i);
                adapter.add(new Member(obj.getLong("kakao_id"), obj.getString("name"), GlobalApplication.getInstance().getBitmapFromString(obj.getString("member_picture")), obj.getString("phone")));
            }
            adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onAccessibleReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (!result)
                Toast.makeText(parent, "CODE: " + json.getInt("code"), Toast.LENGTH_SHORT).show();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
