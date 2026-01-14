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

public class Adapter {

    public interface OnSpotAddedListener {
        void onSpotAdded(ParkingSpot spot);
    }
    public interface OnAccountCreatedListener {
        void onAccountCreated(Account account);
    }

    public static void showAddDialog(Context context, OnSpotAddedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_spot, null);

        EditText edtX = view.findViewById(R.id.edtX);
        EditText edtY = view.findViewById(R.id.edtY);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הוסף מקום חניה חדש");
        builder.setView(view);

        builder.setPositiveButton("הוסף", (dialog, which) -> {
            try {
                double x = Double.parseDouble(edtX.getText().toString());
                double y = Double.parseDouble(edtY.getText().toString());

                ParkingSpot spot = new ParkingSpot(x, y);
                listener.onSpotAdded(spot);

            } catch (NumberFormatException e) {
                Toast.makeText(context, "אנא הזן ערכים תקינים", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    public static void showSignUp(Context context, OnAccountCreatedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_signup, null);

        EditText Email = view.findViewById(R.id.EmailS);
        EditText Password = view.findViewById(R.id.PasswordS);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הירשם");
        builder.setView(view);
        builder.setPositiveButton("הוסף", null); // Set to null first to override later
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the button click to prevent auto-closing
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String email = Email.getText().toString().trim();
            String password = Password.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "אנא הזן כתובת אימייל תקינה", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validation for: length > 6, at least one capital letter, and at least one number
            boolean hasUppercase = password.matches(".*[A-Z].*");
            boolean hasDigit = password.matches(".*[0-9].*");

            if (password.length() <= 6 || !hasUppercase || !hasDigit) {
                Toast.makeText(context, "הסיסמה חייבת להיות באורך של מעל 6 תווים, ולכלול אות גדולה ומספר", Toast.LENGTH_LONG).show();
                return;
            }

            Account account = new Account(email, password);
            listener.onAccountCreated(account);
            dialog.dismiss(); // Only close if validation passes
        });
    }
}
