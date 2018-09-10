package com.triper.jsilver.tripmanager.login;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.usermgmt.response.model.UserProfile;
import com.labo.kaji.fragmentanimations.MoveAnimation;
import com.triper.jsilver.tripmanager.DataType.Member;
import com.triper.jsilver.tripmanager.DataType.TripManager;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;

/**
 * Created by JSilver on 2017-09-06.
 */

public class LoginExtraFragment extends Fragment {
    private LoginActivity parent;

    private Member user;

    private EditText edit_name;
    private EditText edit_phone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (LoginActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_extra, container, false);

        edit_name = (EditText) view.findViewById(R.id.edit_name);
        edit_phone = (EditText) view.findViewById(R.id.edit_phone);
        edit_phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (count == 1) {
                    if (start == 2 || start == 7) {
                        edit_phone.setText(edit_phone.getText() + "-");
                        edit_phone.setSelection(edit_phone.getText().length());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        Button btn_register = (Button) view.findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestMemberRegister();
            }
        });

        return view;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return MoveAnimation.create(MoveAnimation.LEFT, enter, 600);
    }

    private boolean validateInput() {
        String phone = edit_phone.getText().toString();
        if (phone.length() < 13 || phone.charAt(3) != '-' || phone.charAt(8) != '-') {
            Toast.makeText(parent, "전화번호 형식이 맞지 않습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void requestMemberRegister() {
        if(!validateInput())
            return;

        Long kakao_id = UserProfile.loadFromCache().getId();
        String name = edit_name.getText().toString();
        String phone = edit_phone.getText().toString();
        String fcm_token = FirebaseInstanceId.getInstance().getToken();

        DownloadImageTask asyncTask = new DownloadImageTask(kakao_id, name, phone, fcm_token);
        asyncTask.execute(UserProfile.loadFromCache().getProfileImagePath());
    }

    public void onRegisterReceived(String data) {
        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            /* 회원 등록 성공 후 메인 액티비티로 이동 */
            if(result) {
                TripManager tripManager = GlobalApplication.getInstance().getTripManager();
                tripManager.setUser(user);
                tripManager.saveUserData(getContext().getFilesDir(), "user.dat");

                startActivity(new Intent(parent, MainActivity.class));
                parent.finish();
            }
            else
                Toast.makeText(parent, "CODE: " + json.getInt("code"), Toast.LENGTH_SHORT).show();

            GlobalApplication.getInstance().progressOff();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private Long kakao_id;
        private String name;
        private String phone;
        private String fcm_token;

        public DownloadImageTask(Long kakao_id, String name, String phone, String fcm_token) {
            this.kakao_id = kakao_id;
            this.name = name;
            this.phone = phone;
            this.fcm_token = fcm_token;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Bitmap doInBackground(String... urls) {
            Bitmap bitmap = null;
            try {
                if (urls[0] != null)
                    bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(new URL(urls[0]).openConnection().getInputStream()), 100, 100, true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            /* 서버에 회원등록 정보를 보냄 */
            Intent service = new Intent(parent, SocketIOService.class);
            service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_MEMBER);
            service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "register");

            user = new Member(kakao_id, name, result, phone, fcm_token);
            service.putExtra(SocketIOService.EXTRA_DATA,  user.toJSONObject().toString());
            parent.startService(service);

            GlobalApplication.getInstance().progressOn(parent, "loading");
        }
    }
}
