package com.example.parkingfinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView map = null;
    private FireStoreHelper helper;
    private FloatingActionButton addSpotBtn;
    private FloatingActionButton centerLocationBtn;
    private MyLocationNewOverlay mLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        // Initialize Map
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        
        // Fix: Use a default coordinate instead of calling getMyLocation() on a null object
        GeoPoint startPoint = new GeoPoint(32.0853, 34.7818); // Default to Tel Aviv
        mapController.setCenter(startPoint);

        // Initialize Location Overlay
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        
        // Change person icon to arrow for direction
        Drawable arrowDrawable = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.next);
        if (arrowDrawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(arrowDrawable.getIntrinsicWidth(), arrowDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            arrowDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            arrowDrawable.draw(canvas);
            mLocationOverlay.setDirectionArrow(bitmap, bitmap);
        }

        mLocationOverlay.enableMyLocation();
        mLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(mLocationOverlay);

        // Auto-center on user's location once it's found
        mLocationOverlay.runOnFirstFix(() -> {
            runOnUiThread(() -> {
                GeoPoint myLocation = mLocationOverlay.getMyLocation();
                if (myLocation != null) {
                    mapController.animateTo(myLocation);
                    mapController.setZoom(17.0);
                }
            });
        });

        // Initialize Firestore helper
        helper = new FireStoreHelper();

        // Add spot button logic
        addSpotBtn = findViewById(R.id.AddSpot);
        addSpotBtn.setOnClickListener(v ->
                Adapter.showAddDialog(this, spot -> {
                    helper.addParkingSpot(
                            spot.getX(),
                            spot.getY(),
                            ref -> Toast.makeText(this, "חניה נוספה בהצלחה", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
        );

        // Center on location button logic
        centerLocationBtn = findViewById(R.id.CenterLocation);
        centerLocationBtn.setOnClickListener(v -> {
            GeoPoint myLocation = mLocationOverlay.getMyLocation();
            if (myLocation != null) {
                map.getController().animateTo(myLocation);
                map.getController().setZoom(17.0);
            } else {
                Toast.makeText(this, "המיקום לא זמין כרגע", Toast.LENGTH_SHORT).show();
            }
        });

        // Listen for parking spots and display them on map
        helper.listenToParkingSpots(new ParkingSpotListener() {
            @Override
            public void onUpdate(List<ParkingSpot> updatedList) {
                updateMapMarkers(updatedList);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "שגיאה בטעינת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMapMarkers(List<ParkingSpot> spots) {
        if (map == null) return;

        // Clear existing overlays except the location overlay
        map.getOverlays().removeIf(overlay -> !(overlay instanceof MyLocationNewOverlay));

        for (ParkingSpot spot : spots) {
            GeoPoint point = new GeoPoint(spot.getX(), spot.getY());
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Set marker color based on status
            Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default);
            if (icon != null) {
                icon = DrawableCompat.wrap(icon).mutate();
                if (spot.isEmpty()) {
                    DrawableCompat.setTint(icon, ContextCompat.getColor(this, android.R.color.holo_green_light));
                    marker.setTitle("חניה פנויה");
                    marker.setSubDescription("לחץ לפרטים נוספים");
                } else {
                    DrawableCompat.setTint(icon, ContextCompat.getColor(this, android.R.color.holo_red_light));
                    marker.setTitle("חניה תפוסה");
                }
                marker.setIcon(icon);
            }

            map.getOverlays().add(marker);
        }
        
        map.invalidate(); // Refresh map
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (helper != null) helper.stopListening();
    }
}
