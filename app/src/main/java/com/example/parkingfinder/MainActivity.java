package com.example.parkingfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.view.Menu;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkingfinder.Adapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;






import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ParkingSpotAdapter.OnParkingSpotAction {

    private RecyclerView recyclerView;
    private ParkingSpotAdapter adapter;
    private FireStoreHelper helper;
    private FloatingActionButton addSpotBtn;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול FirestoreHelper
        helper = new FireStoreHelper();

        // אתחול RecyclerView וה-Adapter
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new ParkingSpotAdapter(this); // this = מימוש OnParkingSpotAction
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        addSpotBtn = findViewById(R.id.AddSpot);

        // האזנה בזמן אמת לשינויים ב-ParkingSpots
        helper.listenToParkingSpots(new ParkingSpotListener() {
            @Override
            public void onUpdate(List<ParkingSpot> updatedList) {
                adapter.submitList(updatedList);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this,
                        "שגיאה: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        addSpotBtn.setOnClickListener(v ->
                Adapter.showAddDialog(this, spot -> {
                    // This code runs when the user clicks "Add" in the dialog
                    helper.addParkingSpot(
                            spot.getX(),
                            spot.getY(),
                            ref -> Toast.makeText(this, "חניה נוספה בהצלחה", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                })
        );



    }



    // לחיצה רגילה על מקום חנייה
    @Override
    public void onClick(ParkingSpot spot) {
        Toast.makeText(this, "נבחר מקום חנייה: x=" + spot.getX() + ", y=" + spot.getY(),
                Toast.LENGTH_SHORT).show();
    }


    // לחיצה ארוכה על מקום חנייה

    @Override
    public void onLongClick(ParkingSpot spot) {
        // First dialog: choose Delete or Go Back
        new AlertDialog.Builder(this)
                .setTitle("אפשרויות מקום חנייה")
                .setMessage("מה תרצה לעשות עם מקום החנייה הזה?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    // Confirmation dialog before actual deletion
                    new AlertDialog.Builder(this)
                            .setTitle("אישור מחיקה")
                            .setMessage("האם אתה בטוח שברצונך למחוק את מקום החנייה?")
                            .setPositiveButton("כן", (confirmDialog, confirmWhich) -> {
                                // Delete from Firestore
                                helper.deleteParkingSpot(
                                        spot.getId(),  // <-- use Firebase document ID here
                                        a -> Toast.makeText(this, "מקום חנייה נמחק", Toast.LENGTH_SHORT).show(),
                                        e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );

                            })
                            .setNegativeButton("לא", null)
                            .show();
                })
                .setNegativeButton("חזור", null)
                .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.stopListening(); // עצירת האזנה בזמן יציאה
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu); // <-- your menu file
        return true; // must return true
    }

}
