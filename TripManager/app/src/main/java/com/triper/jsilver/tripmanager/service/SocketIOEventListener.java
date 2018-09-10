package com.triper.jsilver.tripmanager.service;

import com.github.nkzawa.emitter.Emitter;

/**
 * Created by JSilver on 2017-09-02.
 */

public class SocketIOEventListener extends Emitter implements Emitter.Listener {
    private String mEvent;
    private Listener mListener;
    
    public SocketIOEventListener(String mEvent, Listener mListener) {
        this.mEvent = mEvent;
        this.mListener = mListener;
    }
    
    @Override
    public void call(Object... args) {
        if(this.mListener != null)
            this.mListener.onEventCall(mEvent, args);
    }
    
    public interface Listener {
        void onEventCall(String event, Object... args);
    }
}
