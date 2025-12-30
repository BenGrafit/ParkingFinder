package com.example.parkingfinder;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Adapter {

    public interface OnSpotAddedListener {
        void onSpotAdded(ParkingSpot spot);
    }
    public interface  OnAccountCreatedListener{
        void onAccountCreated(Account account);
    }

    public static void showAddDialog(Context context, OnSpotAddedListener listener) {
        // Inflate the custom layout for the dialog
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_spot, null);

        // Get the EditText fields from the layout
        EditText edtX = view.findViewById(R.id.edtX);
        EditText edtY = view.findViewById(R.id.edtY);

        // Create and configure the AlertDialog
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
    public static void showSignUp(Context context, OnAccountCreatedListener listener){
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_signup, null);

        EditText Email = view.findViewById(R.id.EmailS);
        EditText Password = view.findViewById(R.id.PasswordS);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הירשם");
        builder.setView(view);

        builder.setPositiveButton("הוסף", (dialog, which) -> {
            try {
                String email = Email.getText().toString().trim();
                String password = Password.getText().toString().trim();

                Account account = new Account(email, password);
                listener.onAccountCreated(account);




            } catch (NumberFormatException e) {
                Toast.makeText(context, "אנא הזן ערכים תקינים", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        builder.create().show();

    }
}
