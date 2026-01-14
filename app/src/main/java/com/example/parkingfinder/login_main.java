package com.example.parkingfinder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.GnssAntennaInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;

import java.util.List;

public class login_main extends AppCompatActivity {


    private FireStoreHelper helper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);


        helper = new FireStoreHelper();
        EditText Email = findViewById(R.id.Email);
        EditText Password = findViewById(R.id.Password);
        CheckBox stay =findViewById(R.id.stay);
        Button Signin = findViewById(R.id.Signin);
        TextView Signup = findViewById(R.id.noAccount);



        Signin.setOnClickListener(v ->{
            Account thisUser = new Account(Email.getText().toString().trim(), Password.getText().toString().trim());
            Intent intent = new Intent(this, MainActivity.class);
            helper.AccountExists(thisUser,
                    e -> Toast.makeText(this,
                            "שגיאה: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show()
                    ,
                    exists -> {

                    if (exists) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "משתמש לא נמצא", Toast.LENGTH_SHORT).show();
                    }}


                    );


        });

        Signup.setOnClickListener(v -> {
            Adapter.showSignUp(this, account -> {
                helper.AddAccount(
                        account.getEmail().trim(),
                        account.getPassword().trim(),
                        ref -> Toast.makeText(this, "משתמש נוספה בהצלחה", Toast.LENGTH_SHORT).show(),
                        e -> Toast.makeText(this, "שגיאה: " , Toast.LENGTH_SHORT).show());



            });
        });

    }




}