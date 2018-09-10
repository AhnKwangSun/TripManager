package com.triper.jsilver.tripmanager.main;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularimageview.CircularImageView;
import com.triper.jsilver.tripmanager.DataType.Group;
import com.triper.jsilver.tripmanager.DataType.Trip;
import com.triper.jsilver.tripmanager.DataType.TripManager;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Created by JSilver on 2017-09-15.
 */

public class GroupAdapter extends BaseAdapter {
    private MainActivity main;
    private int resource;

    private ArrayList<Trip> trips;

    public GroupAdapter(int resource, MainActivity main) {
        this.main = main;
        this.resource = resource;
        trips = new ArrayList<Trip>();
    }

    @Override
    public int getCount() {
        return trips.size();
    }

    @Override
    public Object getItem(int i) {
        return trips.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) parent.getContext()).getLayoutInflater();
        View view = inflater.inflate(resource, null);

        Group group = trips.get(position).getGroup();

        CircularImageView img_picture = (CircularImageView) view.findViewById(R.id.img_picture);
        TextView txt_members = (TextView) view.findViewById(R.id.txt_members);
        TextView txt_name = (TextView) view.findViewById(R.id.txt_name);
        TextView txt_date = (TextView) view.findViewById(R.id.txt_date);

        if(group.getGroup_picture() != null)
            img_picture.setImageBitmap(group.getGroup_picture());
        txt_members.setText(String.valueOf(group.getMembers().size()));
        txt_name.setText(group.getName());
        txt_date.setText(new SimpleDateFormat("yy-MM-dd").format(group.getStart_date()) + " ~ " + new SimpleDateFormat("yy-MM-dd").format(group.getEnd_date()));

        final ImageView img_menu = (ImageView) view.findViewById(R.id.img_menu);
        if(img_menu != null) {
            final boolean isLeader = (group.getLeader().longValue() == GlobalApplication.getInstance().getTripManager().getUser().getKakao_id().longValue()) ? true : false;
            final int index = position;
            img_menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isLeader)
                        showPopupMenu(img_menu, index, R.menu.menu_group);
                    else
                        showPopupMenu(img_menu, index, R.menu.menu_group_follower);
                }
            });
        }

        return view;
    }

    private void showPopupMenu(View view, final int index, final int resource) {
        PopupMenu popup = new PopupMenu(main, view);
        main.getMenuInflater().inflate(resource, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_modify:
                        Calendar now = Calendar.getInstance();
                        if (now.getTime().after(trips.get(index).getGroup().getEnd_date())) {
                            Toast.makeText(main, "지난 여행에 대해서는 수정 할 수 없습니다.", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        main.getFragment(3).getArguments().putInt("index", index);
                        main.redirectFragment(3);
                        break;
                    case R.id.menu_delete:
                        requestGroupExit(index);
                        break;
                }
                return false;
            }
        });
        popup.show();
    }

    public void add(Trip trip) {
        trips.add(trip);
    }

    public void clear() { trips.clear(); }

    private void requestGroupExit(int index) {
        Intent service = new Intent(main, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "exit");

        TripManager tripManager = GlobalApplication.getInstance().getTripManager();

        JSONObject data = new JSONObject();
        try {
            data.put("member_id", tripManager.getUser().getKakao_id());
            data.put("group_id", tripManager.getTrips().get(index).getGroup().getId());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        main.startService(service);

        GlobalApplication.getInstance().progressOn(main, "loading");
    }
}
