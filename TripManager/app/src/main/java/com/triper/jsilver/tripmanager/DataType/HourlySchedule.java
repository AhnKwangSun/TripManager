package com.triper.jsilver.tripmanager.DataType;

import java.util.Date;

/**
 * Created by JSilver on 2017-09-14.
 */

public class HourlySchedule {
    private int id;
    private Date time;
    private String content;

    public HourlySchedule(int id, Date time, String content) {
        this.id = id;
        this.time = time;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public Date getTime() {
        return time;
    }

    public String getContent() {
        return content;
    }
}
