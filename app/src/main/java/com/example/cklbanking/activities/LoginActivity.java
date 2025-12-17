package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.utils.AnimationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    private EditText editEmail, editPassword;
    private MaterialButton btnLogin;
    private TextView btnRegister, btnForgotPassword;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.getUid());
            return;
        }

        // Initialize UI
        initViews();
        setupListeners();
    }

    private void initViews() {
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            AnimationHelper.applyActivityTransition(this);
        });
        btnForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
            AnimationHelper.applyActivityTransition(this);
        });
    }

    private void loginUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Vui lòng nhập email");
            editEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Vui lòng nhập mật khẩu");
            editPassword.requestFocus();
            return;
        }

        // Show progress
        showProgress(true);

        // Sign in with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleAndNavigate(user.getUid());
                        }
                    } else {
                        // Sign in failed
                        showProgress(false);
                        Toast.makeText(LoginActivity.this, 
                            "Đăng nhập thất bại: " + task.getException().getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRoleAndNavigate(String userId) {
        android.util.Log.d("LoginActivity", "Checking user role for userId: " + userId);
        
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    showProgress(false);
                    
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        android.util.Log.d("LoginActivity", "User role: " + role);
                        
                        if ("customer".equals(role)) {
                            // Navigate to Customer Dashboard (không bắt buộc eKYC để vào app)
                            // eKYC sẽ được check khi cần thiết (ví dụ: giao dịch lớn)
                            android.util.Log.d("LoginActivity", "Navigating to CustomerDashboard");
                            Intent intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            AnimationHelper.applyActivityTransition(this);
                            finish();
                        } else if ("staff".equals(role) || "officer".equals(role)) {
                            // Navigate to Officer Dashboard
                            android.util.Log.d("LoginActivity", "Navigating to OfficerDashboard");
                            Intent intent = new Intent(LoginActivity.this, OfficerDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            AnimationHelper.applyActivityTransition(this);
                            finish();
                        } else {
                            // Unknown role
                            android.util.Log.w("LoginActivity", "Unknown role: " + role);
                            Toast.makeText(this, "Vai trò không xác định", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        // User document not found
                        android.util.Log.w("LoginActivity", "User document not found in Firestore");
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng. Vui lòng đăng ký lại.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    android.util.Log.e("LoginActivity", "Error loading user from Firestore", e);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
        btnForgotPassword.setEnabled(!show);
        editEmail.setEnabled(!show);
        editPassword.setEnabled(!show);
    }
}
