package com.kunalfarmah.eyecarereminder;

import android.app.Application;
import android.util.Log;

public class EyeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread arg0, Throwable arg1) {
                Log.e("Error", arg1.getCause().getMessage());
            }
        });
    }
}
