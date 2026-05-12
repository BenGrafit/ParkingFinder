package com.example.parkingfinder;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * MapManager - Refactored to use OSMBonusPack features for folder management,
 * markers, and routing. This follows the recommendation to use the library's
 * built-in capabilities.
 */
public class MapManager {

    private final MapView map;
    private final Context context;
    private final ExecutorService executor;
    private Polyline currentRoadOverlay;
    private final FolderOverlay markersFolder;
    private List<ParkingSpot> allParkingSpots = new ArrayList<>();

    public MapManager(MapView map, Context context, ExecutorService executor) {
        this.map = map;
        this.context = context;
        this.executor = executor;
        this.markersFolder = new FolderOverlay();
    }

    public void initMap() {
        if (map == null) return;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(new GeoPoint(32.0853, 34.7818)); // Tel Aviv default
        
        // Use FolderOverlay to group markers. This is the OSMBonusPack way to manage groups of markers.
        map.getOverlays().add(markersFolder);
    }

    public void updateMapMarkers(List<ParkingSpot> spots, MyLocationNewOverlay mLocationOverlay) {
        if (map == null) return;
        this.allParkingSpots = spots;

        // Efficiently clear only the markers in our managed folder.
        markersFolder.getItems().clear();

        for (ParkingSpot spot : spots) {
            GeoPoint point = new GeoPoint(spot.getX(), spot.getY());
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Resource check as per original code
            int markerResId = context.getResources().getIdentifier("marker_default", "drawable", context.getPackageName());
            if (markerResId == 0) markerResId = android.R.drawable.ic_dialog_map; 
            
            Drawable icon = ContextCompat.getDrawable(context, markerResId);
            if (icon != null) {
                icon = DrawableCompat.wrap(icon).mutate();
                int colorRes = spot.isEmpty() ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                DrawableCompat.setTint(icon, ContextCompat.getColor(context, colorRes));
                marker.setIcon(icon);
                marker.setTitle(spot.isEmpty() ? "חניה פנויה" : "חניה תפוסה");
                marker.setSnippet("לחץ לניווט");
            }

            marker.setOnMarkerClickListener((m, mv) -> {
                drawRouteToSpot(m.getPosition(), mLocationOverlay);
                m.showInfoWindow();
                return true;
            });

            markersFolder.add(marker);
        }
        map.invalidate();
    }

    public void drawRouteToSpot(GeoPoint destination, MyLocationNewOverlay mLocationOverlay) {
        if (mLocationOverlay == null || mLocationOverlay.getMyLocation() == null) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "מיקום לא ידוע", Toast.LENGTH_SHORT).show());
            }
            return;
        }

        executor.execute(() -> {
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(mLocationOverlay.getMyLocation());
            waypoints.add(destination);

            // OSRM is the standard routing engine in OSMBonusPack
            RoadManager roadManager = new OSRMRoadManager(context, "ParkingFinder");
            ((OSRMRoadManager)roadManager).setMean(OSRMRoadManager.MEAN_BY_CAR);
            
            Road road = roadManager.getRoad(waypoints);
            
            if (road.mStatus != Road.STATUS_OK) {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() -> 
                        Toast.makeText(context, "שגיאה בחישוב מסלול", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

            if (context instanceof MainActivity) {
                ((MainActivity) context).runOnUiThread(() -> {
                    if (currentRoadOverlay != null) map.getOverlays().remove(currentRoadOverlay);
                    currentRoadOverlay = roadOverlay;
                    if (currentRoadOverlay != null) {
                        map.getOverlays().add(currentRoadOverlay);
                        map.invalidate();
                    }
                });
            }
        });
    }

    public void saveSearchToHistory(String name, GeoPoint point) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("lat", point.getLatitude());
        values.put("lon", point.getLongitude());
        context.getContentResolver().insert(SearchHistoryProvider.CONTENT_URI, values);
    }

    public List<ParkingSpot> getAllParkingSpots() {
        return allParkingSpots;
    }
    
    public void animateTo(GeoPoint point) {
        map.getController().animateTo(point);
    }
}
