package com.triper.jsilver.tripmanager.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.IntroActivity;
import com.triper.jsilver.tripmanager.Member.MemberUpdateFragment;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.Trip.TripActivity;
import com.triper.jsilver.tripmanager.service.GPSService;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;

    private BroadcastReceiver receiver;

    private Fragment[] fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GlobalApplication.setCurrentActivity(this);

        /* 통신 리시버 등록 */
        IntentFilter filter = new IntentFilter("SocketIOService");
        receiver = new MainSocketIOReceiver();
        registerReceiver(receiver, filter);

        if(!GlobalApplication.getInstance().getOffineMode()) {
            if (GlobalApplication.getInstance().checkLocationServices()) {
            /* 위치추적 권한 확인 */
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

            /* 위치추적 서비스 등록 */
                Intent service = new Intent(this, GPSService.class);
                startService(service);
            }
            else {
                Toast.makeText(this, "위치 서비스를 활성화 시킨 후 어플리케이션을 재실행 해주십시오.", Toast.LENGTH_SHORT).show();

                Intent setting = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                overridePendingTransition(R.anim.slide_right_to_left, 0);
                startActivity(setting);
                finish();
            }
        }

        /* 그룹 관리에 필요한 Fragment 생성 */
        fragments = new Fragment[5];
        fragments[0] = new GroupDisplayFragment();
        fragments[1] = new GroupCreateFragment();
        fragments[2] = new GroupJoinFragment();
        fragments[3] = new GroupUpdateFragment();
        fragments[4] = new MemberUpdateFragment();

        ImageView img_option = (ImageView) findViewById(R.id.img_option);
        img_option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectFragment(4);
            }
        });
        TextView txt_title = (TextView) findViewById(R.id.txt_title);
        txt_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectFragment(0);
            }
        });

        /* GroupDispalyFragment 출력 */
        getSupportFragmentManager().beginTransaction().replace(R.id.layout_fragment, fragments[0]).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* 통신 서비스 리시버 해제 */
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if (intervalTime <= FINISH_INTERVAL_TIME) {
            super.onBackPressed();
        } else {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(), "뒤로 버튼을 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public Fragment getFragment(int index) { return fragments[index]; }

    public void redirectFragment(int index) {
        FragmentManager fm = getSupportFragmentManager();

        /* 현재 보여지는 Fragment를 찾음 */
        Fragment current = null;
        for(int i = 0; i < fm.getFragments().size(); i++) {
            Fragment f = fm.getFragments().get(i);
            if(f == null)
                break;

            current = f;
        }

        /* redirectFragment(0) : Group Display로 이동하는 경우,
         * 다른 화면에서 이동하는 경우에는 Stack에서 제거하는 방식으로 동작, Display에서 이동하는 경우에는 새로고침 함 */
        if(current == fragments[index])
            getSupportFragmentManager().beginTransaction().detach(fragments[index]).attach(fragments[index]).commit();
        else {
            if(index == 0 && fm.getBackStackEntryCount() > 0)
                for(int i = 0; i < fm.getBackStackEntryCount(); i++)
                    fm.popBackStack();
            else
                if (!isFinishing())
                    fm.beginTransaction().addToBackStack(null).replace(R.id.layout_fragment, fragments[index]).commit();
        }
    }

    public void redirectTripActivity(int index) {
        Intent intent = new Intent(this, TripActivity.class);
        intent.putExtra("index", index);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    public class MainSocketIOReceiver extends BroadcastReceiver {

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
                case SocketIOService.EVENT_TYPE_MEMBER:
                    onMemberReceived(intent);
                    break;
                case SocketIOService.EVENT_TYPE_GROUP:
                    onGroupReceived(intent);
                    break;
            }
        }

        private void onErrorReceived(Intent intent) {
            try {
                JSONObject data = new JSONObject(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                GlobalApplication.getInstance().progressOff();

                switch (data.getInt("code")) {
                    case 0:
                        Toast.makeText(MainActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(MainActivity.this, "서버가 응답하지 않습니다.", Toast.LENGTH_SHORT).show();
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
                case "update":
                    ((MemberUpdateFragment) fragments[4]).onUpdateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "unregister":
                    ((MemberUpdateFragment) fragments[4]).onUnregisterReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }

        private void onGroupReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);
            switch (sub_event) {
                case "create":
                    ((GroupCreateFragment) fragments[1]).onCreateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "update":
                    ((GroupUpdateFragment) fragments[3]).onUpdateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "find":
                    ((GroupJoinFragment) fragments[2]).onFindReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "join":
                    ((GroupJoinFragment) fragments[2]).onJoinReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "exit":
                    ((GroupDisplayFragment) fragments[0]).onExitReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "display":
                    ((GroupDisplayFragment) fragments[0]).onDisplayReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "getEditor":
                    ((GroupDisplayFragment) fragments[0]).onGetEditorReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }
    }
}
