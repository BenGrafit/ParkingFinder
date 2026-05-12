package com.example.parkingfinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class FunFactReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "fun_fact_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String locationName = intent.getStringExtra("location_name");
        String funFact = intent.getStringExtra("fun_fact");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fun Facts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Fun Fact about " + locationName)
                .setContentText(funFact)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(funFact))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
