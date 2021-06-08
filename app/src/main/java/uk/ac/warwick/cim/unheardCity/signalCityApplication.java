package uk.ac.warwick.cim.unheardCity;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Create the application context
 */

public class signalCityApplication extends Application {

    private static Context mContext;

    public void onCreate() {
        super.onCreate();
        Log.i("APP", "Create context");
        mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return mContext;
    }
}
