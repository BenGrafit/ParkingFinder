package com.example.parkingfinder;

import androidx.annotation.Nullable;
import android.util.Log;

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
 * 🔹 FireStoreHelper – אחראי לכל העבודה מול Firebase Firestore עבור ParkingSpot.
 */
public class FireStoreHelper {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference spotsRef = db.collection("parkingSpots");
    private final CollectionReference accountsRef = db.collection("logininfo");
    private ListenerRegistration liveRegistration;

    // ----------------------------------------------------
    // 🔸 יצירה (Create) – הוספת חניה חדשה
    // ----------------------------------------------------
    public void addParkingSpot(double x, double y,
                               OnSuccessListener<DocumentReference> onSuccess,
                               OnFailureListener onFailure) {

        ParkingSpot spot = new ParkingSpot(x, y);
        // IsEmpty כבר מוגדר ב-ParkingSpot כ-true
        spotsRef.add(spot)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);

    }



    // ----------------------------------------------------
    // 🔸 קריאה בזמן אמת (Read – realtime listener)
    // ----------------------------------------------------
    public void listenToParkingSpots(ParkingSpotListener listener) {
        stopListening();

        liveRegistration = spotsRef
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (value == null) return;

                    List<ParkingSpot> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        ParkingSpot spot = doc.toObject(ParkingSpot.class);
                        spot.setId(doc.getId()); // אפשר לשמור ID אם רוצים
                        list.add(spot);
                    }

                    listener.onUpdate(list);
                });
    }

    // ----------------------------------------------------
    // 🔸 קריאה חד-פעמית (ללא האזנה)
    // ----------------------------------------------------
    public void getAllOnce(OnSuccessListener<List<ParkingSpot>> onSuccess,
                           OnFailureListener onFailure) {

        spotsRef.get()
                .addOnSuccessListener(qs -> {
                    List<ParkingSpot> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        ParkingSpot spot = doc.toObject(ParkingSpot.class);
                        list.add(spot);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    // ----------------------------------------------------
    // 🔸 עדכון (Update)
    // ----------------------------------------------------
    public void updateParkingSpot(String id,
                                  @Nullable Boolean newIsEmpty,
                                  @Nullable Double newX,
                                  @Nullable Double newY,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {

        DocumentReference doc = spotsRef.document(id);

        if (newIsEmpty != null) doc.update("IsEmpty", newIsEmpty);
        if (newX != null) doc.update("x", newX);
        if (newY != null) doc.update("y", newY);

        // עדכון אחרון כדי להפעיל את ה-OnSuccess
        doc.update("x", newX != null ? newX : 0.0)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // ----------------------------------------------------
    // 🔸 מחיקה (Delete)
    // ----------------------------------------------------
    public void deleteParkingSpot(String id,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {

        spotsRef.document(id).delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // ----------------------------------------------------
    // 🔸 סינון – דוגמה: רק חניות פנויות
    // ----------------------------------------------------
    public void getAvailableOnly(OnSuccessListener<List<ParkingSpot>> onSuccess,
                                 OnFailureListener onFailure) {

        spotsRef.whereEqualTo("IsEmpty", true)
                .get()
                .addOnSuccessListener(qs -> {
                    List<ParkingSpot> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        ParkingSpot spot = doc.toObject(ParkingSpot.class);
                        list.add(spot);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    // ----------------------------------------------------
    // 🔸 הפסקת האזנה בזמן אמת
    // ----------------------------------------------------
    public void stopListening() {
        if (liveRegistration != null) {
            liveRegistration.remove();
            liveRegistration = null;
        }
    }




























    public void AddAccount(String Email, String Password,
                           OnSuccessListener<DocumentReference> onSuccess,
                           OnFailureListener onFailure) {

        Account newAccount = new Account(Email, Password);
        // IsEmpty כבר מוגדר ב-ParkingSpot כ-true
        accountsRef.add(newAccount)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);

    }

    public void AccountExists(Account thisUser, OnFailureListener onFailure, AccountListener Listener){
        accountsRef
                .whereEqualTo("email", thisUser.getEmail().trim())
                .whereEqualTo("password", thisUser.getPassword().trim())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean Exists = !querySnapshot.isEmpty();
                    Listener.onResult(Exists);
                })
                .addOnFailureListener(onFailure);




    }
}
