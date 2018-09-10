package com.triper.jsilver.tripmanager.DataType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JSilver on 2017-09-13.
 */

public class Location {
    public double latitude;
    public double longitude;

    public Location() {
        this.latitude = 0.0f;
        this.longitude = 0.0f;
    }

    public JSONObject toJSONObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("latitude", latitude);
            data.put("longitude", longitude);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return  data;
    }
}
