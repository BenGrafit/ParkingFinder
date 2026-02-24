package com.example.parkingfinder;

/* 
 * 1. IMPORTS
 * In Android, we import specialized libraries for UI (widgets), 
 * Location (GPS), and Maps (osmdroid).
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 2. AppCompatActivity
 * In Java, you usually create a class with a 'main' method. In Android, the system
 * manages your class. Extending 'AppCompatActivity' tells Android that this class 
 * is a "Screen" (Activity) that supports modern features on older phones.
 */
public class MainActivity extends AppCompatActivity {

    // Member variables: These live as long as the Screen is open.
    private MapView map = null; // The visual map component.
    private FireStoreHelper helper; // Your custom class to talk to Firebase database.
    private FloatingActionButton addSpotBtn; // A round button floating over the map.
    private FloatingActionButton centerLocationBtn; 
    private MyLocationNewOverlay mLocationOverlay; // A "Layer" on the map that shows where you are.
    private Polyline currentRoadOverlay; // A "Layer" that draws the blue/red line for the road.
    private List<ParkingSpot> allParkingSpots = new ArrayList<>(); // Stores the data from the database.
    private AutoCompleteTextView addressSearch; // An input field that gives suggestions while you type.
    private Button searchButton;

    /**
     * 3. ExecutorService (Background Workers)
     * Android is strict: The "Main Thread" (the loop that draws the screen) 
     * is NEVER allowed to do slow things like downloading data from the internet. 
     * If it does, the app freezes (ANR - App Not Responding).
     * This 'executor' is like a "back-office worker" that does heavy tasks in the background.
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 4. onCreate
     * This is the "Constructor" of your screen. Android calls this method 
     * automatically when the app first opens.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * 5. StrictMode
         * This command tells Android: "I know networking on the main thread is bad, 
         * but allow it for now." We use this here to simplify the routing logic.
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /*
         * 6. Context & Preferences
         * 'Context' is a handle to the Android system. 'PreferenceManager' 
         * stores small settings like "Has the user seen the tutorial?". 
         * Osmdroid needs this to know where to save map tiles on your phone.
         */
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        /*
         * 7. setContentView
         * This connects this Java file to your XML layout file (activity_main.xml).
         */
        setContentView(R.layout.activity_main);

        // findViewById: Finds the element in your XML by its ID so you can control it in Java.
        addressSearch = findViewById(R.id.addressSearch);
        searchButton = findViewById(R.id.searchButton);

        // Map setup
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Sets the look of the map (standard street map).
        map.setMultiTouchControls(true); // Allows zooming with two fingers.

        // IMapController: A "Remote Control" for the map to move or zoom it.
        IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        
        // GeoPoint: An object that holds Latitude and Longitude.
        GeoPoint startPoint = new GeoPoint(32.0853, 34.7818); 
        mapController.setCenter(startPoint);

