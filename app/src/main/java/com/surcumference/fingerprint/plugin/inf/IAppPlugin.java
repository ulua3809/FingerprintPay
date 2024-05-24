package com.surcumference.fingerprint.plugin.inf;

import android.app.Application;
import android.content.Context;


public interface IAppPlugin extends Application.ActivityLifecycleCallbacks {
    int getVersionCode(Context context);


    boolean getMockCurrentUser();
}
