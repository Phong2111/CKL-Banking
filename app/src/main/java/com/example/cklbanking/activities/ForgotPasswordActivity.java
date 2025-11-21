package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText editEmail;
    private MaterialButton btnResetPassword, btnBackToLogin;
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

        btnResetPassword.setEnabled(false);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnResetPassword.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, 
                            "Email đặt lại mật khẩu đã được gửi!", 
                            Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, 
                            "Lỗi: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
