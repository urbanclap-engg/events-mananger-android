package com.urbanclap.analytics_client_manager_android;

import android.support.annotation.NonNull;

/**
 * @author : Adnaan 'Zohran' Ahmed <adnaanahmed@urbanclap.com>
 * @version : 1.0.0
 * @since : 07 Dec 2017 12:24 PM
 */


public interface OnErrorCallback {
    void onError(@NonNull String errorMessage);
}
