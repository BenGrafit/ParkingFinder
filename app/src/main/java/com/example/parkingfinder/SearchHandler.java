package com.example.parkingfinder;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * SearchHandler - Handles address suggestions and search logic using the native android Geocoder.
 * Refactored to prioritize local results and rely on native geocoding intelligence.
 */
public class SearchHandler {

    private final Context context;
    private final ExecutorService executor;
    private final SuggestionsAdapter suggestionsAdapter;
    private final RecyclerView suggestionsRecyclerView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String lastQuery = "";
    private Runnable pendingSearch;
    private String cachedCity = null;

    public SearchHandler(Context context, EditText addressSearch, ExecutorService executor, 
                         SuggestionsAdapter suggestionsAdapter, RecyclerView suggestionsRecyclerView) {
        this.context = context;
        this.executor = executor;
        this.suggestionsAdapter = suggestionsAdapter;
        this.suggestionsRecyclerView = suggestionsRecyclerView;
    }

    /**
     * Loads the recent search history from the SearchHistoryProvider.
     */
    public void loadSearchHistory() {
        executor.execute(() -> {
            List<String> historyList = new ArrayList<>();
            try (Cursor cursor = context.getContentResolver().query(SearchHistoryProvider.CONTENT_URI, 
                    null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex("name");
                    do {
                        historyList.add(cursor.getString(nameIndex));
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            mainHandler.post(() -> {
                if (!historyList.isEmpty()) {
                    suggestionsAdapter.setSuggestions(historyList);
                    suggestionsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    suggestionsRecyclerView.setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * Fetches address suggestions as the user types, with debouncing and location biasing.
     */
    public void fetchSuggestions(String query, MyLocationNewOverlay mLocationOverlay) {
        final String cleanQuery = (query != null) ? query.trim() : "";
        if (cleanQuery.length() < 2) {
            mainHandler.post(() -> suggestionsRecyclerView.setVisibility(View.GONE));
            return;
        }

        // Debounce to prevent excessive API calls
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
        }

        pendingSearch = () -> {
            this.lastQuery = cleanQuery;
            executor.execute(() -> performFetch(cleanQuery, mLocationOverlay));
        };
        mainHandler.postDelayed(pendingSearch, 400);
    }

    /**
     * Core search logic using native android.location.Geocoder.
     * Uses a strict local bounding box with a fallback to global search.
     */
    private void performFetch(String query, MyLocationNewOverlay mLocationOverlay) {
        // Initialize Geocoder with Hebrew locale as requested
        Geocoder geocoder = new Geocoder(context, new Locale("he", "IL"));
        try {
            GeoPoint myLoc = (mLocationOverlay != null) ? mLocationOverlay.getMyLocation() : null;
            
            // Auto-detect the city to improve scoring (part of existing logic)
            if (myLoc != null && cachedCity == null) {
                try {
                    List<Address> currentAddr = geocoder.getFromLocation(myLoc.getLatitude(), myLoc.getLongitude(), 1);
                    if (currentAddr != null && !currentAddr.isEmpty()) {
                        cachedCity = currentAddr.get(0).getLocality();
                    }
                } catch (Exception ignored) {}
            }

            List<Address> addresses = new ArrayList<>();
            
            if (myLoc != null) {
                // Step 1: Strictly bounded local search (~0.15 degrees radius)
                double r = 0.15;
                try {
                    List<Address> tightResults = geocoder.getFromLocationName(query, 10, 
                            myLoc.getLatitude() - r, myLoc.getLongitude() - r, 
                            myLoc.getLatitude() + r, myLoc.getLongitude() + r);
                    
                    if (tightResults != null && !tightResults.isEmpty()) {
                        addresses.addAll(tightResults);
                    }
                } catch (Exception ignored) {}

                // Step 2: Fallback to global search if 0 results in bounding box
                if (addresses.isEmpty()) {
                    try {
                        List<Address> globalResults = geocoder.getFromLocationName(query, 10);
                        if (globalResults != null) {
                            addresses.addAll(globalResults);
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                // Fallback for when location is not available
                try {
                    List<Address> globalResults = geocoder.getFromLocationName(query, 10);
                    if (globalResults != null) {
                        addresses.addAll(globalResults);
                    }
                } catch (Exception ignored) {}
            }

            if (!query.equals(lastQuery)) return;

            // Sort results: prioritizes relevance score, then proximity
            Collections.sort(addresses, (a1, a2) -> {
                int s1 = calculateMatchScore(a1.getAddressLine(0), query);
                int s2 = calculateMatchScore(a2.getAddressLine(0), query);
                if (s1 != s2) return Integer.compare(s2, s1);

                if (myLoc != null) {
                    float[] d1 = new float[1], d2 = new float[1];
                    android.location.Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), a1.getLatitude(), a1.getLongitude(), d1);
                    android.location.Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), a2.getLatitude(), a2.getLongitude(), d2);
                    return Float.compare(d1[0], d2[0]);
                }
                return 0;
            });

            List<String> resultsStrings = new ArrayList<>();
            for (Address addr : addresses) {
                // Construct a clean "Street, City" format
                String street = addr.getThoroughfare();
                if (street == null) street = addr.getFeatureName();
                
                String city = addr.getLocality();
                if (city == null) city = addr.getSubAdminArea();
                
                if (street != null) {
                    // Using native Geocoder intelligence directly, no manual string replacement
                    String displayString = street;
                    if (city != null) {
                        displayString += ", " + city;
                    }

                    // Append distance if user location is available
                    if (myLoc != null) {
                        float[] dist = new float[1];
                        android.location.Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), 
                                addr.getLatitude(), addr.getLongitude(), dist);
                        displayString += " (" + formatDistance(dist[0]) + ")";
                    }
                    resultsStrings.add(displayString);
                }
            }

            mainHandler.post(() -> {
                if (query.equals(lastQuery)) {
                    if (resultsStrings.isEmpty()) {
                        suggestionsRecyclerView.setVisibility(View.GONE);
                    } else {
                        suggestionsAdapter.setSuggestions(resultsStrings);
                        suggestionsRecyclerView.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    /**
     * Helper to format distance in meters/kilometers.
     */
    private String formatDistance(float meters) {
        if (meters < 1000) {
            return (int) meters + " m";
        } else {
            return String.format(Locale.getDefault(), "%.1f km", meters / 1000f);
        }
    }

    private int calculateMatchScore(String address, String query) {
        if (address == null || query == null) return 0;
        String a = address.toLowerCase();
        String q = query.toLowerCase();
        
        int score = 0;
        if (a.contains(q)) score += 1000;
        
        for (String word : q.split("\\s+")) {
            if (word.length() > 1 && a.contains(word)) score += 100;
        }
        
        // Bonus for results in the same city
        if (cachedCity != null && a.contains(cachedCity.toLowerCase())) score += 500;
        
        return score;
    }

    public void startVoiceRecognition(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "דבר עכשיו לחיפוש כתובת...");
        try {
            launcher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(context, "החיפוש הקולי אינו נתמך במכשיר זה", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Performs a final search for the destination and triggers parking spot lookup.
     */
    public void searchNearestParking(String addressText, MapManager mapManager) {
        // Strip distance suffix if present
        final String query = addressText.contains(" (") ? addressText.substring(0, addressText.lastIndexOf(" (")) : addressText;

        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(context, new Locale("he", "IL"));
            try {
                // Final destination search
                List<Address> addresses = geocoder.getFromLocationName(query, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    GeoPoint dest = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                    
                    mainHandler.post(() -> {
                        mapManager.saveSearchToHistory(query, dest);
                        
                        List<ParkingSpot> allSpots = mapManager.getAllParkingSpots();
                        ParkingSpot nearest = null;
                        double minDistance = Double.MAX_VALUE;
                        
                        for (ParkingSpot spot : allSpots) {
                            if (spot.isEmpty()) {
                                double d = dest.distanceToAsDouble(new GeoPoint(spot.getX(), spot.getY()));
                                if (d < minDistance) {
                                    minDistance = d;
                                    nearest = spot;
                                }
                            }
                        }
                        
                        if (nearest != null && context instanceof MainActivity) {
                            ((MainActivity) context).onParkingFound(new GeoPoint(nearest.getX(), nearest.getY()), dest);
                        } else {
                            Toast.makeText(context, "לא נמצאו חניות פנויות בקרבת מקום", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(context, "הכתובת לא נמצאה", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(context, "שגיאת חיפוש: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
