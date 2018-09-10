package com.triper.jsilver.tripmanager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.support.v7.app.AppCompatDialog;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.auth.KakaoSDK;
import com.triper.jsilver.tripmanager.DataType.TripManager;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-05.
 */

public class GlobalApplication extends Application {
    public static final int PICK_FROM_ALBUM = 1;
    public static final int CROP_FROM_IMAGE = 2;

    private static GlobalApplication instance = null;
    private static volatile Activity currentActivity = null;

    private TripManager tripManager;
    private Boolean offineMode;

    private AppCompatDialog progressDialog;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        KakaoSDK.init(new KakaoSDKAdapter());

        tripManager = new TripManager();
        offineMode = false;

        getHashKey();
        convertHashKey();
    }

    /**
     * singleton Application 객체를 얻는다.
     * @return singleton Application 객체
     */
    public static GlobalApplication getInstance() {
        if(instance == null)
            throw new IllegalStateException("this application does not inherit GlobalApplication");
        return instance;
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(Activity currentActivity) {
        GlobalApplication.currentActivity = currentActivity;
    }

    public TripManager getTripManager() { return tripManager; }

    public Boolean getOffineMode() {
        return offineMode;
    }

    public void setOffineMode(Boolean offine) {
        offineMode = offine;
    }

    public void progressOn(Activity activity, String message) {
        if (activity == null || activity.isFinishing() || progressDialog != null)
            return;

        progressDialog = new AppCompatDialog(activity);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.activity_loading);
        progressDialog.show();
    }

    public void progressOff() {
        if(progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public boolean checkLocationServices() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean checkIsTracing(Date start_date, Date end_date) {
        Calendar now = Calendar.getInstance();
        if (now.getTime().after(start_date) && now.getTime().before(end_date))
            return true;
        return false;
    }


    /* Bitmap 이미지를 String으로 변환 */
    public String getStringFromBitmap(Bitmap bitmap) {
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }

    /* String을 Bitmap 이미지로 변환 */
    public Bitmap getBitmapFromString(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

    private void getHashKey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA");
                messageDigest.update(signature.toByteArray());
                Log.e("[HASH KEY]", Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT));
            }
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void convertHashKey() {
        byte[] sha1 = {
                (byte) 0xE6, (byte) 0xBC, 0x47, (byte) 0xD3, 0x10,
                0x54, 0x59, (byte) 0xFD, (byte) 0xD5, (byte) 0x9F,
                0x37, (byte) 0xDB, 0x6C, 0x02, 0x50, (byte) 0x95,
                (byte) 0x92, (byte) 0xFC, 0x1D, 0x68
        };

        String mEncodedToString = Base64.encodeToString(sha1, Base64.NO_WRAP);
        Log.e("[HASH]", mEncodedToString);
    }
}
