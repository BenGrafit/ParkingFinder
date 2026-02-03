package com.example.parkingfinder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class login_main extends AppCompatActivity {

    private static final String TAG = "GoogleSignIn";
    private FireStoreHelper helper;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Location permission is required for the map to work properly.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Log.e(TAG, "ID Token is null");
                            Toast.makeText(this, "Error: Google ID Token is null", Toast.LENGTH_LONG).show();
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed. Code: " + e.getStatusCode(), e);
                        String errorMsg = "Google Sign-In failed. Error code: " + e.getStatusCode();
                        if (e.getStatusCode() == 10) {
                            errorMsg += " (Check Web Client ID and SHA-1 in Firebase)";
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.w(TAG, "Sign-in result not OK: " + result.getResultCode());
                    Toast.makeText(this, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);

        // Request permissions immediately
        checkAndRequestPermissions();

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        helper = new FireStoreHelper();
        EditText Email = findViewById(R.id.Email);
        EditText Password = findViewById(R.id.Password);
        Button Signin = findViewById(R.id.Signin);
        TextView Signup = findViewById(R.id.noAccount);
        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);

        Signin.setOnClickListener(v -> {
            Account thisUser = new Account(Email.getText().toString().trim(), Password.getText().toString().trim());
            helper.AccountExists(thisUser,
                    e -> Toast.makeText(login_main.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(),
                    exists -> {
                        if (exists) {
                            Intent intent = new Intent(login_main.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(login_main.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        Signup.setOnClickListener(v -> Adapter.showSignUp(login_main.this, account -> helper.AddAccount(
                account.getEmail().trim(),
                account.getPassword().trim(),
                ref -> Toast.makeText(login_main.this, "User added successfully", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(login_main.this, "Error adding user", Toast.LENGTH_SHORT).show())));

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void signInWithGoogle() {
        // Sign out first to ensure the account picker always appears for testing
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String email = user != null ? user.getEmail() : "";
                        Toast.makeText(login_main.this, "Signed in: " + email, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(login_main.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Auth Failed";
                        Log.e(TAG, "Firebase auth failed", task.getException());
                        Toast.makeText(login_main.this, "Firebase Auth Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
