package com.triper.jsilver.tripmanager.Trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triper.jsilver.tripmanager.DataType.HourlySchedule;
import com.triper.jsilver.tripmanager.DataType.Schedule;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;
import org.qap.ctimelineview.TimelineRow;
import org.qap.ctimelineview.TimelineViewAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by JSilver on 2017-10-03.
 */

public class TripScheduleFragment extends Fragment {
    private TripActivity parent;

    private Schedule schedule;
    private int day;
    private boolean isAccessible;

    private ListView list_schedule;
    private TimelineViewAdapter adapter;

    public static Fragment newFragment(int day) {
        Bundle bundle = new Bundle();
        Fragment fragment = new TripScheduleFragment();
        bundle.putInt("day", day);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (TripActivity) getActivity();

        day = getArguments().getInt("day");
        schedule = parent.getTrip().getSchedules().get(day - 1);
        isAccessible = parent.isAccessible();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_schedule, container, false);

        TextView txt_day = (TextView) view.findViewById(R.id.txt_day);
        TextView txt_date = (TextView) view.findViewById(R.id.txt_date);
        txt_day.setText("Day " + day);
        txt_date.setText(new SimpleDateFormat("yyyy.MM.dd").format(schedule.getDate()));

        list_schedule = (ListView) view.findViewById(R.id.list_schedule);
        adapter = new TimelineViewAdapter(parent, R.layout.ctimeline_row,  new ArrayList<TimelineRow>(), false);
        list_schedule.setAdapter(adapter);

        if(isAccessible) {
            ImageButton btn_create = (ImageButton) view.findViewById(R.id.btn_create);
            btn_create.setVisibility(View.VISIBLE);
            btn_create.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final ScheduleCreateDialog dialog = new ScheduleCreateDialog(parent);
                    dialog.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String time = dialog.getTime();
                            String content = dialog.getContent();

                            if(!validateInput(time, content))
                                return;

                            requestScheduleCreate(time, content);

                            dialog.hideKeyboard();
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            });

            list_schedule.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    try {
                        final TimelineRow timelineRow = (TimelineRow) adapterView.getItemAtPosition(i);
                        final ScheduleUpdateDialog dialog = new ScheduleUpdateDialog(parent, new SimpleDateFormat("HH:mm").format(new SimpleDateFormat("h:mm a", Locale.US).parse(timelineRow.getTitle())), timelineRow.getDescription());
                        dialog.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                switch (view.getId()) {
                                    case R.id.btn_update:
                                        String time = dialog.getTime();
                                        String content = dialog.getContent();

                                        if(!validateInput(time, content))
                                            return;

                                        requestScheduleUpdate(timelineRow.getId(), time, content);
                                        break;
                                    case R.id.btn_delete:
                                        requestScheduleDelete(timelineRow.getId());
                                        break;
                                }

                                dialog.hideKeyboard();
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    }
                    catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        dataSetUpdate();
        return view;
    }

    private void dataSetUpdate() {
        ArrayList<HourlySchedule> hourlySchedules = schedule.getHourlySchedules();
        Calendar now = Calendar.getInstance();
        for (int i = 0; i < hourlySchedules.size(); i++) {
            HourlySchedule hourlySchedule = hourlySchedules.get(i);
            TimelineRow timelineRow = new TimelineRow(hourlySchedule.getId());
            timelineRow.setTitle(new SimpleDateFormat("h:mm a", Locale.US).format(hourlySchedule.getTime()));
            timelineRow.setDescription(hourlySchedule.getContent());
            timelineRow.setBellowLineColor(ContextCompat.getColor(parent, R.color.colorLightGrayGreen));
            timelineRow.setBellowLineSize(1);
            timelineRow.setBackgroundColor(ContextCompat.getColor(parent, R.color.colorGreen));
            timelineRow.setBackgroundSize(12);
            if(now.getTime().after(hourlySchedule.getTime()))
                timelineRow.setBackgroundColor(ContextCompat.getColor(parent, R.color.colorLightGrayGreen));
            timelineRow.setTitleColor(ContextCompat.getColor(parent, R.color.colorGreen));
            timelineRow.setDescriptionColor(ContextCompat.getColor(parent, R.color.colorDarkGreen));

            adapter.add(timelineRow);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean validateInput(String time, String content) {
        if (time == "") {
            Toast.makeText(parent, "시간을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (content.length() == 0) {
            Toast.makeText(parent, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void requestScheduleCreate(String time, String content) {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_SCHEDULE);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "create");

        JSONObject data = new JSONObject();
        try {
            data.put("date", new SimpleDateFormat("yyyy-MM-dd ").format(schedule.getDate()) + time);
            data.put("content", content);
            data.put("group_id", parent.getTrip().getGroup().getId());

            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    private void requestScheduleUpdate(int id, String time, String content) {
        if(!validateInput(time, content))
            return;

        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_SCHEDULE);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "update");

        JSONObject data = new JSONObject();
        try {
            data.put("id", id);
            data.put("date", new SimpleDateFormat("yyyy-MM-dd ").format(schedule.getDate()) + time);
            data.put("content", content);

            data.put("group_id", parent.getTrip().getGroup().getId());
            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    private void requestScheduleDelete(int id) {
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_SCHEDULE);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "delete");

        JSONObject data = new JSONObject();
        try {
            data.put("id", id);

            data.put("group_id", parent.getTrip().getGroup().getId());
            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

}
