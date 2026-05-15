package com.example.parkingfinder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity - Manages the map, search, and parking spot updates.
 */
public class MainActivity extends AppCompatActivity {

    private MapView map;
    private EditText addressSearch;
    private ImageButton searchButton;
    private ImageButton voiceSearchButton;
    private FloatingActionButton addSpotBtn;
    private FloatingActionButton centerLocationBtn;
    private FloatingActionButton btnListView;
    private RecyclerView suggestionsRecyclerView;
    private SuggestionsAdapter suggestionsAdapter;

    private FirebaseRepository repository;
    private MapManager mapManager;
    private LocationHelper locationHelper;
    private SearchHandler searchHandler;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                locationHelper.setupLocationOverlay();
            });

    private final ActivityResultLauncher<Intent> speechResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        addressSearch.setText(matches.get(0));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        initUI();
        initManagers();
        setupListeners();
    }

    private void initUI() {
        map = findViewById(R.id.map);
        addressSearch = findViewById(R.id.addressSearch);
        searchButton = findViewById(R.id.searchButton);
        voiceSearchButton = findViewById(R.id.voiceSearchButton);
        addSpotBtn = findViewById(R.id.AddSpot);
        centerLocationBtn = findViewById(R.id.CenterLocation);
        btnListView = findViewById(R.id.btnListView);
        suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView);
    }

    private void initManagers() {
        repository = new FirebaseRepository();
        mapManager = new MapManager(map, this, executor);
        locationHelper = new LocationHelper(this, map);
        
        suggestionsAdapter = new SuggestionsAdapter(suggestion -> {
            addressSearch.setText(suggestion);
            suggestionsRecyclerView.setVisibility(View.GONE);
            String query = suggestion;
            if (query.contains(" (")) query = query.substring(0, query.lastIndexOf(" ("));
            
            searchHandler.searchNearestParking(query, mapManager);
        });

        suggestionsRecyclerView.setAdapter(suggestionsAdapter);

        searchHandler = new SearchHandler(this, addressSearch, executor, suggestionsAdapter, suggestionsRecyclerView);

        mapManager.initMap();
        locationHelper.handlePermissions(requestPermissionLauncher);
        locationHelper.setupLocationOverlay();
    }

    private void setupListeners() {
        addressSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    searchHandler.loadSearchHistory();
                } else if (s.length() >= 3) {
                    searchHandler.fetchSuggestions(s.toString(), locationHelper.getLocationOverlay());
                } else {
                    suggestionsRecyclerView.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        addressSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && addressSearch.getText().length() == 0) {
                searchHandler.loadSearchHistory();
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = addressSearch.getText().toString();
            if (query.contains(" (")) query = query.substring(0, query.lastIndexOf(" ("));
            if (!query.isEmpty()) {
                suggestionsRecyclerView.setVisibility(View.GONE);
                searchHandler.searchNearestParking(query, mapManager);
            }
        });

        voiceSearchButton.setOnClickListener(v -> searchHandler.startVoiceRecognition(speechResultLauncher));

        addSpotBtn.setOnClickListener(v -> DialogUtils.showAddSpotDialog(this, locationHelper.getLocationOverlay(), spot -> {
            repository.addParkingSpot(spot.getX(), spot.getY(),
                ref -> Toast.makeText(this, "חניה נוספה בהצלחה", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }));

        centerLocationBtn.setOnClickListener(v -> locationHelper.centerOnLocation());

        btnListView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ParkingListActivity.class);
            startActivity(intent);
        });

        repository.listenToParkingSpots(new ParkingSpotListener() {
            @Override
            public void onUpdate(List<ParkingSpot> updatedList) {
                mapManager.updateMapMarkers(updatedList, locationHelper.getLocationOverlay());
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onParkingFound(GeoPoint spot, GeoPoint dest) {
        mapManager.drawRouteToSpot(spot, locationHelper.getLocationOverlay());
        mapManager.animateTo(dest);
        // Set the target spot for proximity notification
        locationHelper.setTargetSpot(spot);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        locationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        locationHelper.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        repository.stopListening();
    }
}
