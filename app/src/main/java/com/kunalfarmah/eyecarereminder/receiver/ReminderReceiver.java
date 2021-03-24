package com.kunalfarmah.eyecarereminder.receiver;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;


import com.kunalfarmah.eyecarereminder.utils.Constants;
import com.kunalfarmah.eyecarereminder.service.NotificationService;
import com.kunalfarmah.eyecarereminder.service.TimerService;

import java.util.Calendar;

import static android.content.Context.POWER_SERVICE;

public class ReminderReceiver extends BroadcastReceiver {

    SharedPreferences sharedPreferences;
    boolean enabled;
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        sharedPreferences = context.getSharedPreferences(Constants.SETTINGS_APP_SETTINGS,Context.MODE_PRIVATE);
        enabled = sharedPreferences.getBoolean(Constants.WATER_REMINDER_ENABLED, false);

        if (intent.getStringExtra("CANCEL") != null) {
            context.stopService(new Intent(context, NotificationService.class));
        } else {
            // if water reminder was disabled, simply return
            if (!enabled)
                return;
            // oreo device restarts or re-installs the app with reminder enabled, restart the
            // service only
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long interval = sharedPreferences.getLong(Constants.ALARM_INTERVAL, 30);
                interval = interval * 60 * 1000;
                long start = sharedPreferences.getLong(Constants.START_TIME, 0);
                long curr = System.currentTimeMillis();
                long diff = curr - start;

                if (!isMyServiceRunning(TimerService.class, context)) {
                    // if the reminder period had past while phone off, start afresh
                    if (diff > interval) {
                        sharedPreferences.edit().putLong(Constants.START_TIME, System.currentTimeMillis()).apply();
                    }
                    Intent countDown = new Intent(context, TimerService.class);
                    context.startForegroundService(countDown);
                }
            }
            // in other cases, reset the alarm
            else {
                PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "MyApp::MyWakelockTag");
                wakeLock.acquire(60 /*10 minutes*/);
                int wakeHr = sharedPreferences.getInt(Constants.WAKE_HR, 11);
                int wakeMin = sharedPreferences.getInt(Constants.WAKE_MIN, 0);
                int sleepHr = sharedPreferences.getInt(Constants.SLEEP_HR, 23);
                int sleepMin = sharedPreferences.getInt(Constants.WAKE_MIN, 0);

                Calendar calNow = Calendar.getInstance();
                long currHr = calNow.get(Calendar.HOUR_OF_DAY);
                long currMin = calNow.get(Calendar.MINUTE);
                long currTime = (currHr * 60) + currMin;

                long wakeTime = (wakeHr * 60) + wakeMin;
                long sleepTime = (sleepHr * 60) + sleepMin;

                if (sleepHr <= wakeHr) {
                    sleepTime += 24 * 60;
                }
                if (currHr < wakeHr) {
                    currTime += 24 * 60;
                }

                long interval = sharedPreferences.getLong(Constants.ALARM_INTERVAL, 30);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Constants.WATER_REMINDER, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                if (wakeTime < currTime && currTime < sleepTime) {
                    Intent i = new Intent(context, NotificationService.class);
                    i.putExtra("TEXT", intent.getStringExtra("TEXT"));
                    // resetting alarm
                    calNow.add(Calendar.MINUTE, (int) interval);
                    if (Build.VERSION.SDK_INT >= 23) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calNow.getTimeInMillis(), pendingIntent);
                    } else
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calNow.getTimeInMillis(), pendingIntent);
                    context.startService(i);
                } else {
                    calNow.set(Calendar.HOUR_OF_DAY, wakeHr);
                    calNow.set(Calendar.MINUTE, wakeMin);
                    calNow.add(Calendar.MINUTE, (int) interval);
                    if (Build.VERSION.SDK_INT >= 23) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calNow.getTimeInMillis(), pendingIntent);
                    } else
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calNow.getTimeInMillis(), pendingIntent);
                }
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
