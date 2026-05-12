package com.example.parkingfinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * FirebaseRepository - Handles all database interactions with Firestore.
 */
public class FirebaseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference spotsRef = db.collection("parkingSpots");
    private final CollectionReference accountsRef = db.collection("logininfo");
    private ListenerRegistration liveRegistration;

    public void addParkingSpot(double x, double y,
                               OnSuccessListener<DocumentReference> onSuccess,
                               OnFailureListener onFailure) {
        ParkingSpot spot = new ParkingSpot(x, y);
        spotsRef.add(spot)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void listenToParkingSpots(ParkingSpotListener listener) {
        stopListening();

        liveRegistration = spotsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (value == null) return;

            List<ParkingSpot> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                ParkingSpot spot = doc.toObject(ParkingSpot.class);
                if (spot != null) {
                    spot.setId(doc.getId());
                    list.add(spot);
                }
            }
            listener.onUpdate(list);
        });
    }

    public void stopListening() {
        if (liveRegistration != null) {
            liveRegistration.remove();
            liveRegistration = null;
        }
    }

    public void addAccount(String email, String password,
                           OnSuccessListener<DocumentReference> onSuccess,
                           OnFailureListener onFailure) {
        Account newAccount = new Account(email, password);
        accountsRef.add(newAccount)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void checkAccountExists(Account user, OnFailureListener onFailure, AccountListener listener) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) return;
        
        accountsRef
                .whereEqualTo("email", user.getEmail().trim())
                .whereEqualTo("password", user.getPassword().trim())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onResult(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(onFailure);
    }
}
