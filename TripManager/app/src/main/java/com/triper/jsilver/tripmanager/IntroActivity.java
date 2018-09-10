package com.triper.jsilver.tripmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.login.LoginActivity;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by JSilver on 2017-09-10.
 */

public class IntroActivity extends AppCompatActivity {
    private BroadcastReceiver receiver;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        if(GlobalApplication.getCurrentActivity() != null)
            GlobalApplication.getCurrentActivity().finish();

        GlobalApplication.setCurrentActivity(this);

        /* 통신 서비스 시작 */
        Intent service = new Intent(this, SocketIOService.class);
        startService(service);

        /* 통신 리시버 등록 */
        IntentFilter filter = new IntentFilter("SocketIOService");
        receiver = new LoginSocketIOReceiver();
        registerReceiver(receiver, filter);

        /* 기기의 네트워크 상태를 확인 */
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            GlobalApplication.getInstance().setOffineMode(true);
            if (GlobalApplication.getInstance().getTripManager().loadUserData(getFilesDir(), "user.dat")) {
                Toast.makeText(this, "오프라인 모드로 시작합니다. 최신 정보와 다를 수 있습니다.", Toast.LENGTH_SHORT).show();

                handler.postDelayed(new Runnable() {
                    public void run() {
                        startActivity(new Intent(IntroActivity.this, MainActivity.class));
                        finish();
                    }
                }, 1000);
            }
            else {
                Toast.makeText(this, "Wifi, 데이터 상태를 확인해주세요.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else {
            GlobalApplication.getInstance().setOffineMode(false);
            handler.postDelayed(new Runnable() {
                public void run() {
                    requestApplicationVersion();
                }
            }, 1000);
        }
    }

    @Override
    public void onBackPressed() {
        /* 뒤로가기를 눌러도 종료되지 않게 재정의 */
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public void requestMe() {
        UserManagement.requestMe(new MeResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                Log.e("[ERROR]", "requestMe.onFailure : " + errorResult);
            }

            /* 세션이 닫혀있는 경우, LoginActivity로 이동 */
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                Log.e("[ERROR]", "requestMe.onSessionClosed : " + errorResult);

                handler.postDelayed(new Runnable() {
                    public void run() {
                        startActivity(new Intent(IntroActivity.this, LoginActivity.class));
                        finish();
                    }
                }, 1000);
            }

            @Override
            public void onNotSignedUp() {
                Log.e("[ERROR]", "requestMe.onNotSignedUp");
            }

            /* 세션이 열려있고 카카오로부터 사용자 인증정보를 받아온 경우, 내부 저장소로부터 사용자 정보를 읽어서 저장한 후 MainActivity로 이동 */
            @Override
            public void onSuccess(UserProfile result) {
                if (!GlobalApplication.getInstance().getTripManager().loadUserData(getFilesDir(), "user.dat")) {
                    /* 세션은 열려있으나 유저파일이 없는 경우.  */
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            startActivity(new Intent(IntroActivity.this, LoginActivity.class).putExtra("group_id", getIntent().getIntExtra("group_id", -1)));
                            finish();
                        }
                    }, 1000);
                }
                else {
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            requestApplicationUser();
                        }
                    }, 1000);
                }
            }
        });
    }

    private void requestApplicationVersion() {
        Intent service = new Intent(this, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_APPLICATION);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "version");

        JSONObject data = new JSONObject();
        try {
            data.put("version", getText(R.string.version));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        startService(service);

        GlobalApplication.getInstance().progressOn(this, "loading");
    }

    private void requestApplicationUser() {
        /* 서버에 회원등록 정보를 보냄 */
        Intent service = new Intent(this, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_APPLICATION);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "user");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", GlobalApplication.getInstance().getTripManager().getUser().getKakao_id());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA,  data.toString());
        startService(service);

        GlobalApplication.getInstance().progressOn(this, "loading");
    }

    private void onVersionReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result)
                requestMe();
            else {
                int code = json.getInt("code");
                switch (code) {
                    case 1:
                        Toast.makeText(this, "CODE: " + code + ", " + "최신 버전으로 업데이트 후 실행 가능합니다.", Toast.LENGTH_SHORT).show();
                        finish();

                        Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.triper.jsilver.tripmanager");
                        Intent intent  = new Intent(Intent.ACTION_VIEW,uri);
                        startActivity(intent);
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onUserReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if(result)
                startActivity(new Intent(IntroActivity.this, MainActivity.class).putExtra("group_id", getIntent().getIntExtra("group_id", -1)));
            else
                startActivity(new Intent(IntroActivity.this, LoginActivity.class).putExtra("group_id", getIntent().getIntExtra("group_id", -1)));

            finish();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public class LoginSocketIOReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int event_type = intent.getIntExtra(SocketIOService.EXTRA_EVENT_TYPE, -1);
            if (event_type == -1)
                return;

            /* 그룹 액티비티에서는 통신에러, 회원, 그룹관련 이벤트만 처리 */
            switch (event_type) {
                case SocketIOService.EVENT_TYPE_ERROR:
                    onErrorReceived(intent);
                    break;
                case SocketIOService.EVENT_TYPE_APPLICATION:
                    onApplicationReceived(intent);
                    break;
            }
        }

        private void onErrorReceived(Intent intent) {
            try {
                JSONObject data = new JSONObject(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                GlobalApplication.getInstance().progressOff();

                switch (data.getInt("code")) {
                    case 0:
                        Toast.makeText(IntroActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(IntroActivity.this, "서버가 응답하지 않습니다.", Toast.LENGTH_SHORT).show();
                        break;
                }

                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void onApplicationReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);

            switch (sub_event) {
                case "version":
                    onVersionReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "user":
                    onUserReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }
    }
}
