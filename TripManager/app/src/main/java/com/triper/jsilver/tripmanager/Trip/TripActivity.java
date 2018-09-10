package com.triper.jsilver.tripmanager.Trip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.triper.jsilver.tripmanager.DataType.Trip;
import com.triper.jsilver.tripmanager.DataType.TripManager;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JSilver on 2017-10-03.
 */

public class TripActivity extends AppCompatActivity {
    private BroadcastReceiver receiver;

    private Fragment[] fragments;

    private ImageView[] buttons;
    private int[] images_active = { R.drawable.icon_schedule_active, R.drawable.icon_notification_active, R.drawable.icon_address_active, R.drawable.icon_map_active };
    private int[] images_inactive = { R.drawable.icon_schedule_inactive, R.drawable.icon_notification_inactive, R.drawable.icon_address_inactive, R.drawable.icon_map_inactive };
    private int current;

    private Trip trip;
    private boolean isLeader = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        GlobalApplication.setCurrentActivity(this);

        TripManager tripManager = GlobalApplication.getInstance().getTripManager();
        trip = tripManager.getTrips().get(getIntent().getIntExtra("index", -1));
        if (tripManager.getUser().getKakao_id().longValue() == trip.getGroup().getLeader().longValue())
            isLeader = true;

        /* 통신 리시버 등록 */
        IntentFilter filter = new IntentFilter("SocketIOService");
        receiver = new TripSocketIOReceiver();
        registerReceiver(receiver, filter);

        /* 여행 관리에 필요한 Fagment 생성 */
        fragments = new Fragment[6];
        fragments[0] = new TripSchedulePagerFragment();
        fragments[1] = new TripNotificationFragment();
        fragments[2] = new TripFollowerFragment();
        fragments[3] = new TripMapFragment();

        TextView txt_title = (TextView) findViewById(R.id.txt_title);
        txt_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectMainActivity();
            }
        });

        buttons = new ImageView[4];
        buttons[0] = (ImageView) findViewById(R.id.img_schedule);
        buttons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectFragment(0);
            }
        });
        buttons[1] = (ImageView) findViewById(R.id.img_notification);
        buttons[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectFragment(1);
            }
        });
        buttons[2] = (ImageView) findViewById(R.id.img_follower);
        buttons[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLeader)
                    redirectFragment(2);
                else
                    Toast.makeText(TripActivity.this, "인솔자에게만 보여지는 기능입니다.", Toast.LENGTH_SHORT).show();
            }
        });
        buttons[3] = (ImageView) findViewById(R.id.img_map);
        buttons[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectFragment(3);
            }
        });

        current = 0;

        /* GroupDispalyFragment 출력 */
        getSupportFragmentManager().beginTransaction().replace(R.id.layout_fragment, fragments[0]).commit();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        redirectMainActivity();
    }

    public Trip getTrip() { return trip; }

    public boolean isLeader() { return isLeader; }

    public boolean isAccessible() {
        Long kakao_id = GlobalApplication.getInstance().getTripManager().getUser().getKakao_id();
        boolean isEditor = trip.getGroup().getEditors().contains(kakao_id);

        if(isLeader || isEditor)
            return true;
        return false;
    }

    public void redirectFragment(int index) {
        switchButtonIcon(index);
        if (!isFinishing())
            getSupportFragmentManager().beginTransaction().replace(R.id.layout_fragment, fragments[index]).commit();
    }

    public void refreshFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().detach(fragment).attach(fragment).commit();
    }

    public void redirectMainActivity() {
        Intent intent = new Intent(TripActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void switchButtonIcon(int index) {
        buttons[current].setImageResource(images_inactive[current]);
        buttons[index].setImageResource(images_active[index]);
        current = index;
    }

    public class TripSocketIOReceiver extends BroadcastReceiver {

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
                case SocketIOService.EVENT_TYPE_SCHEDULE:
                    onScheduleReceived(intent);
                    break;
                case SocketIOService.EVENT_TYPE_NOTIFICATION:
                    onNotificationReceived(intent);
                    break;
                case SocketIOService.EVENT_TYPE_FOLLOWER:
                    onFollowerReceived(intent);
                    break;
            }
        }

        private void onErrorReceived(Intent intent) {
            try {
                JSONObject data = new JSONObject(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                GlobalApplication.getInstance().progressOff();

                switch (data.getInt("code")) {
                    case 0:
                        Toast.makeText(TripActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(TripActivity.this, "서버가 응답하지 않습니다.", Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void onScheduleReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);
            switch (sub_event) {
                case "create":
                    ((TripSchedulePagerFragment) fragments[0]).onCreateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "update":
                    ((TripSchedulePagerFragment) fragments[0]).onUpdateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "delete":
                    ((TripSchedulePagerFragment) fragments[0]).onDeleteReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "display":
                    ((TripSchedulePagerFragment) fragments[0]).onDisplayReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }

        private void onNotificationReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);
            switch (sub_event) {
                case "create":
                    ((TripNotificationFragment) fragments[1]).onCreateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "update":
                    ((TripNotificationFragment) fragments[1]).onUpdateReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "delete":
                    ((TripNotificationFragment) fragments[1]).onDeleteReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "display":
                    ((TripNotificationFragment) fragments[1]).onDisplayReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }

        private void onFollowerReceived(Intent intent) {
            String sub_event = intent.getStringExtra(SocketIOService.EXTRA_SUB_EVENT);
            switch (sub_event) {
                case "find":
                    ((TripFollowerFragment) fragments[2]).onFindReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "display":
                    ((TripFollowerFragment) fragments[2]).onDisplayReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "location":
                    ((TripMapFragment) fragments[3]).onLocationReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
                case "accessible":
                    ((TripFollowerFragment) fragments[2]).onAccessibleReceived(intent.getStringExtra(SocketIOService.EXTRA_DATA));
                    break;
            }
        }
    }
}
