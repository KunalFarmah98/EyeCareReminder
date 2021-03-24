package com.kunalfarmah.eyecarereminder.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


import com.kunalfarmah.eyecarereminder.utils.Constants;
import com.kunalfarmah.eyecarereminder.R;
import com.kunalfarmah.eyecarereminder.receiver.NotificationBroadcastReceiver;

import java.util.Calendar;

public class TimerService extends Service {
    CountDownTimer countDownTimer;
    SharedPreferences sharedPreferences;
    int interval, intervalInt;
    public static final String TAG = "TimerService";
    public static final String SERVICE_NOTIFICATION_CHANNEL = "EyeCareReminder";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendForegroundNotification();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Timer Started");
        // preventing dose mode
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        sendForegroundNotification();
        sharedPreferences = getSharedPreferences(Constants.SETTINGS_APP_SETTINGS, MODE_PRIVATE);
        interval = (int) sharedPreferences.getLong(Constants.ALARM_INTERVAL, 30);
        intervalInt = interval;
        interval = interval*60*1000;

        // running a timer for a day and checking current time every second
        countDownTimer = new CountDownTimer(24 * 60 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // finding wake, sleep, current and start time in minutes
                int wakeHr = sharedPreferences.getInt(Constants.WAKE_HR, 11);
                int wakeMin = sharedPreferences.getInt(Constants.WAKE_MIN, 0);

                int sleepHr = sharedPreferences.getInt(Constants.SLEEP_HR, 23);
                int sleepMin = sharedPreferences.getInt(Constants.WAKE_MIN, 0);

                int startHr = sharedPreferences.getInt(Constants.START_HR, wakeHr);
                int startMin = sharedPreferences.getInt(Constants.START_MIN, wakeMin);

                Calendar calNow = Calendar.getInstance();
                long currHr = calNow.get(Calendar.HOUR_OF_DAY);
                long currMin = calNow.get(Calendar.MINUTE);
                long currTime = (currHr * 60) + currMin;

                Calendar startCal  = (Calendar) calNow.clone();
                startCal.set(Calendar.HOUR_OF_DAY,startHr);
                startCal.set(Calendar.MINUTE,startMin);

                long startTime = (startHr * 60) + startMin;

                long wakeTime = (wakeHr * 60) + wakeMin;

                long sleepTime = (sleepHr * 60) + sleepMin;

                long curr = calNow.getTimeInMillis();
                long start = startCal.getTimeInMillis();

                // day changed, time back to 0hrs
                if(curr<start){
                    curr = curr+24*60*60*1000;
                }

                // accounting for 24 hour format day change
                if (sleepHr < wakeHr) {
                    sleepTime += 24 * 60;
                }
                if (currHr < wakeHr) {
                    currTime += 24 * 60;
                }

                long diff = curr - start;
                // if its time to remind (withing 5 minutes of interval), STICKY SERVICE will be
                // restarted well within 5 mins of killing by Android
                if (interval <= diff && diff < interval + 10*60*1000) {
                    // if time is within the period of wake and sleep, notify
                    if (wakeTime < currTime && currTime < sleepTime) {
                        Intent i = new Intent(getApplicationContext(), NotificationService.class);
                        i.putExtra("TEXT", getApplicationContext().getString(R.string.this_your_water_reminder));
                        getApplicationContext().startService(new Intent(getApplicationContext(), NotificationService.class));
                        // now a new cycle starts for the interval from this time, incrementing last start time with interval
                        // adding interval to prev start time
                        startCal.add(Calendar.MINUTE,intervalInt);
                        // setting the new start time for next cycle
                        sharedPreferences.edit().putInt(Constants.START_HR, startCal.get(Calendar.HOUR_OF_DAY)).apply();
                        sharedPreferences.edit().putInt(Constants.START_MIN, startCal.get(Calendar.MINUTE)).apply();
                    }
                    else{
                        // as we are out of active hours, set reminder to fire exactly interval mins after wake time
                        startCal.set(Calendar.HOUR_OF_DAY,wakeHr);
                        startCal.set(Calendar.MINUTE,wakeMin);
                        // setting the new start time for next cycle
                        sharedPreferences.edit().putInt(Constants.START_HR, startCal.get(Calendar.HOUR_OF_DAY)).apply();
                        sharedPreferences.edit().putInt(Constants.START_MIN, startCal.get(Calendar.MINUTE)).apply();
                    }

                }
                // if the interval passed for some reason
                else if(diff>interval+10*60*1000){
                    // if we can still remind, remind
                    if (wakeTime < currTime && currTime < sleepTime) {
                        Intent i = new Intent(getApplicationContext(), NotificationService.class);
                        i.putExtra("TEXT", getApplicationContext().getString(R.string.this_your_water_reminder));
                        getApplicationContext().startService(new Intent(getApplicationContext(), NotificationService.class));
                        // setting the new start time for next cycle as the current time for consistency
                        sharedPreferences.edit().putInt(Constants.START_HR, calNow.get(Calendar.HOUR_OF_DAY)).apply();
                        sharedPreferences.edit().putInt(Constants.START_MIN, calNow.get(Calendar.MINUTE)).apply();
                    }
                    else{
                        // as we are out of active hours, set reminder to fire exactly interval mins after wake time
                        startCal.set(Calendar.HOUR_OF_DAY,wakeHr);
                        startCal.set(Calendar.MINUTE,wakeMin);
                        // setting the new start time for next cycle
                        sharedPreferences.edit().putInt(Constants.START_HR, startCal.get(Calendar.HOUR_OF_DAY)).apply();
                        sharedPreferences.edit().putInt(Constants.START_MIN, startCal.get(Calendar.MINUTE)).apply();
                    }

                }
                Log.d("Tick", String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                this.start();
            }
        };
        countDownTimer.start();
        return Service.START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendForegroundNotification() {
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel serviceChannel = new NotificationChannel(
                SERVICE_NOTIFICATION_CHANNEL,
                getString(R.string.foreground_notify_title),
                NotificationManager.IMPORTANCE_DEFAULT);
        notifManager.createNotificationChannel(serviceChannel);

        Intent intent = new Intent(this, NotificationBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                Constants.FOREGROUND_SERVICE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new Notification.Builder(this, SERVICE_NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.foreground_notify_title))
                .setContentText(getString(R.string.hide_notification))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .getNotification();
        startForeground(Constants.FOREGROUND_SERVICE_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        countDownTimer.cancel();
        Log.d(TAG, "destroyed");
    }
}
