package com.urbanclap.analytics_client_manager_android;

import java.util.HashMap;

/**
 * Created by kanavarora on 10/06/16.
 */
public class ChannelConfig {

    private Integer csvFile;
    private AnalyticsClientInterface channelClient;

    public ChannelConfig(Integer csvFile, AnalyticsClientInterface channelClient) {
        this.csvFile = csvFile;
        this.channelClient = channelClient;
    }

    public Integer getCsvFile() {
        return this.csvFile;
    }

    public AnalyticsClientInterface getChannelClient() {
        return this.channelClient;
    }
}
