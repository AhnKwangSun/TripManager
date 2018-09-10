package com.triper.jsilver.tripmanager.Trip;

import android.app.Activity;
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
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-29.
 */

public class ScheduleCreateDialog extends Dialog {
    private TextView txt_time;
    private EditText edit_content;

    private View.OnClickListener listener;

    public ScheduleCreateDialog(Context context) {
        super(context);
        setOwnerActivity((Activity) context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_schedule_create);

        txt_time = (TextView) findViewById(R.id.txt_time);
        edit_content = (EditText) findViewById(R.id.edit_content);

        final Calendar now = Calendar.getInstance();
        txt_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog dpd = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                        txt_time.setText(new SimpleDateFormat("HH:mm").format(new Date(0, 0, 0, hourOfDay, minute)));
                        now.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        now.set(Calendar.MINUTE, minute);
                    }
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
                dpd.show(getOwnerActivity().getFragmentManager(), "start_date");
            }
        });

        Button btn_create = (Button) findViewById(R.id.btn_create);
        btn_create.setOnClickListener(listener);
    }

    public String getTime() { return txt_time.getText().toString(); }

    public String getContent() { return edit_content.getText().toString(); }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_content.getWindowToken(), 0);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }
}
