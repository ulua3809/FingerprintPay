package com.surcumference.fingerprint.plugin.inf;

import android.app.Activity;
import android.content.Context;

public interface IAppPlugin {
    int getVersionCode(Context context);

    void onActivityResumed(Activity activity);

    void onActivityCreated(Activity activity);

    void onActivityPaused(Activity activity);

    boolean getMockCurrentUser();
}
