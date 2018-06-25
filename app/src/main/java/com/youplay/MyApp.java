package com.youplay;

import android.app.Application;

/**
 * Created by tan on 02/01/17.
 **/

public class MyApp extends Application {

    private NetComponent mNetComponent;
    private boolean appUpdated = false;
    private boolean videoServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager preferenceManager = new PreferenceManager(this);
        if (BuildConfig.VERSION_CODE != preferenceManager.getCurrentAppVersion()
                || preferenceManager.getCurrentAppVersion() == -1) {
            appUpdated = true;
            preferenceManager.setCurrentAppVersion(BuildConfig.VERSION_CODE);
        }

        // Dagger%COMPONENT_NAME%
        mNetComponent = DaggerNetComponent.builder()
                // list of modules that are part of this component need to be created here too
                .appModule(new AppModule(this)) // This also corresponds to the name of your module: %component_name%Module
                .netModule(new NetModule(BuildConfig.base_url))
                .build();

    }

    public NetComponent getNetComponent() {
        return mNetComponent;
    }

    public boolean isAppUpdated() {
        return appUpdated;
    }

    public boolean isVideoServiceRunning() {
        return videoServiceRunning;
    }

    public void setVideoServiceRunning(boolean videoServiceRunning) {
        this.videoServiceRunning = videoServiceRunning;
    }
}
