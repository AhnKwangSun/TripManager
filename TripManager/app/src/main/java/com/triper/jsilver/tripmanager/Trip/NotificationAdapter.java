package com.triper.jsilver.tripmanager.Trip;

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

import com.mikhaellopez.circularimageview.CircularImageView;
import com.triper.jsilver.tripmanager.DataType.Notification;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by JSilver on 2017-10-03.
 */

public class NotificationAdapter extends BaseAdapter {
    private Activity main;

    private ArrayList<Notification> notifications;

    public NotificationAdapter(Activity main) {
        this.main = main;
        notifications = new ArrayList<Notification>();
    }

    @Override
    public int getCount() {
        return notifications.size();
    }

    @Override
    public Object getItem(int i) {
        return notifications.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        LayoutInflater inflater = ((Activity) parent.getContext()).getLayoutInflater();
        View view = inflater.inflate(R.layout.listviewitem_notification, null);

        Notification notification = notifications.get(position);

        CircularImageView img_picture = (CircularImageView) view.findViewById(R.id.img_picture);
        TextView txt_name = (TextView) view.findViewById(R.id.txt_name);
        TextView txt_date = (TextView) view.findViewById(R.id.txt_date);
        TextView txt_content = (TextView) view.findViewById(R.id.txt_content);
        final ImageView img_menu = (ImageView) view.findViewById(R.id.img_menu);

        if(notification.getWriter().getMember_picture() != null)
            img_picture.setImageBitmap(notification.getWriter().getMember_picture());
        txt_name.setText(notification.getWriter().getName());
        txt_date.setText(new SimpleDateFormat("MM월 dd일 a h:mm", Locale.US).format(notification.getDate()));
        txt_content.setText(notification.getContent());

        if (main != null) {
            if (((TripActivity) main).isLeader() || GlobalApplication.getInstance().getTripManager().getUser().getKakao_id().longValue() == notification.getWriter().getKakao_id().longValue()) {
                img_menu.setVisibility(View.VISIBLE);

                final int index = position;
                img_menu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showPopupMenu(img_menu, index, R.menu.menu_notification);
                    }
                });
            }
            else
                img_menu.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    private void showPopupMenu(View view, final int index, final int resource) {
        PopupMenu popup = new PopupMenu(main, view);
        main.getMenuInflater().inflate(resource, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                final Notification notification = notifications.get(index);
                switch (menuItem.getItemId()) {
                    case R.id.menu_modify:
                        final NotificationUpdateDialog dialog = new NotificationUpdateDialog(main, notifications.get(index).getContent());
                        dialog.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String content = dialog.getContent();
                                dialog.hideKeyboard();
                                dialog.dismiss();

                                requestNotificationUpdate(notification.getId(), content);
                            }
                        });
                        dialog.show();
                        break;
                    case R.id.menu_delete:
                        requestNotificationDelete(notification.getId());
                        break;
                }
                return false;
            }
        });
        popup.show();
    }

    public void add(Notification notification) {
        notifications.add(notification);
    }

    private void requestNotificationUpdate(int id, String content) {
        Intent service = new Intent(main, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_NOTIFICATION);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "update");

        JSONObject data = new JSONObject();
        try {
            data.put("id", id);
            data.put("content", content);

            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
            data.put("group_id", ((TripActivity) main).getTrip().getGroup().getId());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        main.startService(service);

        GlobalApplication.getInstance().progressOn(main, "loading");
    }

    private void requestNotificationDelete(int id) {
        Intent service = new Intent(main, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_NOTIFICATION);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "delete");

        JSONObject data = new JSONObject();
        try {
            data.put("id", id);

            data.put("member_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
            data.put("group_id", ((TripActivity) main).getTrip().getGroup().getId());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        main.startService(service);

        GlobalApplication.getInstance().progressOn(main, "loading");
    }

}
