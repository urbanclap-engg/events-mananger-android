package com.urbanclap.analytics_client_manager_android;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsClientManager {
    private static final String LOG_TAG = "AnalyticsClientManager";
    private static final String KEYWORD_DEV_TO_PROVIDE = "devToProvide";

    private static AnalyticsClientManager m_instance;
    private static boolean m_enableStrictKeyValidation;
    private static boolean m_enableAlertOnError;

    static {
        m_instance = new AnalyticsClientManager();
    }

    private HashMap<String, AnalyticsClientInterface> channelClients;
    private HashMap<String, HashMap<String, CSVProperties>> channelTriggerMappings;
    private HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings;
    private Context context;

    private OnErrorCallback onErrorCallback = null;

    protected AnalyticsClientManager() {
    }

    public static void initialize(HashMap<String, ChannelConfig> channelConfigs,
                                  HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings,
                                  Context c) {
        AnalyticsClientManager.initialize(channelConfigs, triggerEventMappings, c, true, true);
    }

    public static void initialize(HashMap<String, ChannelConfig> channelConfigs,
                                  HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings,
                                  Context c,
                                  boolean enableStrictKeyValidation,
                                  boolean enableAlertOnError) {
        m_enableStrictKeyValidation = enableStrictKeyValidation;
        m_enableAlertOnError = enableAlertOnError;
        m_instance.init(channelConfigs, triggerEventMappings, c);
    }

    protected static void logError(String errorString) {
        if (m_enableAlertOnError && m_instance != null) {
            m_instance.handleError(errorString);
        }
        Log.e(LOG_TAG, errorString);
    }

    protected void handleError(String errorString) {
        // Clients can override this however they want
        if (this.context != null) {
            Toast.makeText(this.context, errorString, Toast.LENGTH_SHORT).show();
        }

        if (onErrorCallback != null)
            onErrorCallback.onError(errorString);
    }

    private static void validateMissingChannelsForTriggers(String channel,
                                                           HashMap<String, CSVProperties> channelTriggers,
                                                           HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings) {
        if (channelTriggers == null) {
            return;
        }
        for (String trigger : channelTriggers.keySet()) {
            if ((triggerEventMappings.get(trigger) == null) ||
                    (triggerEventMappings.get(trigger).get(channel) == null)) {
                AnalyticsClientManager.logError("event missing for trigger: " + trigger +
                        " for channel: " + channel);
            }
        }
    }

    public static void triggerEvent(String trigger, JSONObject properties) {
        if (m_instance != null) {
            m_instance.triggerEventInternal(trigger, properties);
        } else {
            logError("AnalyticsClientManager not initialized");
        }
    }

    public static boolean setValueInMultiLevelObj(JSONObject props, String multiLevelKey, Object val) {
        String[] levelKeys = multiLevelKey.split("\\.");
        JSONObject currentJSONObj = props;
        for (int i = 0; i < levelKeys.length; i++) {
            String levelKey = levelKeys[i];
            if (levelKey.length() <= 0) {
                return false;
            } else if (i == levelKeys.length - 1) {
                try {
                    currentJSONObj.putOpt(levelKey, val);
                    return true;
                } catch (JSONException e) {
                    AnalyticsClientManager.logError("Error setting val: " + val +
                            " for key: " + multiLevelKey + " in props: " + props);
                    return false;
                }
            } else {
                if (currentJSONObj.opt(levelKey) == null) {
                    // key not there so create it first
                    try {
                        currentJSONObj.put(levelKey, new JSONObject());
                    } catch (JSONException e) {
                        AnalyticsClientManager.logError("Error setting val: " + val +
                                " for key: " + multiLevelKey + " in props: " + props);
                        return false;
                    }
                }
                // ensured key is present
                if (currentJSONObj.optJSONObject(levelKey) == null) {
                    AnalyticsClientManager.logError("Error setting val: " + val +
                            " for key: " + multiLevelKey + " in props: " + props);
                    return false;
                } else {
                    currentJSONObj = currentJSONObj.optJSONObject(levelKey);
                }
            }
        }
        return false;
    }

    public void setOnErrorCallback(OnErrorCallback onErrorCallback) {
        this.onErrorCallback = onErrorCallback;
    }

    protected void init(HashMap<String, ChannelConfig> channelConfigs,
                        HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings,
                        Context c) {
        this.channelClients = new HashMap<String, AnalyticsClientInterface>();
        this.channelTriggerMappings = new HashMap<>();
        this.context = c;

        for (Map.Entry<String, ChannelConfig> entry : channelConfigs.entrySet()) {
            String channel = entry.getKey();
            ChannelConfig channelConfig = entry.getValue();
            int csvFile = channelConfig.getCsvFile();
            if (csvFile < 0) {
                AnalyticsClientManager.logError("missing csv file for config of channel: " + channel);
                continue;
            }

            AnalyticsClientInterface channelClient = channelConfig.getChannelClient();
            if (channelClient == null) {
                AnalyticsClientManager.logError("client in config does not implement AnalyticsClientInterface for channel: " + channel);
                continue;
            }

            HashMap<String, CSVProperties> channelTriggers = AnalyticsUtility.parseCSVFileIntoAnalyticsEvents(this.context.getResources(), csvFile);
            this.channelTriggerMappings.put(channel, channelTriggers);
            AnalyticsClientManager.validateMissingChannelsForTriggers(channel, channelTriggers, triggerEventMappings);

            channelClient.setup(this.context);
            this.channelClients.put(channel, channelClient);
        }
        this.triggerEventMappings = triggerEventMappings;
    }

    private void triggerEventInternal(String trigger, JSONObject props) {
        if (this.triggerEventMappings.get(trigger) == null) {
            AnalyticsClientManager.logError("Trigger not present in trigger mappings provided: " + trigger);
            return;
        }

        for (Map.Entry<String, DevOverrides> entry : this.triggerEventMappings.get(trigger).entrySet()) {
            String channel = entry.getKey();
            DevOverrides devOverrides = entry.getValue();

            AnalyticsClientInterface channelClient = this.channelClients.get(channel);
            if (channelClient == null) {
                AnalyticsClientManager.logError("Channel Client object not present for channel: " + channel +
                        "trigger: " + trigger);
                continue;
            }

            HashMap<String, CSVProperties> channelSpecificTriggers = this.channelTriggerMappings.get(channel);
            if (channelSpecificTriggers == null) {
                AnalyticsClientManager.logError("channel: " + channel + " doesn't have a csv file for defining triggers");
                continue;
            }

            CSVProperties csvProperties = channelSpecificTriggers.get(trigger);
            if (csvProperties == null) {
                AnalyticsClientManager.logError("Trigger: " + trigger +
                        " not present in csv file for channel: " + channel);
                return;
            }

            JSONObject eventProperties = new JSONObject();
            boolean isMissingKey = false;
            for (Map.Entry<String, String> csvPropertyEntry : csvProperties.entrySet()) {
                String eventKey = csvPropertyEntry.getKey();
                String eventValue = csvPropertyEntry.getValue();

                if (eventValue.equals(KEYWORD_DEV_TO_PROVIDE)) {
                    // to be provided by dev
                    String propsKey = devOverrides.get(eventKey);
                    if (propsKey == null) {
                        isMissingKey = true;
                        AnalyticsClientManager.logError("eventKey: " + eventKey +
                                " needs to be overriden for channel: " + channel +
                                " and trigger: " + trigger);
                        continue;
                    }
                    if (props == null) {
                        isMissingKey = true;
                        AnalyticsClientManager.logError("Trigger: " + trigger +
                                " channel: " + channel + " requires eventKey: " + eventKey +
                                " to be overriden, but called with empty props"
                        );
                        continue;
                    }
                    Object val = getValueInMultiLevelObj(props, propsKey);
                    if (val == null) {
                        isMissingKey = true;
                        AnalyticsClientManager.logError("path for key: " + propsKey +
                                " not present in props: " + props +
                                " for channel: " + channel + " trigger: " + trigger);
                        continue;
                    }

                    if (!AnalyticsClientManager.setValueInMultiLevelObj(eventProperties, eventKey, val)) {
                        isMissingKey = true;
                        AnalyticsClientManager.logError("unable to set value for key: " + eventKey +
                                " in eventProps " + eventProperties);
                    }
                } else {
                    // non dev provided
                    if (!AnalyticsClientManager.setValueInMultiLevelObj(eventProperties, eventKey, eventValue)) {
                        isMissingKey = true;
                        AnalyticsClientManager.logError("unable to set csv value for key: " + eventKey +
                                " in eventProps " + eventProperties + " for channel: " + channel +
                                " and trigger: " + trigger);
                    }
                }
            }
            if (!isMissingKey || !AnalyticsClientManager.m_enableStrictKeyValidation) {
                channelClient.sendEvent(eventProperties);
            }
        }
    }

    private Object getValueInMultiLevelObj(JSONObject propsObj, String multiLevelKey) {
        String[] levelKeys = multiLevelKey.split("\\.");
        Object currObject = propsObj;
        for (String levelKey : levelKeys) {
            if ((currObject instanceof JSONObject) && (((JSONObject) currObject).opt(levelKey) != null)) {
                currObject = ((JSONObject) currObject).opt(levelKey);
            } else {
                return null;
            }
        }
        return currObject;
    }

}
