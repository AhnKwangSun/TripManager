package com.triper.jsilver.tripmanager.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularimageview.CircularImageView;
import com.triper.jsilver.tripmanager.DataType.Group;
import com.triper.jsilver.tripmanager.DataType.TripManager;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-20.
 */

public class GroupCreateFragment extends Fragment {
    private MainActivity parent;

    private TripManager tripManager;

    private EditText edit_name;
    private CircularImageView img_picture;
    private EditText edit_password;
    private TextView txt_start_date;
    private TextView txt_end_date;

    private Bitmap picture;

    private Group group;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (MainActivity) getActivity();
        tripManager = GlobalApplication.getInstance().getTripManager();
        group = new Group();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_create, container, false);

        edit_name = (EditText) view.findViewById(R.id.edit_name);
        img_picture = (CircularImageView) view.findViewById(R.id.img_picture);
        edit_password = (EditText) view.findViewById(R.id.edit_password);
        txt_start_date = (TextView) view.findViewById(R.id.txt_start_date);
        txt_end_date = (TextView) view.findViewById(R.id.txt_end_date);

        Button btn_create = (Button) view.findViewById(R.id.btn_create);
        btn_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestGroupCreate();
            }
        });

        final Calendar now = Calendar.getInstance();
        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        start.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        end.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));

        txt_start_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        edit_name.setText(group.getName());
        if(group.getGroup_picture() != null)
            img_picture.setImageBitmap(group.getGroup_picture());
        if (group.getStart_date() != null)
            txt_start_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(group.getStart_date()));
        if (group.getEnd_date() != null)
            txt_end_date.setText(new SimpleDateFormat("yyyy-MM-dd").format(group.getEnd_date()));
        edit_password.setText(group.getPassword());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            group.setName(edit_name.getText().toString());
            if (txt_start_date.getText().toString() != "")
                group.setStart_date(new SimpleDateFormat("yyyy-MM-dd").parse(txt_start_date.getText().toString()));
            if(txt_end_date.getText().toString() != "")
                group.setEnd_date(new SimpleDateFormat("yyyy-MM-dd").parse(txt_end_date.getText().toString()));
            group.setPassword(edit_password.getText().toString());
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

        if (edit_password.getText().toString().length() == 0) {
            Toast.makeText(parent, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void requestGroupCreate() {
        if(!validateInput())
            return;

        /* 서버에 그룹 생성 정보를 보냄 */
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_GROUP);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "create");

        JSONObject data = new JSONObject();
        try {
            data.put("name", edit_name.getText().toString());
            data.put("group_picture", (picture == null) ? JSONObject.NULL : GlobalApplication.getInstance().getStringFromBitmap(picture));
            data.put("password", edit_password.getText().toString());
            data.put("start_date", txt_start_date.getText().toString());
            data.put("end_date", txt_end_date.getText().toString());
            data.put("leader", tripManager.getUser().getKakao_id());
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
