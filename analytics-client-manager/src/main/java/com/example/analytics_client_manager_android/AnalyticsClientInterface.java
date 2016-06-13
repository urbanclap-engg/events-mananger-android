package com.example.analytics_client_manager_android;

import org.json.*;
/**
 * Created by kanavarora on 10/06/16.
 */
public interface AnalyticsClientInterface {

    void setup();
    void sendEvent(JSONObject properties);
}
