package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Delay and navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkAuthAndNavigate();
        }, SPLASH_DELAY);
    }

    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (currentUser != null) {
            // User is signed in, check role and navigate accordingly
            checkUserRoleAndNavigate(currentUser.getUid());
        } else {
            // No user signed in, go to login
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void checkUserRoleAndNavigate(String userId) {
        android.util.Log.d("SplashActivity", "Checking user role for userId: " + userId);
        
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Intent intent;
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        android.util.Log.d("SplashActivity", "User role: " + role);
                        
                        if ("customer".equals(role)) {
                            // Navigate to Customer Dashboard (không check eKYC ở đây, để user vào được app)
                            android.util.Log.d("SplashActivity", "Navigating to CustomerDashboard");
                            intent = new Intent(SplashActivity.this, CustomerDashboardActivity.class);
                        } else if ("staff".equals(role) || "officer".equals(role)) {
                            // Navigate to Officer Dashboard
                            android.util.Log.d("SplashActivity", "Navigating to OfficerDashboard");
                            intent = new Intent(SplashActivity.this, OfficerDashboardActivity.class);
                        } else {
                            // Unknown role, go to login
                            android.util.Log.w("SplashActivity", "Unknown role: " + role);
                            intent = new Intent(SplashActivity.this, LoginActivity.class);
                        }
                    } else {
                        // User document not found, go to login
                        android.util.Log.w("SplashActivity", "User document not found in Firestore");
                        intent = new Intent(SplashActivity.this, LoginActivity.class);
                    }
                    
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Error loading user, go to login
                    android.util.Log.e("SplashActivity", "Error loading user from Firestore", e);
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
    }
}
