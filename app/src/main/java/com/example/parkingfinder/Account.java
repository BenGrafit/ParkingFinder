package com.example.parkingfinder;

/**
 * Data model for User Account.
 */
public class Account {
    private String email;
    private String password;

    // Default constructor required for Firestore
    public Account() {}

    public Account(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
