# Analytics Client Manager

Analytics client manager is a csv based events library which can be used to set up Analytics for your application. 

## Getting Started
---
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

   In your app/module `build.grade` add the following repo: 
```sh 
maven { url "https://dl.bintray.com/uc-engg/maven" }
```
  In dependencies add:
```sh
api 'com.urbanclap:analytics-client-manager:0.0.1'
```

## PreSetup
Once you have imported the library in your project lets get started with the csv
-  Figure out all the channels with which you want to send events. Eg, `UCServer`, `Mixpanel`, `Facebook`, etc.

-  For each channel create a csv that lists
    all the triggers that the channel is supporting and what keys/values to expect for that trigger. For example, for channel `UCServer`, create a `UCServerEvents.csv`, 
    which could look something like this:
[RAW CSV](https://raw.githubusercontent.com/stopdrake/events-mananger-android/master/sample.csv)

| trigger | schema_type | event.page | event.section | event.action |
| ------ | ------ | ------ | ------ | ------ |
| homeLoad | event| home || load |
| assist_clicked | event | devToProvide || click |

  This is parsed as follows:
    For every row in the csv, for the trigger it lists which keys need to be sent to that channel. Most of the values
    for these can be provided in the csv itself. For values that need to be provided
    by dev, just add the keyword `devToProvide` there and this will ensure the dev
    will have to take care of that value.
    Also the dot (.) separator is used in keys to introduce heirarchy.

# Setup
-  For each channel, define a class that conforms to the interface AnalyticsClientInterface.
    Eg. Define a class UCServerEventsManager. Implement the two methods.
    Example for Mixpanel, create a MixpanelEventsManager, and for the implementation
    of methods, just delegate to the Mixpanel SDK.
    
-  In code, define a class something like `ChannelConfigMappings.java` and define the channel configs structure here. Its a mapping of a channel to its config.
```sh
public class ChannelConfigMappings {
    public static final UCServerChannelClient ucServerChannelClient = new UCServerChannelClient();
    private static final MixPanelChannelClient mixPanelChannelClient = new MixPanelChannelClient();

    public static final HashMap<String, ChannelConfig> channelConfigs =
            new HashMap<String, ChannelConfig>() {{
                put("ucserver",
                        new ChannelConfig(R.raw.ucserverevents_android, ucServerChannelClient));
                put("mixpanel",
                        new ChannelConfig(R.raw.mixpanel_events, cleverTapChannelClient));
            }}; 
        }
```

Define the triggerMappings structure. Maps each trigger from triggername to 
    all the channels that trigger is supporting, and for each channel, the devProvided
    values.
    Example:
    Create a trigger as follows
```sh    
public static final String HomeLoadTrigger = "homeLoad";
```
Now create a Map of all the developer overrides required by the above trigger
```sh
public static final DevOverrides triggerLocalityValueOverrides = new DevOverrides() {{
        put("customer_id", "customer_id"); 
        put("event_value", "event_value"); // here event value maybe the page load time
}};
```
Now connect the developer override with their respective channels
```sh
private static final HashMap<String, DevOverrides> homeLoadMapping = new HashMap<String, DevOverrides>() {{
        put("ucserver", UCServerDevOverrides.triggerLocalityValueOverrides);
        put("mixpanel", UCServerDevOverrides.triggerLocalityValueOverrides)
    }};
```
In the above example, we have set up that for home load trigger, its supposed to send events to ucserver and mixpanel.

Finally connect the trigger to their respective mappings

```sh
public static final HashMap<String, HashMap<String, DevOverrides>> triggerEventMappings =
            new HashMap<String, HashMap<String, DevOverrides>>() {{

                put(HomeLoadTrigger, homeLoadMapping);
                put(Trigger2 , trigger2Mapping);
                put(Trigger3 , trigger3Mapping);
    }};
```

The `triggerEventMappings` and `ChannelConfigMappings.channelConfigs` will be supplied to `AnalyticsClientManager.init()`

## Usage

Now that we have setup our system lets get to the interesting part i.e. sending events.
For our above example to fire the home load event just write the following in your code

```sh
AnalyticsClientManager.triggerEvent(HomeLoadTrigger, new JSONObject("{
    'customer_id': '1234',
    'event_value': '143'
}");
```
