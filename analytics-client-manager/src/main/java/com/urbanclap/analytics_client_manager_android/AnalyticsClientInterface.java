package com.urbanclap.analytics_client_manager_android;

import android.content.Context;

import org.json.*;
/**
 * Created by kanavarora on 10/06/16.
 */
public interface AnalyticsClientInterface {

    void setup(Context context);
    void sendEvent(JSONObject properties);
}
