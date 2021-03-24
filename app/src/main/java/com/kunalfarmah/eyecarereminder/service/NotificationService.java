package com.kunalfarmah.eyecarereminder.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.kunalfarmah.eyecarereminder.activity.AlarmSetActivity;
import com.kunalfarmah.eyecarereminder.utils.Constants;
import com.kunalfarmah.eyecarereminder.R;
import com.kunalfarmah.eyecarereminder.receiver.ReminderReceiver;


public class NotificationService extends Service {

    Ringtone ringtone;
    SharedPreferences sharedPreferences;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = getSharedPreferences(Constants.SETTINGS_APP_SETTINGS,MODE_PRIVATE);

        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire(60 /*1 minute*/);

        sendNotification(intent);
        return Service.START_STICKY;
    }

    private void sendNotification(Intent intent) {
        String channelId = getString(R.string.channel_id);
        Intent contentIntent = new Intent(this, AlarmSetActivity.class);
        intent.putExtra("Notify", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                contentIntent, 0);
        Intent deleteIntent = new Intent(this, ReminderReceiver.class);
        deleteIntent.putExtra("CANCEL", "CANCEL");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setContentIntent(pendingIntent)
                .setDeleteIntent(PendingIntent.getBroadcast(this, (int) SystemClock.uptimeMillis(), deleteIntent, 0))
                .setContentText(getApplicationContext().getString(R.string.this_your_water_reminder));
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    " Notification",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify((int) SystemClock.uptimeMillis(), notificationBuilder.build());
    }

    private void startRingtone() {
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
        ringtone.play();
        Thread th = new Thread(() -> {
            try {
                Thread.sleep(25000);  //30000 is for 30 seconds, 1 sec =1000
                if (ringtone.isPlaying())
                    ringtone.stop();   // for stopping the ringtone
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        th.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        ringtone.stop();
        super.onDestroy();
    }
}
