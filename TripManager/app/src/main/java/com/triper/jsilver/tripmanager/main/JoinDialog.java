package com.triper.jsilver.tripmanager.main;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.triper.jsilver.tripmanager.R;

/**
 * Created by JSilver on 2017-09-29.
 */

public class JoinDialog extends Dialog {
    private String name;

    private EditText edit_password;
    private View.OnClickListener listener;

    public JoinDialog( Context context, String name) {
        super(context);
        this.name = name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_group_join);

        edit_password = (EditText) findViewById(R.id.edit_password);
        TextView txt_name = (TextView) findViewById(R.id.txt_name);
        txt_name.setText(name);

        Button btn_join = (Button) findViewById(R.id.btn_join);
        btn_join.setOnClickListener(listener);
    }

    public String getPassword() {
        return edit_password.getText().toString();
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_password.getWindowToken(), 0);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }
}
