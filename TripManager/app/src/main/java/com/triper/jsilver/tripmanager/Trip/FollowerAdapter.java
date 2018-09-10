package com.triper.jsilver.tripmanager.Trip;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mikhaellopez.circularimageview.CircularImageView;
import com.triper.jsilver.tripmanager.DataType.Group;
import com.triper.jsilver.tripmanager.DataType.Member;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by JSilver on 2017-10-03.
 */

public class FollowerAdapter extends BaseAdapter {
    private Activity main;
    private ArrayList<Member> members;

    private Group group;

    public FollowerAdapter(Activity main, Group group) {
        this.main = main;
        members = new ArrayList<Member>();

        this.group = group;
    }

    @Override

    public int getCount() {
        return members.size();
    }

    @Override
    public Object getItem(int i) {
        return members.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) parent.getContext()).getLayoutInflater();
        View view = inflater.inflate(R.layout.listviewitem_follower, null);

        final Member member = members.get(position);

        CircularImageView img_picture = (CircularImageView) view.findViewById(R.id.img_picture);
        TextView txt_name = (TextView) view.findViewById(R.id.txt_name);
        TextView txt_phone = (TextView) view.findViewById(R.id.txt_phone);
        if(member.getMember_picture() != null)
            img_picture.setImageBitmap(member.getMember_picture());

        final ToggleButton btn_accessible = (ToggleButton) view.findViewById(R.id.btn_accessible);
        if (group.getEditors().contains(member.getKakao_id())) {
            btn_accessible.setChecked(true);
            btn_accessible.setBackgroundResource(R.drawable.icon_edit_active);
        }
        else {
            btn_accessible.setChecked(false);
            btn_accessible.setBackgroundResource(R.drawable.icon_edit_inactive);
        }

        btn_accessible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(member.getKakao_id().longValue() == group.getLeader().longValue()) {
                    Toast.makeText(main, "관리자 권한은 수정이 불가능 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(btn_accessible.isChecked()) {
                    btn_accessible.setBackgroundResource(R.drawable.icon_edit_active);
                    requesFollowerAccessible(member.getKakao_id(), true);
                }
                else {
                    btn_accessible.setBackgroundResource(R.drawable.icon_edit_inactive);
                    requesFollowerAccessible(member.getKakao_id(), false);
                }
            }
        });

        txt_name.setText(member.getName());
        txt_phone.setText(member.getPhone());

        return view;
    }

    public void add(Member member) {
        members.add(member);
    }

    public void clear() { members.clear(); }

    private void requesFollowerAccessible(Long member_id, boolean accessible) {
        Intent service = new Intent(main, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_FOLLOWER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "accessible");

        JSONObject data = new JSONObject();
        try {
            data.put("member_id", member_id);
            data.put("group_id", group.getId());
            data.put("accessible", accessible);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        main.startService(service);

        GlobalApplication.getInstance().progressOn(main, "loading");
    }
}
