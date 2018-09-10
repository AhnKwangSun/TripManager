package com.triper.jsilver.tripmanager.Trip;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.triper.jsilver.tripmanager.R;

/**
 * Created by JSilver on 2017-09-29.
 */

public class NotificationUpdateDialog extends Dialog {
    private String content;

    private EditText edit_content;

    private View.OnClickListener listener;

    public NotificationUpdateDialog(Context context, String content) {
        super(context);
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notification_update);

        edit_content = (EditText) findViewById(R.id.edit_content);
        edit_content.setText(content);
        edit_content.setSelection(content.length());

        Button btn_update = (Button) findViewById(R.id.btn_update);
        btn_update.setOnClickListener(listener);
    }

    public String getContent() { return edit_content.getText().toString(); }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_content.getWindowToken(), 0);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }
}
