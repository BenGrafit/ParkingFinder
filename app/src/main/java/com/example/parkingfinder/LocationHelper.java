package com.example.parkingfinder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * LocationHelper - Handles GPS permissions, MyLocationNewOverlay setup, and proximity checks.
 * Now uses AlarmManager to trigger an Arrival Notification when the destination is reached.
 */
public class LocationHelper {

    private final Context context;
    private final MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private GeoPoint targetSpot;
    private boolean arrivalNotified = false;

    public LocationHelper(Context context, MapView map) {
        this.context = context;
        this.map = map;
    }

    public void handlePermissions(ActivityResultLauncher<String[]> launcher) {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(p);
            }
        }

        if (!missingPermissions.isEmpty()) {
            launcher.launch(missingPermissions.toArray(new String[0]));
        }
    }

    public void setupLocationOverlay() {
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map) {
            @Override
            public void onLocationChanged(android.location.Location location, org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                super.onLocationChanged(location, source);
                checkProximity(new GeoPoint(location));
            }
        };
        
        Drawable arrow = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass);
        if (arrow != null) {
            Bitmap bitmap = Bitmap.createBitmap(arrow.getIntrinsicWidth(), arrow.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            arrow.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            arrow.draw(canvas);
            mLocationOverlay.setDirectionArrow(bitmap, bitmap);
        }

        mLocationOverlay.enableMyLocation();
        mLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(mLocationOverlay);

        mLocationOverlay.runOnFirstFix(() -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).runOnUiThread(() -> {
                    if (mLocationOverlay.getMyLocation() != null) {
                        map.getController().animateTo(mLocationOverlay.getMyLocation());
                        map.getController().setZoom(17.0);
                    }
                });
            }
        });
    }

    public void setTargetSpot(GeoPoint spot) {
        this.targetSpot = spot;
        this.arrivalNotified = false;
    }

    private void checkProximity(GeoPoint currentLoc) {
        if (targetSpot != null && !arrivalNotified) {
            double distance = currentLoc.distanceToAsDouble(targetSpot);
            // If user is within 25 meters of the parking spot
            if (distance <= 25.0) {
                arrivalNotified = true;
                triggerArrivalAlarm();
            }
        }
    }

    /**
     * Schedules an immediate alarm to trigger the ArrivalReceiver notification.
     */
    private void triggerArrivalAlarm() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ArrivalReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public void centerOnLocation() {
        if (mLocationOverlay != null && mLocationOverlay.getMyLocation() != null) {
            map.getController().animateTo(mLocationOverlay.getMyLocation());
            map.getController().setZoom(17.0);
        } else {
            Toast.makeText(context, "מיקום לא זמין", Toast.LENGTH_SHORT).show();
        }
    }

    public MyLocationNewOverlay getLocationOverlay() {
        return mLocationOverlay;
    }

    public void onResume() {
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    public void onPause() {
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }
}
