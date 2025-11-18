package com.example.splendore;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CountdownActivity extends AppCompatActivity {

    private TextView tvDays, tvHours, tvMinutes, tvSeconds;
    private TextView tvEventTitle;
    private MaterialButton btnWebsite, btnTakePhoto;
    private CountDownTimer countDownTimer;

    // SET YOUR TARGET DATE HERE
    private static final int TARGET_YEAR = 2025;
    private static final int TARGET_MONTH = 10; // 0=Jan, 10=Nov
    private static final int TARGET_DAY = 27;
    private static final int TARGET_HOUR = 9;
    private static final int TARGET_MINUTE = 0;

    private static final String WEBSITE_URL = "https://splendore.rajagiri.edu/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        requestNotificationPermission();
        initViews();
        setupListeners();
        startCountdown();
        scheduleDailyNotification();
    }

    // ---------------------------------------------------
    // REQUEST POST_NOTIFICATIONS PERMISSION (Android 13+)
    // ---------------------------------------------------
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    private void initViews() {
        tvEventTitle = findViewById(R.id.tvEventTitle);

        tvDays = findViewById(R.id.tvDays);
        tvHours = findViewById(R.id.tvHours);
        tvMinutes = findViewById(R.id.tvMinutes);
        tvSeconds = findViewById(R.id.tvSeconds);

        btnWebsite = findViewById(R.id.btnWebsite);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
    }

    // ---------------------------------------------------
    // DAILY NOTIFICATION SCHEDULER
    // ---------------------------------------------------
    private void scheduleDailyNotification() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);

        // If it's already past 8:30 today, schedule for tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, NotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Daily repeating alarm (NO special permission needed)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void setupListeners() {
        btnWebsite.setOnClickListener(v -> openWebsite());
        btnTakePhoto.setOnClickListener(v -> openCamera());
    }

    // ---------------------------------------------------
    // COUNTDOWN TIMER
    // ---------------------------------------------------
    private void startCountdown() {
        Calendar targetDate = Calendar.getInstance();
        targetDate.set(TARGET_YEAR, TARGET_MONTH, TARGET_DAY, TARGET_HOUR, TARGET_MINUTE, 0);

        long difference = targetDate.getTimeInMillis() - System.currentTimeMillis();

        if (difference <= 0) {
            showEventStarted();
            return;
        }

        countDownTimer = new CountDownTimer(difference, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdown(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                showEventStarted();
            }
        }.start();
    }

    private void updateCountdown(long millisUntilFinished) {
        long days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished);
        long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;

        tvDays.setText(String.format("%02d", days));
        tvHours.setText(String.format("%02d", hours));
        tvMinutes.setText(String.format("%02d", minutes));
        tvSeconds.setText(String.format("%02d", seconds));
    }

    // ---------------------------------------------------
    // WHEN COUNTDOWN FINISHES
    // ---------------------------------------------------
    private void showEventStarted() {
        tvDays.setText("00");
        tvHours.setText("00");
        tvMinutes.setText("00");
        tvSeconds.setText("00");
        tvEventTitle.setText("Splendore is Here!");

        // Trigger notification once event starts
        sendEventStartedNotification();
    }

    private void sendEventStartedNotification() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        sendBroadcast(intent); // triggers NotificationReceiver immediately
    }

    // ---------------------------------------------------
    // OPEN WEBSITE / CAMERA
    // ---------------------------------------------------
    private void openWebsite() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL));
        startActivity(browserIntent);
    }

    private void openCamera() {
        Intent intent = new Intent(this, camPage.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
