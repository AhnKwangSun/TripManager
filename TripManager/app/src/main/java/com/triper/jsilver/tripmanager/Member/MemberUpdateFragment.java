package com.triper.jsilver.tripmanager.Member;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.util.helper.log.Logger;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.triper.jsilver.tripmanager.DataType.Member;
import com.triper.jsilver.tripmanager.GlobalApplication;
import com.triper.jsilver.tripmanager.R;
import com.triper.jsilver.tripmanager.Trip.TripActivity;
import com.triper.jsilver.tripmanager.login.LoginActivity;
import com.triper.jsilver.tripmanager.login.LoginKakaoFragment;
import com.triper.jsilver.tripmanager.main.MainActivity;
import com.triper.jsilver.tripmanager.service.SocketIOService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by JSilver on 2017-10-07.
 */

public class MemberUpdateFragment extends Fragment {
    private Activity parent;

    private Member member;

    private CircularImageView img_picture;
    private EditText edit_name;
    private EditText edit_phone;

    private Bitmap picture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parent = getActivity();
        member = (Member) GlobalApplication.getInstance().getTripManager().getUser().clone();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_member_update, container, false);

        img_picture = (CircularImageView) view.findViewById(R.id.img_picture);
        edit_name = (EditText) view.findViewById(R.id.edit_name);
        edit_phone = (EditText) view.findViewById(R.id.edit_phone);

        Button btn_update = (Button) view.findViewById(R.id.btn_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestMemberUpdate();
            }
        });

        Button btn_unregister = (Button) view.findViewById(R.id.btn_unregister);
        btn_unregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final MemberUnregisterDialog dialog = new MemberUnregisterDialog(parent);
                dialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch(view.getId()) {
                            case R.id.btn_yes:
                                requestMemberUnregister();
                                break;
                            case R.id.btn_no:
                                dialog.dismiss();
                                break;
                        }
                    }
                });
                dialog.show();
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

        if(member.getMember_picture() != null) {
            img_picture.setImageBitmap(member.getMember_picture());
            picture = member.getMember_picture();
        }
        edit_name.setText(member.getName().toString());
        edit_phone.setText(member.getPhone().toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        member.setName(edit_name.getText().toString());
        member.setPhone(edit_phone.getText().toString());
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
                    member.setMember_picture(picture);
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
        String phone = edit_phone.getText().toString();
        if (phone.length() < 13 || phone.charAt(3) != '-' || phone.charAt(8) != '-') {
            Toast.makeText(parent, "전화번호 형식이 맞지 않습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void requestMemberUpdate() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_name.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(edit_phone.getWindowToken(), 0);

        if(!validateInput())
            return;

        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_MEMBER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "update");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", member.getKakao_id());
            data.put("name", edit_name.getText().toString());
            data.put("member_picture", (picture == null) ? null : GlobalApplication.getInstance().getStringFromBitmap(picture));
            data.put("phone", edit_phone.getText().toString());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        service.putExtra(SocketIOService.EXTRA_DATA, data.toString());
        parent.startService(service);

        GlobalApplication.getInstance().progressOn(parent, "loading");
    }

    private void requestMemberUnregister() {
        /* 서버에 그룹 생성 정보를 보냄 */
        Intent service = new Intent(parent, SocketIOService.class);
        service.putExtra(SocketIOService.EXTRA_EVENT_TYPE, SocketIOService.EVENT_TYPE_MEMBER);
        service.putExtra(SocketIOService.EXTRA_SUB_EVENT, "unregister");

        JSONObject data = new JSONObject();
        try {
            data.put("kakao_id", member.getKakao_id());
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
                Member user = GlobalApplication.getInstance().getTripManager().getUser();
                user.setName(edit_name.getText().toString());
                user.setMember_picture(picture);
                user.setPhone(edit_phone.getText().toString());
                GlobalApplication.getInstance().getTripManager().saveUserData(parent.getFilesDir(), "user.dat");
                picture = null;

                if(parent instanceof MainActivity)
                    ((MainActivity) parent).redirectFragment(0);
                else
                    ((TripActivity) parent).redirectFragment(0);
            }
            else
                Toast.makeText(parent, "CODE: " + json.getInt("code"), Toast.LENGTH_SHORT).show();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onUnregisterReceived(String data) {
        GlobalApplication.getInstance().progressOff();

        try {
            JSONObject json = new JSONObject(data);
            boolean result = (json.getInt("result") == 1) ? true : false;

            if (result) {
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

                Intent intent = new Intent(parent, LoginActivity.class);
                startActivity(intent);
                parent.finish();
            }
            else
                Toast.makeText(parent, "CODE: " + json.getInt("code"), Toast.LENGTH_SHORT).show();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
