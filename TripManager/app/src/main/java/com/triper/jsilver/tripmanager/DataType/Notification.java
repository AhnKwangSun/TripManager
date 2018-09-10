package com.triper.jsilver.tripmanager.DataType;

import java.util.Date;

/**
 * Created by JSilver on 2017-09-14.
 */

public class Notification {
    private int id;
    private Date date;
    private String content;
    private Member writer;

    public Notification(int id, Date date, String content, Member writer) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.writer = writer;
    }

    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }

    public Member getWriter() { return writer; }
}
