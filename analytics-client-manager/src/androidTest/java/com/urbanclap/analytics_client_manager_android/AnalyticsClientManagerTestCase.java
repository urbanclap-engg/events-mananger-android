package com.urbanclap.analytics_client_manager_android;

import android.test.AndroidTestCase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kanavarora on 13/06/16.
 */
public class AnalyticsClientManagerTestCase extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static void testMultiLevelSet() {
        JSONObject event = new JSONObject();
        AnalyticsClientManager.setValueInMultiLevelObj(event, "key1", "val1");
        assertTrue(event.optString("key1").equals("val1"));

        AnalyticsClientManager.setValueInMultiLevelObj(event, "key1.key2", "val2"); // shouldnt do anything, not possible.
        assertTrue(event.optString("key1").equals("val1"));

        AnalyticsClientManager.setValueInMultiLevelObj(event, "key1.key2.key3", "val2"); // shouldnt do anything, not possible.
        assertTrue(event.optString("key1").equals("val1"));

        AnalyticsClientManager.setValueInMultiLevelObj(event, "key2.key3", "val3");
        assertTrue(event.optJSONObject("key2").optString("key3").equals("val3"));

        AnalyticsClientManager.setValueInMultiLevelObj(event, "key2.key4", "val4");
        assertTrue(event.optJSONObject("key2").optString("key3").equals("val3"));
        assertTrue(event.optJSONObject("key2").optString("key4").equals("val4"));
    }

    public void testBasic () {
        final TestChannel tc = new TestChannel();
        HashMap<String, ChannelConfig> channelConfigs = new HashMap<String, ChannelConfig>() {{
            put("testChannel", new ChannelConfig() {{
                put("csvFile", R.raw.test_channel_events);
                put("client", tc);
            }});
        }};
        HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings = new HashMap<String, HashMap<String, DevOverrides>>() {{
            put("trigger1", new HashMap<String, DevOverrides>(){{
                put("testChannel", new DevOverrides());
            }});

            put("trigger2", new HashMap<String, DevOverrides>(){{
                put("testChannel", new DevOverrides(){{
                    put("eventKey2", "devOverridenKey2");
                }});
            }});

            put("trigger3", new HashMap<String, DevOverrides>(){{
                put("testChannel", new DevOverrides(){{
                    put("keyParent.keyChild", "devOverridenParent.devOverridenChild");
                }});
            }});
        }};


        AnalyticsClientManager.initialize(channelConfigs, triggerEventMappings, getContext().getResources(), true, true);

        AnalyticsClientManager.triggerEvent("trigger1", null);
        assertTrue(tc.events.size() == 1);

        // this should have failed, so no events added, console should alert
        AnalyticsClientManager.triggerEvent("trigger2", null);
        assertTrue(tc.events.size() == 1);

        JSONObject event = tc.events.get(0);
        assertTrue(event.optString("eventKey1").equals("val1"));
        assertTrue(event.optJSONObject("keyParent").optString("keyChild").equals("childVal"));


        AnalyticsClientManager.triggerEvent("trigger2", new JSONObject(new HashMap(){{
            put("devOverridenKey2", "devVal2");
        }}));
        assertTrue(tc.events.size() == 2);
        HashMap<Object, Object> resEvent = new HashMap(){{
            put("eventKey1", "val2");
            put("eventKey2", "devVal2");
        }};
        try {
            assertTrue(JsonHelper.toMap(tc.events.get(1)).equals(resEvent));
        } catch (Exception e) {
            assertTrue(1==0);
        }

        tc.events.clear();
        assertTrue(tc.events.size() == 0);

        // testing other data types (int)
        AnalyticsClientManager.triggerEvent("trigger2", new JSONObject(new HashMap(){{
            put("devOverridenKey2", 3);}}));

        assertTrue(tc.events.size() == 1);
        HashMap<Object, Object> resEvent3 = new HashMap(){{
            put("eventKey1", "val2");
            put("eventKey2", 3);
        }};
        try {
            assertTrue(JsonHelper.toMap(tc.events.get(0)).equals(resEvent3));
        } catch (Exception e) {
            assertTrue(1==0);
        }

        // testing map type
        AnalyticsClientManager.triggerEvent("trigger2", new JSONObject(new HashMap(){{
            put("devOverridenKey2", new HashMap(){{
                put("key", "val");}});
        }}));
        assertTrue(tc.events.size() == 2);
        HashMap<Object, Object> resEvent4 = new HashMap(){{
            put("eventKey1", "val2");
            put("eventKey2", new HashMap(){{
                put("key", "val");}});
        }};
        try {
            assertTrue(JsonHelper.toMap(tc.events.get(1)).equals(resEvent4));
        } catch (Exception e) {
            assertTrue(1==0);
        }

        // flush events
        tc.events.clear();
        assertTrue(tc.events.size() == 0);


        // testing multilevel
        AnalyticsClientManager.triggerEvent("trigger3", new JSONObject(new HashMap(){{
            put("" , 3);
        }}));
        assertTrue(tc.events.size() == 0); // should console error.

        AnalyticsClientManager.triggerEvent("trigger3", new JSONObject(new HashMap(){{
            put("devOverridenParent" , 3);
        }}));
        assertTrue(tc.events.size() == 0); // should console error

        AnalyticsClientManager.triggerEvent("trigger3", new JSONObject(new HashMap(){{
            put("devOverridenParent" , new HashMap(){{
                put("devOverridenChild", 3);}});
        }}));
        assertTrue(tc.events.size() == 1);
        HashMap<Object, Object> resEvent5 = new HashMap(){{
            put("keyParent", new HashMap(){{
                put("keyChild", 3);}});
        }};

        try {
            assertTrue(JsonHelper.toMap(tc.events.get(0)).equals(resEvent5));
        } catch (Exception e) {
            assertTrue(1==0);
        }
    }

    public static class TestChannel implements AnalyticsClientInterface {
        public ArrayList<JSONObject> events;
        @Override
        public void setup() {
            events = new ArrayList<>();
        }

        @Override
        public void sendEvent(JSONObject properties) {
            events.add(properties);
        }

    }
}
