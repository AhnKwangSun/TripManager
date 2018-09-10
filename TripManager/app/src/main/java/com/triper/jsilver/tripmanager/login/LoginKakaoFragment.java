package com.triper.jsilver.tripmanager.login;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.labo.kaji.fragmentanimations.MoveAnimation;
import com.triper.jsilver.tripmanager.DataType.Member;
import com.triper.jsilver.tripmanager.DataType.TripManager;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JSilver on 2017-09-06.
 */

public class LoginKakaoFragment extends Fragment {
    private LoginActivity parent;

    private SessionCallback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = (LoginActivity)getActivity();

        /* 카카오 세션이 열릴 때 수행할 Callback 함수 지정 (카카오 로그인 버튼이 눌르면 수행) */
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_kakao, container, false);
        return view;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return MoveAnimation.create(MoveAnimation.LEFT, enter, 600);
    }

    /* Fragment가 파괴될 때 Callback을 제거함 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }

    private void requestMemberCheck() {
        /* 서버에 회원등록 정보를 보냄 */
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_MEMBER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "check");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", UserProfile.loadFromCache().getId());
            data.put("fcm_token", FirebaseInstanceId.getInstance().getToken());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA,  data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    public void onCheckReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            /* 회원이 이미 가입되어있으면 새로운 정보로 갱신 후 이동 */
            if(result) {
                TripManager tripManager = GlobalApplication.getInstance().getTripManager();
                tripManager.setUser(new Member(UserProfile.loadFromCache().getId(), json.getString("name"), GlobalApplication.getInstance().getBitmapFromString(json.getString("member_picture")), json.getString("phone"), FirebaseInstanceId.getInstance().getToken()));
                tripManager.saveUserData(getContext().getFilesDir(), "user.dat");

                startActivity(new Intent(parent, MainActivity.class).putExtra("group_id", parent.getIntent().getIntExtra("group_id", -1)));
                parent.finish();
            }
            else
                parent.redirectFragment(1);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class SessionCallback implements ISessionCallback {

        /* 세션이 정상적으로 열린 경우 */
        @Override
        public void onSessionOpened() {
            UserManagement.requestMe(new MeResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    Log.e("[ERROR]", "requestMe.onFailure : " + errorResult);
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Log.e("[ERROR]", "requestMe.onSessionClosed : " + errorResult);
                }

                @Override
                public void onNotSignedUp() {
                    Log.e("[ERROR]", "requestMe.onNotSignedUp");
                }

                @Override
                public void onSuccess(UserProfile result) {
                    Log.e("[USER PROFILE]", result.toString());
                    requestMemberCheck();
                }
            });
        }

        /* 세션을 여는데 실패한 경우 */
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            Log.e("[ERROR]", "Session Fail Error is " + exception.getMessage().toString());
        }
    }
}
