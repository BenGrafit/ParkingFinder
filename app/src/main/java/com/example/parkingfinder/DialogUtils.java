package com.example.parkingfinder;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

/**
 * DialogUtils - Helper class for showing various dialogs in the application.
 */
public class DialogUtils {

    public interface OnSpotAddedListener {
        void onSpotAdded(ParkingSpot spot);
    }

    public interface OnAccountCreatedListener {
        void onAccountCreated(Account account);
    }

    public static void showAddSpotDialog(Context context, MyLocationNewOverlay locationOverlay, OnSpotAddedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_spot, null);

        EditText edtX = view.findViewById(R.id.edtX);
        EditText edtY = view.findViewById(R.id.edtY);
        Button btnUseCurrentLocation = view.findViewById(R.id.btnUseCurrentLocation);

        btnUseCurrentLocation.setOnClickListener(v -> {
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                GeoPoint myLoc = locationOverlay.getMyLocation();
                edtX.setText(String.valueOf(myLoc.getLatitude()));
                edtY.setText(String.valueOf(myLoc.getLongitude()));
            } else {
                Toast.makeText(context, "המיקום אינו זמין כרגע", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הוסף מקום חניה חדש");
        builder.setView(view);

        builder.setPositiveButton("הוסף", (dialog, which) -> {
            try {
                String xStr = edtX.getText().toString();
                String yStr = edtY.getText().toString();
                
                if (TextUtils.isEmpty(xStr) || TextUtils.isEmpty(yStr)) {
                    Toast.makeText(context, "אנא הזן קואורדינטות", Toast.LENGTH_SHORT).show();
                    return;
                }

                double x = Double.parseDouble(xStr);
                double y = Double.parseDouble(yStr);

                ParkingSpot spot = new ParkingSpot(x, y);
                listener.onSpotAdded(spot);

            } catch (NumberFormatException e) {
                Toast.makeText(context, "אנא הזן ערכים תקינים", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    public static void showSignUpDialog(Context context, OnAccountCreatedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_signup, null);

        EditText emailEditText = view.findViewById(R.id.EmailS);
        EditText passwordEditText = view.findViewById(R.id.PasswordS);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הירשם");
        builder.setView(view);
        builder.setPositiveButton("הוסף", null); 
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "אנא הזן כתובת אימייל תקינה", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean hasUppercase = password.matches(".*[A-Z].*");
            boolean hasDigit = password.matches(".*[0-9].*");

            if (password.length() <= 6 || !hasUppercase || !hasDigit) {
                Toast.makeText(context, "הסיסמה חייבת להיות באורך של מעל 6 תווים, ולכלול אות גדולה ומספר", Toast.LENGTH_LONG).show();
                return;
            }

            Account account = new Account(email, password);
            listener.onAccountCreated(account);
            dialog.dismiss();
        });
    }
}
