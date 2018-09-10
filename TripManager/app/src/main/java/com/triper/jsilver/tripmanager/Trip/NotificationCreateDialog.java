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

public class NotificationCreateDialog extends Dialog {
    private EditText edit_content;

    private View.OnClickListener listener;

    public NotificationCreateDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notification_create);

        edit_content = (EditText) findViewById(R.id.edit_content);

        Button btn_create = (Button) findViewById(R.id.btn_create);
        btn_create.setOnClickListener(listener);
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
