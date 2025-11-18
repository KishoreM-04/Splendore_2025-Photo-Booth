package com.example.splendore;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "daily_channel";

    // Target event time
    private static final int TARGET_YEAR = 2025;
    private static final int TARGET_MONTH = 10; // 0 = Jan, 10 = Nov
    private static final int TARGET_DAY = 27;
    private static final int TARGET_HOUR = 9;
    private static final int TARGET_MINUTE = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Calculate remaining days
        Calendar target = Calendar.getInstance();
        target.set(TARGET_YEAR, TARGET_MONTH, TARGET_DAY, TARGET_HOUR, TARGET_MINUTE, 0);

        long now = System.currentTimeMillis();
        long diff = target.getTimeInMillis() - now;

        String message;

        if (diff <= 0) {
            message = "Splendore is Here! ✨🔥";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            message = days + " days remaining! Don't miss it! Something exciting is coming ✨🔥";
        }

        // Create channel
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Reminder",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Splendore Countdown")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show it
        manager.notify(1001, builder.build());
    }
}
