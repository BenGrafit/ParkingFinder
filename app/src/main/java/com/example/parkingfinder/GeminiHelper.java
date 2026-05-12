package com.example.parkingfinder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {
    private static final String TAG = "GeminiHelper";
    // IMPORTANT: Replace with your actual API Key or guide the user to provide it.
    private static final String API_KEY = "YOUR_GEMINI_API_KEY"; 
    
    private final GenerativeModelFutures model;
    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public GeminiHelper(Context context) {
        this.context = context;
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    public void generateFunFactAndScheduleNotification(String locationName) {
        if (API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            Log.e(TAG, "API Key not set!");
            return;
        }

        Content content = new Content.Builder()
                .addText("Give me one short fun fact about " + locationName + ". Keep it under 20 words.")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String funFact = result.getText();
                scheduleNotification(locationName, funFact);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini Error: " + t.getMessage());
            }
        }, executor);
    }

    private void scheduleNotification(String locationName, String funFact) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FunFactReceiver.class);
        intent.putExtra("location_name", locationName);
        intent.putExtra("fun_fact", funFact);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 5000; // 5 seconds later

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}
