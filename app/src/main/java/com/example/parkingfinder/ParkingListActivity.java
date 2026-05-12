package com.example.parkingfinder;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ParkingListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ParkingSpotAdapter adapter;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("רשימת חניות");
        }

        recyclerView = findViewById(R.id.recyclerViewParking);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ParkingSpotAdapter(new ParkingSpotAdapter.OnParkingSpotAction() {
            @Override
            public void onClick(ParkingSpot spot) {
                // Future action: click to show on map or details
                Toast.makeText(ParkingListActivity.this, "נבחר: " + spot.getId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongClick(ParkingSpot spot) {
                // Future action: long click to delete or edit
            }
        });
        recyclerView.setAdapter(adapter);

        repository = new FirebaseRepository();
        repository.listenToParkingSpots(new ParkingSpotListener() {
            @Override
            public void onUpdate(List<ParkingSpot> updatedList) {
                adapter.submitList(updatedList);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ParkingListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.stopListening();
        }
    }
}