        /*
         * 8. MyLocationNewOverlay
         * This creates the little icon that shows where YOU are.
         * 'GpsMyLocationProvider' is the part that talks to the phone's GPS hardware.
         */
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        
        // Creating a Direction Arrow
        Drawable arrowDrawable = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.next);
        if (arrowDrawable != null) {
            // We convert a 'Drawable' (vector image) into a 'Bitmap' (pixel grid) for the map.
            Bitmap bitmap = Bitmap.createBitmap(arrowDrawable.getIntrinsicWidth(), arrowDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            arrowDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            arrowDrawable.draw(canvas);
            mLocationOverlay.setDirectionArrow(bitmap, bitmap); // Sets the arrow icon.
        }

        mLocationOverlay.enableMyLocation(); // Start tracking GPS.
        mLocationOverlay.setDrawAccuracyEnabled(true); // Show the light blue circle around the user.
        map.getOverlays().add(mLocationOverlay); // Add this layer to the map.

        /*
         * 9. runOnFirstFix
         * GPS takes time to find you (satellite search). This method says: 
         * "Wait until you find the coordinates, THEN run this code."
         */
        mLocationOverlay.runOnFirstFix(() -> {
            /*
             * runOnUiThread: Because we are in a background GPS task, we can't 
             * touch the UI directly. We "jump" back to the Main Thread here.
             */
            runOnUiThread(() -> {
                GeoPoint myLocation = mLocationOverlay.getMyLocation();
                if (myLocation != null) {
                    mapController.animateTo(myLocation); // Smooth glide to your location.
                    mapController.setZoom(17.0);
                }
            });
        });

        helper = new FireStoreHelper();

        // Button Click Listeners (Lambdas)
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

        centerLocationBtn = findViewById(R.id.CenterLocation);
        centerLocationBtn.setOnClickListener(v -> {
            GeoPoint myLocation = mLocationOverlay.getMyLocation();
            if (myLocation != null) {
                map.getController().animateTo(myLocation);
                map.getController().setZoom(17.0);
            } else {
                // Toast: A small popup message at the bottom of the screen.
                Toast.makeText(this, "המיקום לא זמין כרגע", Toast.LENGTH_SHORT).show();
            }
        });

        setupSearchSuggestions();

        searchButton.setOnClickListener(v -> {
            String addressText = addressSearch.getText().toString();
            // Clean the text if it contains distance info like "(1.4km)"
            if (addressText.contains(" (")) {
                addressText = addressText.substring(0, addressText.lastIndexOf(" ("));
            }
            if (addressText.isEmpty()) {
                Toast.makeText(this, "אנא הכנס כתובת", Toast.LENGTH_SHORT).show();
                return;
            }
            searchNearestParking(addressText);
        });

        // Firebase Real-time listener: Whenever the database changes, this code runs.
        helper.listenToParkingSpots(new ParkingSpotListener() {
            @Override
            public void onUpdate(List<ParkingSpot> updatedList) {
                updateMapMarkers(updatedList); // Redraw markers when data changes.
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "שגיאה בטעינת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 10. TextWatcher
     * This is a listener that watches the keyboard. Every time you type a letter, 
     * 'onTextChanged' fires.
     */
    private void setupSearchSuggestions() {
        addressSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) { // Only start searching after 3 letters to save data.
                    fetchSuggestions(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 11. Geocoding (Address Lookup)
     * GeocoderNominatim is a service that takes a string ("London") and returns 
     * a Latitude and Longitude.
     */
    private void fetchSuggestions(String query) {
        executor.execute(() -> { // Run this in the background!
            GeocoderNominatim geocoder = new GeocoderNominatim(Locale.getDefault(), "ParkingFinder");
            try {
                GeoPoint myLocation = mLocationOverlay.getMyLocation();
                List<Address> addresses;
                if (myLocation != null) {
                    // Bias search results to be near the user.
                    double delta = 0.5;
                    addresses = geocoder.getFromLocationName(query, 10, 
                            myLocation.getLatitude() - delta, myLocation.getLongitude() - delta, 
                            myLocation.getLatitude() + delta, myLocation.getLongitude() + delta, true);
                } else {
                    addresses = geocoder.getFromLocationName(query, 10);
                }
                
                List<String> suggestionStrings = new ArrayList<>();
                for (Address addr : addresses) {
                    String name = addr.getFeatureName();
                    
                    StringBuilder sb = new StringBuilder();
                    if (name != null) sb.append(name);

                    // Distance Calculation
                    if (myLocation != null) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(myLocation.getLatitude(), myLocation.getLongitude(),
                                addr.getLatitude(), addr.getLongitude(), results);
                        double distanceKm = results[0] / 1000.0;
                        sb.append(String.format(Locale.getDefault(), " (%.1fkm)", distanceKm));
                    }
                    
                    if (sb.length() > 0) suggestionStrings.add(sb.toString());
                }

                // Jump back to Main Thread to show the dropdown list.
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_dropdown_item_1line, suggestionStrings);
                    addressSearch.setAdapter(adapter);
                    if (!suggestionStrings.isEmpty()) {
                        addressSearch.showDropDown();
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    /**
     * 12. searchNearestParking
     * Finds the destination coordinates and calls the pathfinding logic.
     */
    private void searchNearestParking(String addressText) {
        GeocoderNominatim geocoder = new GeocoderNominatim(Locale.getDefault(), "ParkingFinder");
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                GeoPoint destination = new GeoPoint(address.getLatitude(), address.getLongitude());
                
                ParkingSpot nearestSpot = findNearestSpot(destination);
                if (nearestSpot != null) {
                    drawRouteToSpot(new GeoPoint(nearestSpot.getX(), nearestSpot.getY()));
                    map.getController().animateTo(destination);
                } else {
                    Toast.makeText(this, "לא נמצאו חניות פנויות", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "כתובת לא נמצאה", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בחיפוש כתובת: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ParkingSpot findNearestSpot(GeoPoint destination) {
        ParkingSpot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ParkingSpot spot : allParkingSpots) {
            if (spot.isEmpty()) { 
                double distance = destination.distanceToAsDouble(new GeoPoint(spot.getX(), spot.getY()));
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = spot;
                }
            }
        }
        return nearest;
    }

    /**
     * 13. Pathfinding (Routing)
     * This uses OSRM (Open Source Routing Machine) to find the lines 
     * representing the roads between two points.
     */
    private void drawRouteToSpot(GeoPoint spotPoint) {
        // Clear old route if it exists.
        if (currentRoadOverlay != null) {
            map.getOverlays().remove(currentRoadOverlay);
        }

        GeoPoint startPoint = mLocationOverlay.getMyLocation();
        if (startPoint == null) {
            Toast.makeText(this, "מיקום המשתמש אינו זמין", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(startPoint);
        waypoints.add(spotPoint);

        RoadManager roadManager = new OSRMRoadManager(this, "ParkingFinder");
        Road road = roadManager.getRoad(waypoints); // This makes a network request.
        
        // buildRoadOverlay: Takes the road data and creates a Polyline (line on map).
        currentRoadOverlay = RoadManager.buildRoadOverlay(road);
        map.getOverlays().add(currentRoadOverlay);
        
        // invalidate: Force the map to redraw itself so we see the new line.
        map.invalidate();
        
        Toast.makeText(this, "מנווט לחניה הקרובה ביותר", Toast.LENGTH_SHORT).show();
    }

    /**
     * 14. Markers (Pins on Map)
     * Converts your ParkingSpot objects from Firebase into visual Markers (pins).
     */
    private void updateMapMarkers(List<ParkingSpot> spots) {
        if (map == null) return;
        this.allParkingSpots = spots;

        // removeIf: A list command that keeps only the layers we want.
        map.getOverlays().removeIf(overlay -> !(overlay instanceof MyLocationNewOverlay || overlay == currentRoadOverlay));

        for (ParkingSpot spot : spots) {
            GeoPoint point = new GeoPoint(spot.getX(), spot.getY());
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Marker Tinting: Change color of the pin (Red for busy, Green for free).
            Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default);
            if (icon != null) {
                icon = DrawableCompat.wrap(icon).mutate();
                if (spot.isEmpty()) {
                    DrawableCompat.setTint(icon, ContextCompat.getColor(this, android.R.color.holo_green_light));
                    marker.setTitle("חניה פנויה");
                } else {
                    DrawableCompat.setTint(icon, ContextCompat.getColor(this, android.R.color.holo_red_light));
                    marker.setTitle("חניה תפוסה");
                }
                marker.setIcon(icon);
            }

            marker.setOnMarkerClickListener((m, mapView) -> {
                drawRouteToSpot(m.getPosition());
                m.showInfoWindow(); // Shows the popup text above the pin.
                return true;
            });

            map.getOverlays().add(marker);
        }
        
        map.invalidate(); 
    }

    /*
     * 15. Activity Lifecycle
     * Android activities can be "Paused" (phone call comes in) or "Resumed" (user returns).
     * We must tell the map to pause/resume GPS usage to save battery.
     */
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

    /**
     * 16. onDestroy
     * Called when the app is closed completely. We shut down our background worker
     * and stop listening to the database.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (helper != null) helper.stopListening();
    }
}
