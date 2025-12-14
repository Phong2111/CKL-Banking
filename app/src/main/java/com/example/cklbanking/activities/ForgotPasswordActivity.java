package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";
    
    private TextInputEditText editEmail;
    private MaterialButton btnResetPassword, btnBackToLogin;
    private CircularProgressIndicator progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        editEmail = findViewById(R.id.editEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progressBar = findViewById(R.id.progressBar);
        
        // Hide progress bar if not in layout
        if (progressBar != null) {
            progressBar.setVisibility(android.view.View.GONE);
        }
    }

    private void setupListeners() {
        btnResetPassword.setOnClickListener(v -> resetPassword());
        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void resetPassword() {
        String email = editEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Vui lòng nhập email");
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Email không hợp lệ");
            return;
        }

        Log.d(TAG, "Starting password reset for: " + email);
        showLoading(true);
        btnResetPassword.setEnabled(false);

        // Tạo password reset request trong Firestore
        // Cloud Function sẽ tự động trigger và gửi email
        com.google.firebase.firestore.FirebaseFirestore db = 
            com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        String requestId = "reset_" + System.currentTimeMillis();
        Map<String, Object> resetRequest = new HashMap<>();
        resetRequest.put("email", email);
        resetRequest.put("status", "pending");
        resetRequest.put("createdAt", com.google.firebase.Timestamp.now());
        
        Log.d(TAG, "Creating password reset request: " + requestId);
        
        // Set timeout để tránh treo
        android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (btnResetPassword != null && !btnResetPassword.isEnabled()) {
                Log.w(TAG, "Timeout waiting for Firestore response");
                showLoading(false);
                btnResetPassword.setEnabled(true);
                Toast.makeText(this, 
                    "Kết nối chậm. Đang thử phương pháp khác...", 
                    Toast.LENGTH_SHORT).show();
                // Fallback to Firebase Auth
                tryFirebaseAuthMethod(email);
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 seconds timeout
        
        db.collection("password_reset_requests")
                .document(requestId)
                .set(resetRequest)
                .addOnSuccessListener(aVoid -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    Log.d(TAG, "Password reset request created successfully: " + requestId);
                    
                    // Hiển thị thông báo ngay lập tức
                    Toast.makeText(this, 
                        "Đang xử lý yêu cầu...", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Đợi một chút để function xử lý
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        // Kiểm tra status
                        checkPasswordResetStatus(requestId, email);
                    }, 2000);
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    showLoading(false);
                    btnResetPassword.setEnabled(true);
                    Log.e(TAG, "Error creating password reset request", e);
                    Log.e(TAG, "Error details: " + e.getMessage(), e);
                    
                    // Hiển thị lỗi cụ thể
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("UNAVAILABLE")) {
                        Toast.makeText(this, 
                            "Không thể kết nối đến server. Vui lòng kiểm tra emulator đang chạy.", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, 
                            "Lỗi: " + (errorMsg != null ? errorMsg : "Không xác định"), 
                            Toast.LENGTH_SHORT).show();
                    }
                    
                    // Fallback to Firebase Auth
                    tryFirebaseAuthMethod(email);
                });
    }
    
    /**
     * Fallback method: Use Firebase Auth built-in reset password
     */
    private void tryFirebaseAuthMethod(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, 
                            "Email đặt lại mật khẩu đã được gửi!", 
                            Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? 
                            task.getException().getMessage() : "Lỗi không xác định";
                        Toast.makeText(this, 
                            "Lỗi: " + errorMessage + 
                            "\nVui lòng kiểm tra email đã đăng ký hoặc thử lại sau.", 
                            Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Firebase Auth reset password failed", task.getException());
                    }
                });
    }
    
    private int checkAttempts = 0;
    private static final int MAX_CHECK_ATTEMPTS = 5;
    
    private void checkPasswordResetStatus(String requestId, String email) {
        checkAttempts++;
        Log.d(TAG, "Checking password reset status (attempt " + checkAttempts + "/" + MAX_CHECK_ATTEMPTS + "): " + requestId);
        
        com.google.firebase.firestore.FirebaseFirestore db = 
            com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        db.collection("password_reset_requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        Log.d(TAG, "Password reset status: " + status);
                        
                        if ("sent".equals(status)) {
                            showLoading(false);
                            btnResetPassword.setEnabled(true);
                            checkAttempts = 0;
                            Toast.makeText(this, 
                                "Email đặt lại mật khẩu đã được gửi đến " + email + 
                                "\nVui lòng kiểm tra hộp thư của bạn.", 
                                Toast.LENGTH_LONG).show();
                            finish();
                        } else if ("failed".equals(status)) {
                            String error = documentSnapshot.getString("error");
                            Log.e(TAG, "Password reset email failed: " + error);
                            showLoading(false);
                            btnResetPassword.setEnabled(true);
                            checkAttempts = 0;
                            // Fallback to Firebase Auth
                            tryFirebaseAuthMethod(email);
                        } else if (checkAttempts < MAX_CHECK_ATTEMPTS) {
                            // Still processing, check again after 3 seconds
                            Log.d(TAG, "Still processing, will check again in 3 seconds...");
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                .postDelayed(() -> {
                                    checkPasswordResetStatus(requestId, email);
                                }, 3000);
                        } else {
                            // Max attempts reached
                            Log.w(TAG, "Max check attempts reached, falling back to Firebase Auth");
                            showLoading(false);
                            btnResetPassword.setEnabled(true);
                            checkAttempts = 0;
                            Toast.makeText(this, 
                                "Xử lý quá lâu. Đang thử phương pháp khác...", 
                                Toast.LENGTH_SHORT).show();
                            tryFirebaseAuthMethod(email);
                        }
                    } else {
                        // Document not found
                        Log.w(TAG, "Password reset request document not found: " + requestId);
                        showLoading(false);
                        btnResetPassword.setEnabled(true);
                        checkAttempts = 0;
                        // Fallback to Firebase Auth
                        tryFirebaseAuthMethod(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking password reset status", e);
                    if (checkAttempts < MAX_CHECK_ATTEMPTS) {
                        // Retry
                        new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> {
                                checkPasswordResetStatus(requestId, email);
                            }, 3000);
                    } else {
                        showLoading(false);
                        btnResetPassword.setEnabled(true);
                        checkAttempts = 0;
                        // Fallback to Firebase Auth
                        tryFirebaseAuthMethod(email);
                    }
                });
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        btnResetPassword.setEnabled(!show);
    }
}
