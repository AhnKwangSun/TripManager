package com.triper.jsilver.tripmanager.Member;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.triper.jsilver.tripmanager.R;

/**
 * Created by JSilver on 2017-09-29.
 */

public class MemberUnregisterDialog extends Dialog {
    private View.OnClickListener listener;

    public MemberUnregisterDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_member_unregister);

        Button btn_yes = (Button) findViewById(R.id.btn_yes);
        Button btn_no = (Button) findViewById(R.id.btn_no);
        btn_yes.setOnClickListener(listener);
        btn_no.setOnClickListener(listener);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }
}
