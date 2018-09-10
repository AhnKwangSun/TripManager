package com.triper.jsilver.tripmanager.Trip;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.triper.jsilver.tripmanager.DataType.Notification;
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

public class TripNotificationFragment extends Fragment {
    private TripActivity parent;

    private ListView list_notification;
    private NotificationAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (TripActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_notification, container, false);
        if(parent.isAccessible()) {
            adapter = new NotificationAdapter(parent);

            ImageView img_create = (ImageView)view.findViewById(R.id.img_create);
            img_create.setVisibility(View.VISIBLE);
            img_create.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final NotificationCreateDialog dialog = new NotificationCreateDialog(parent);
                    dialog.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String content = dialog.getContent();

                            if(!validateInput(content))
                                return;

                            requestNotificationCreate(content);

                            dialog.hideKeyboard();
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }
            });
        }
        else {
            ((LinearLayout) view.findViewById(R.id.layout_top)).setVisibility(View.GONE);
            adapter = new NotificationAdapter(null);
        }

        list_notification = (ListView)view.findViewById(R.id.list_notification);
        list_notification.setAdapter(adapter);

        requestNotificationDisplay();
        return view;
    }

    private boolean validateInput(String content) {
        if (content.length() == 0) {
            Toast.makeText(parent, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void requestNotificationCreate(String content) {
        if(!validateInput(content))
            return;

        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_NOTIFICATION);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "create");

        JSONObject data = new JSONObject();
        try {
            data.put("group_id", parent.getTrip().getGroup().getId());
            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
            data.put("content", content);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    private void requestNotificationDisplay() {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_NOTIFICATION);
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

    public void onCreateReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result)
                parent.refreshFragment(this);
            else {
                int code = json.getInt("code");
                switch (code) {
                    case 1:
                        Toast.makeText(parent, "CODE: " + code + ", " + "권한이 없습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(parent, "CODE: " + code, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result)
                parent.refreshFragment(this);
            else {
                int code = json.getInt("code");
                switch (code) {
                    case 1:
                        Toast.makeText(parent, "CODE: " + code + ", " + "권한이 없습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(parent, "CODE: " + code, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onDeleteReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result)
                parent.refreshFragment(this);
            else {
                int code = json.getInt("code");
                switch (code) {
                    case 1:
                        Toast.makeText(parent, "CODE: " + code + ", " + "권한이 없습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(parent, "CODE: " + code, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onDisplayReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONArray array = new JSONArray(data);
            parent.getTrip().loadNotification(array);

            ArrayList<Notification> notifications = parent.getTrip().getNotifications();
            for(int i = 0; i < notifications.size(); i++)
                adapter.add(notifications.get(i));
            adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
