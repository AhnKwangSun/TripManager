package com.triper.jsilver.tripmanager.DataType;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by JSilver on 2017-09-14.
 */

public class Schedule {
    private Date date;
    private ArrayList<HourlySchedule> hourlySchedules;

    public Schedule(Date date) {
        this.date = date;
        hourlySchedules = new ArrayList<>();
    }

    public Date getDate() {
        return date;
    }

    public ArrayList<HourlySchedule> getHourlySchedules() {
        return hourlySchedules;
    }

    public void createHourlySchedule(int id, Date date, String content) {
        hourlySchedules.add(new HourlySchedule(id, date, content));
    }
}
