package com.triper.jsilver.tripmanager.Trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.triper.jsilver.tripmanager.DataType.Trip;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Created by JSilver on 2017-10-03.
 */

public class TripSchedulePagerFragment extends Fragment {
    private TripActivity parent;

    private ViewPager viewpager;
    private SchedulePagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (TripActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_schedule_viewpager, container, false);

        viewpager = (ViewPager) view.findViewById(R.id.viewpager);

        adapter = new SchedulePagerAdapter(getChildFragmentManager(), parent.getTrip().getSchedules().size());
        viewpager.setAdapter(adapter);

        /* 현재 날짜에 맞는 스케쥴로 이동 */
        Calendar now = Calendar.getInstance();
        long start = parent.getTrip().getGroup().getStart_date().getTime();
        long end = now.getTime().getTime();
        int index = (int)((end - start) / Trip.MILIISEC_OF_DAY);
        viewpager.setCurrentItem((index < 0 || index >= adapter.getCount()) ? 0 : index);

        if(!GlobalApplication.getInstance().getOffineMode())
            requestScheduleDisplay();
        else {
            parent.getTrip().loadScheduleData(getContext().getFilesDir(), "schedule.dat", parent.getTrip().getGroup().getId());
            adapter.notifyDataSetChanged();
        }
        return view;
    }

    private void requestScheduleDisplay() {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_SCHEDULE);
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
                        Toast.makeText(parent, "CODE: " + code + ", " + "일정이 이미 존재합니다.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(parent, "CODE: " + code + ", " + "일정이 이미 존재합니다.", Toast.LENGTH_SHORT).show();
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
            parent.getTrip().saveScheduleData(getContext().getFilesDir(), "schedule.dat", array, parent.getTrip().getGroup().getId());
            parent.getTrip().loadSchedule(array);
            adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
