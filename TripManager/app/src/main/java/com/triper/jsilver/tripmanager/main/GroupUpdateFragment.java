package com.triper.jsilver.tripmanager.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularimageview.CircularImageView;
import com.triper.jsilver.tripmanager.DataType.Group;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by JSilver on 2017-10-02.
 */

public class GroupUpdateFragment extends Fragment {
    private MainActivity parent;

    private Group group;

    private EditText edit_name;
    private CircularImageView img_picture;
    private EditText edit_password;
    private TextView txt_start_date;
    private TextView txt_end_date;
    private EditText edit_radius;
    private Switch switch_alram;

    private boolean isTracing;
    private Bitmap picture;

    public GroupUpdateFragment() {
        Bundle bundle = new Bundle();
        setArguments(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (MainActivity) getActivity();
        group = GlobalApplication.getInstance().getTripManager().getTrips().get(getArguments().getInt("index")).getGroup().clone();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_update, container, false);

        edit_name = (EditText) view.findViewById(R.id.edit_name);
        img_picture = (CircularImageView) view.findViewById(R.id.img_picture);
        edit_password = (EditText) view.findViewById(R.id.edit_password);
        txt_start_date = (TextView) view.findViewById(R.id.txt_start_date);
        txt_end_date = (TextView) view.findViewById(R.id.txt_end_date);
        edit_radius = (EditText) view.findViewById(R.id.edit_radius);
        switch_alram = (Switch) view.findViewById(R.id.switch_alram);

        switch_alram.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isTracing = b;
            }
        });

        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        start.setTime(group.getStart_date());
        end.setTime(group.getEnd_date());

        txt_start_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                        Date select = new Date(year - 1900, monthOfYear, dayOfMonth);
                        txt_start_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(select));
                        start.set(year, monthOfYear, dayOfMonth);

                        try {
                            if(select.after(new SimpleDateFormat("yyyy-MM-dd").parse(txt_end_date.getText().toString())))
                                txt_end_date.setText("");
                        }
                        catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
                dpd.setMinDate(now);
                dpd.show(parent.getFragmentManager(), "start_date");
            }
        });

        txt_end_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dpd = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                        txt_end_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date(year - 1900, monthOfYear, dayOfMonth)));
                        end.set(year, monthOfYear, dayOfMonth);
                    }
                }, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH));
                dpd.setMinDate(start);
                dpd.show(parent.getFragmentManager(), "end_date");
            }
        });

        LinearLayout layout_picture = (LinearLayout) view.findViewById(R.id.layout_picture);
        layout_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GlobalApplication.PICK_FROM_ALBUM);
            }
        });

        Button btn_update = (Button) view.findViewById(R.id.btn_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestGroupUpdate();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        edit_name.setText(group.getName().toString());
        if(group.getGroup_picture() != null) {
            img_picture.setImageBitmap(group.getGroup_picture());
            picture = group.getGroup_picture();
        }
        edit_password.setText(group.getPassword());
        txt_start_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(group.getStart_date()));
        txt_end_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(group.getEnd_date()));
        edit_radius.setText(String.valueOf(group.getRadius()));
        switch_alram.setChecked(group.isTracing());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            group.setName(edit_name.getText().toString());
            group.setPassword(edit_password.getText().toString());
            group.setStart_date(new SimpleDateFormat("yyyy-MM-dd").parse(txt_start_date.getText().toString()));
            group.setEnd_date(new SimpleDateFormat("yyyy-MM-dd").parse(txt_end_date.getText().toString()));
            group.setRadius(Integer.parseInt(edit_radius.getText().toString()));
            group.setIsTracing(switch_alram.isChecked());
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GlobalApplication.PICK_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(parent.getContentResolver(), data.getData());
                    bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.getWidth() * 120) / bitmap.getHeight(), 120, true);
                    picture = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - 50, 0, 100, 100);
                    img_picture.setImageBitmap(picture);
                    group.setGroup_picture(picture);
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean validateInput() {
        if (edit_name.getText().toString().length() == 0) {
            Toast.makeText(parent, "여행 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (txt_start_date.getText().toString() == "") {
            Toast.makeText(parent, "시작 일자를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (txt_end_date.getText().toString() == "") {
            Toast.makeText(parent, "종료 일자를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (edit_radius.getText().toString().length() == 0) {
            Toast.makeText(parent, "알림 범위를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void requestGroupUpdate() {
        if(!validateInput())
            return;

        /* 서버에 그룹 생성 정보를 보냄 */
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "update");

        JSONObject data = new JSONObject();
        try {
            data.put("id", group.getId());
            data.put("group_picture", (picture == null) ? null : GlobalApplication.getInstance().getStringFromBitmap(picture));
            data.put("name", edit_name.getText().toString());
            data.put("password", (edit_password.getText().toString().length() == 0) ? JSONObject.NULL : edit_password.getText().toString());
            data.put("start_date", txt_start_date.getText().toString());
            data.put("end_date", txt_end_date.getText().toString());
            data.put("radius", edit_radius.getText().toString());
            data.put("isTracing", (isTracing) ? 1 : 0);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    public void onUpdateReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result) {
                parent.redirectFragment(0);
                picture = null;
            }
            else
                Toast.makeText(parent, "CODE: " + json.getInt("code") + ", " + "이미 존재하는 그룹 명 입니다.", Toast.LENGTH_SHORT).show();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
