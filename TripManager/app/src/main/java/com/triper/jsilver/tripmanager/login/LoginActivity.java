package com.triper.jsilver.tripmanager.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.util.helper.log.Logger;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JSilver on 2017-09-05.
 */

public class LoginActivity extends AppCompatActivity {
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;

    private BroadcastReceiver receiver;

    private Fragment[] fragments;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GlobalApplication.setCurrentActivity(this);

        /* 통신 서비스 리시버 등록 */
        IntentFilter filter = new IntentFilter("SocketIOService");
        receiver = new LoginSocketIOReceiver();
        registerReceiver(receiver, filter);

        /* 로그인에 필요한 Fragment 생성 */
        fragments = new Fragment[2];
        fragments[0] = new LoginKakaoFragment();
        fragments[1] = new LoginExtraFragment();

        /* LoginKakaoFragment 출력 */
        getSupportFragmentManager().beginTransaction().replace(R.id.layout_fragment, fragments[0]).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* 통신 서비스 리시버 해제 */
        unregisterReceiver(receiver);
    }

    /* 카카오 인증창에서 원래의 Activity로 돌아옴 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data))
            return;

        super.onActivityResult(requestCode, resultCode, data);
    }

    /* 뒤로가기 버튼을 2회 누를 시 어플리케이션 종료 */
    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if (intervalTime <= FINISH_INTERVAL_TIME) {
            /* 카카오 연결 해제 */
            UserManagement.requestUnlink(new UnLinkResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    Logger.e(errorResult.toString());
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Log.d("[ERROR]", "onSessionClosed = " + errorResult);
                }

                @Override
                public void onNotSignedUp() {
                    Log.d("[ERROR]", "onNotSignedUp");
                }

                @Override
                public void onSuccess(Long userId) {
                    Log.d("[ERROR]", userId.toString());
                }
            });
            /* 통신 서비스 종료 */
            Intent service = new Intent(this, SocketIOService.class);
            stopService(service);

            super.onBackPressed();
        }
        else {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(), "뒤로 버튼을 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void redirectFragment(int index) {
        if (!isFinishing())
            getSupportFragmentManager().beginTransaction().replace(R.id.layout_fragment, fragments[index]).commit();
    }

    /* 로그인 관련 통신 처리 */
    public class LoginSocketIOReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int event_type = intent.getIntExtra(SocketIOService.EXTRA_EVENT_TYPE, -1);
            if (event_type == -1)
                return;

            /* 로그인 액티비티에서는 통신에러, 회원관련 이벤트만 처리 */
            switch (event_type) {
                case SocketIOService.EVENT_TYPE_ERROR:
                    onErrorReceived(intent);
                    break;
                case SocketIOService.EVENT_TYPE_MEMBER:
                    onMemberReceived(intent);
                    break;
            }
        }

        private void onErrorReceived(Intent intent) {
            try {
                JSONObject data = new JSONObject(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                GlobalApplication.getInstance().progressOff();

                int code = data.getInt("code");
                switch (code) {
                    /* 서버가 닫혀있음 */
                    case 0:
                        Toast.makeText(LoginActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    /* 서버가 지연되고 있음 */
                    case 1:
                        Toast.makeText(LoginActivity.this, "서버가 응답하지 않습니다.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void onMemberReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);

            switch (sub_event) {
                case "check":
                    ((LoginKakaoFragment) fragments[0]).onCheckReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "register":
                    ((LoginExtraFragment) fragments[1]).onRegisterReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }
    }
}
