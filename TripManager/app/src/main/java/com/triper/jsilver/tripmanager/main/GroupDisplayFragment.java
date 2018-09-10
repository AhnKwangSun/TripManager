package com.triper.jsilver.tripmanager.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
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
 * Created by JSilver on 2017-09-20.
 */

public class GroupDisplayFragment extends Fragment {
    private MainActivity parent;

    private TripManager tripManager;

    private ListView list_group;
    private GroupAdapter adapter;
    private int index;

    private SwipeRefreshLayout layout_refresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (MainActivity) getActivity();
        tripManager = GlobalApplication.getInstance().getTripManager();
        index = -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_display, container, false);

        list_group = (ListView) view.findViewById(R.id.list_group);
        adapter = new GroupAdapter(R.layout.listviewitem_group, parent);
        list_group.setAdapter(adapter);

        list_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                index = i;
                if(!GlobalApplication.getInstance().getOffineMode())
                    requestGroupGetEditor();
                else
                    parent.redirectTripActivity(index);
            }
        });

        layout_refresh = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh);
        layout_refresh.setColorSchemeResources(R.color.colorWhite);
        layout_refresh.setProgressBackgroundColorSchemeResource(R.color.colorGreen);
        layout_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                parent.redirectFragment(0);
            }
        });

        FloatingActionButton fab_create = (FloatingActionButton) view.findViewById(R.id.fab_create);
        fab_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parent.redirectFragment(1);
            }
        });

        FloatingActionButton fab_join = (FloatingActionButton) view.findViewById(R.id.fab_join);
        fab_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parent.redirectFragment(2);
            }
        });

        if(!GlobalApplication.getInstance().getOffineMode())
            requestGroupDisplay();
        else {
            tripManager.loadTripData(getContext().getFilesDir(), "trip.dat");

            ArrayList<Trip> trips = tripManager.getTrips();
            for (int i = 0; i < trips.size(); i++)
                adapter.add(trips.get(i));
            adapter.notifyDataSetChanged();
        }

        return view;
    }

    private void requestGroupDisplay() {
        /* 가입된 그룹 정보를 서버에 요청 */
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "display");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", tripManager.getUser().getKakao_id());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    private void requestGroupGetEditor() {
        if(index == -1)
            return;

        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "getEditor");

        JSONObject data = new JSONObject();
        try {
            data.put("group_id", tripManager.getTrips().get(index).getGroup().getId());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    public void onDisplayReceived(String data) {
        GlobalApplication.getInstance().progressOff();
        layout_refresh.setRefreshing(false);

        try {
            JSONArray array = new JSONArray(data);
            tripManager.saveTripData(getContext().getFilesDir(), "trip.dat", array);
            tripManager.loadTrip(array);


            ArrayList<Trip> trips = tripManager.getTrips();
            /* Notification으로 실행된 경우 해당 Trip으로 이동 */
            int group_id = parent.getIntent().getIntExtra("group_id", -1);
            for (int i = 0; i < trips.size(); i++) {
                adapter.add(trips.get(i));
                if (group_id > 0)
                    if (trips.get(i).getGroup().getId() == group_id)
                        parent.redirectTripActivity(i);
            }
            adapter.notifyDataSetChanged();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onExitReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result)
                parent.redirectFragment(0);
            else
                Toast.makeText(parent, json.getString("code"), Toast.LENGTH_SHORT).show();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onGetEditorReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONArray array = new JSONArray(data);
            tripManager.getTrips().get(index).getGroup().updateEditors(array);

            parent.redirectTripActivity(index);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
