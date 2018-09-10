package com.triper.jsilver.tripmanager.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.triper.jsilver.tripmanager.DataType.Trip;
import com.triper.jsilver.tripmanager.DataType.TripManager;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by JSilver on 2017-09-28.
 */

public class GroupJoinFragment extends Fragment {
    private MainActivity parent;

    private ListView list_group;
    private GroupAdapter adapter;

    private EditText edit_name;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_join, container, false);

        list_group = (ListView) view.findViewById(R.id.list_group);
        adapter = new GroupAdapter(R.layout.listviewitem_group_find, null);
        list_group.setAdapter(adapter);

        edit_name = (EditText) view.findViewById(R.id.edit_name);

        list_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                /* ListView에서 Item 선택시 가입 */
                final Trip trip = (Trip) adapterView.getItemAtPosition(i);

                final JoinDialog dialog = new JoinDialog(parent, trip.getGroup().getName());
                dialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String password = dialog.getPassword();
                        dialog.hideKeyboard();
                        dialog.dismiss();

                        /*
                        if(!password.equals(trip.getGroup().getPassword())) {
                            Toast.makeText(parent, "비밀번호를 확인 해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        */
                        requestGroupJoin(trip.getGroup().getId(), password);
                    }
                });
                dialog.show();
            }
        });

        Button btn_find = (Button) view.findViewById(R.id.btn_find);
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestGroupFind();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        edit_name.setText(null);
    }

    private boolean validateInput() {
        if (edit_name.getText().toString().length() == 0) {
            Toast.makeText(parent, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /* 서버에 이름이 포함된 그룹 정보를 요청 */
    private void requestGroupFind() {
        if(!validateInput())
            return;

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_name.getWindowToken(), 0);

        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "find");

        JSONObject data = new JSONObject();
        try {
            data.put("name", edit_name.getText().toString());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    /* 서버에 그룹 가입을 요청 */
    private void requestGroupJoin(int group_id, String password) {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "join");

        JSONObject data = new JSONObject();
        try {
            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
            data.put("group_id", group_id);

            data.put("password", password);
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

            TripManager tripManager = new TripManager();
            tripManager.loadTrip(array);

            ArrayList<Trip> trips = tripManager.getTrips();
            adapter.clear();
            for (int i = 0; i < trips.size(); i++)
                adapter.add(trips.get(i));
            adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onJoinReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result)
                parent.redirectFragment(0);
            else {
                int code = json.getInt("code");
                switch (code) {
                    case 1:
                        Toast.makeText(parent, "CODE: " + code + ", " + "비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(parent, "CODE: " + code + ", " + "이미 가입되어 있습니다.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
