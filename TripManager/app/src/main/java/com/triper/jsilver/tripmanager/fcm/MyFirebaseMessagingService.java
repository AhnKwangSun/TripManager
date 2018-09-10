package com.triper.jsilver.tripmanager.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.IntroActivity;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.login.LoginActivity;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.service.GPSService;

import java.util.Map;

/**
 * Created by JSilver on 2017-09-05.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data.containsKey("boost"))
            boostGPSService(Integer.parseInt(data.get("boost")));
        else
            showNotification(data.get("title"), Integer.parseInt(data.get("group_id")));
    }

    private void showNotification(String title, int group_id) {
        Intent intent = new Intent(this, IntroActivity.class);
        intent.putExtra("group_id", group_id);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

    private void boostGPSService(int boost) {
        /* 위치추적 서비스 등록 */
        Intent service = new Intent(this, GPSService.class);
        service.putExtra("boost", boost);
        startService(service);
    }
}
