package com.kunalfarmah.eyecarereminder.activity;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


import com.kunalfarmah.eyecarereminder.utils.Constants;
import com.kunalfarmah.eyecarereminder.R;
import com.kunalfarmah.eyecarereminder.databinding.ActivityAlarmSetBinding;
import com.kunalfarmah.eyecarereminder.receiver.ReminderReceiver;
import com.kunalfarmah.eyecarereminder.service.TimerService;

import java.util.Calendar;


public class AlarmSetActivity extends AppCompatActivity {

    ActivityAlarmSetBinding binding;
    private Calendar calSet;
    private double interval;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(Constants.SETTINGS_APP_SETTINGS,MODE_PRIVATE);
        binding = ActivityAlarmSetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Eye Care Reminder");
        actionBar.setHomeButtonEnabled(true);

        /*// requesting user to ignore battery optimisations for alarm to work in doze state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = AlarmSetActivity.this.getPackageName();
            PowerManager pm = (PowerManager) AlarmSetActivity.this.getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Dialog dialog = new Dialog(AlarmSetActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setContentView(R.layout.dialog_background_run);
                dialog.findViewById(R.id.ok).setOnClickListener(view -> {
                    dialog.dismiss();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                });
                dialog.findViewById(R.id.cancel).setOnClickListener(view -> {
                    dialog.cancel();
                    Toast.makeText(AlarmSetActivity.this, getString(R.string.please_allow_bkg_running), Toast.LENGTH_SHORT).show();
                    onBackPressed();
                });
                dialog.setCancelable(false);
                dialog.show();
            }
        }*/

        binding.wakePicker.setOnClickListener(v -> {
            Calendar calNow = Calendar.getInstance();
            calSet = (Calendar) calNow.clone();
            int hour = calNow.get(Calendar.HOUR_OF_DAY);
            int minute = calNow.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(AlarmSetActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    sharedPreferences.edit().putInt(Constants.WAKE_HR, selectedHour).apply();
                    sharedPreferences.edit().putInt(Constants.WAKE_MIN, selectedMinute).apply();
                }
            }, hour, minute, false);
            mTimePicker.setTitle("Select Waking Up Time");
            mTimePicker.show();
        });
        binding.sleepPicker.setOnClickListener(v -> {
            Calendar calNow = Calendar.getInstance();
            calSet = (Calendar) calNow.clone();
            int hour = calNow.get(Calendar.HOUR_OF_DAY);
            int minute = calNow.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(AlarmSetActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    sharedPreferences.edit().putInt(Constants.SLEEP_HR, selectedHour).apply();
                    sharedPreferences.edit().putInt(Constants.SLEEP_MIN, selectedMinute).apply();
                }
            }, hour, minute, false);
            mTimePicker.setTitle("Select Sleeping Time");
            mTimePicker.show();
        });

        binding.setReminder.setOnClickListener(v -> {

           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Show alert dialog to the user saying a separate permission is needed
                // Launch the settings activity if the user prefers
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(myIntent);
            }*/

            sharedPreferences.edit().putLong(Constants.ALARM_INTERVAL, Long.parseLong(binding.hourPicker.getText()
                    .toString())).apply();
            sharedPreferences.edit().putBoolean(Constants.WATER_REMINDER_ENABLED, true).apply();
            Calendar calendar = Calendar.getInstance();
            sharedPreferences.edit().putLong(Constants.START_TIME,calendar.getTimeInMillis()).apply();
            sharedPreferences.edit().putInt(Constants.START_HR,calendar.get(Calendar.HOUR_OF_DAY)).apply();
            sharedPreferences.edit().putInt(Constants.START_MIN, calendar.get(Calendar.MINUTE)).apply();

            // using alarm manager prior to Oreo
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                setAlarm(calSet);
            }
            // using foreground service from Oreo onwards
            else {
                try {
                    if (isMyServiceRunning(TimerService.class)) {
                        stopService(new Intent(getApplicationContext(), TimerService.class));
                    }
                }
                catch (Exception e){
                }

                Intent countDown = new Intent(getApplicationContext(), TimerService.class);
                countDown.putExtra("TEXT",getString(R.string.this_your_water_reminder));
                startForegroundService(countDown);
            }
            Toast.makeText(AlarmSetActivity.this, R.string.reminder_success, Toast.LENGTH_SHORT).show();

        });
        binding.cancelReminder.setOnClickListener(v -> {
            sharedPreferences.edit().putBoolean(Constants.WATER_REMINDER_ENABLED, false).apply();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                cancelAlarm();
            } else {
                if (isMyServiceRunning(TimerService.class)) {
                    stopService(new Intent(getApplicationContext(), TimerService.class));
                    Toast.makeText(AlarmSetActivity.this, R.string.reminder_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AlarmSetActivity.this, R.string.no_reminder, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void setAlarm(Calendar calSet) {
        if (calSet == null || binding.hourPicker.getText().toString().isEmpty()) {
            Toast.makeText(this, "Interval Can't be Empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        sharedPreferences.edit().putBoolean(Constants.WATER_REMINDER_ENABLED, true).apply();
        calSet.setTimeInMillis(System.currentTimeMillis());
        interval = Double.parseDouble(binding.hourPicker.getText().toString());
        calSet.add(Calendar.MINUTE, (int) interval);

        sharedPreferences.edit().putLong(Constants.ALARM_INTERVAL, (long) interval).apply();
//        Toast.makeText(this, "Alarm is set for " + calSet.getTime(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getBaseContext(), ReminderReceiver.class);
        intent.putExtra("TEXT", getString(R.string.this_your_water_reminder));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), Constants.WATER_REMINDER, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calSet.getTimeInMillis(), pendingIntent);
        } else
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calSet.getTimeInMillis(), pendingIntent);
        Toast.makeText(this, getString(R.string.reminder_success), Toast.LENGTH_SHORT).show();
    }

    private void cancelAlarm() {
        Intent intent = new Intent(getBaseContext(), ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), Constants.WATER_REMINDER, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        sharedPreferences.edit().putBoolean(Constants.WATER_REMINDER_ENABLED, false).apply();
        Toast.makeText(this, getString(R.string.reminder_cancelled), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}