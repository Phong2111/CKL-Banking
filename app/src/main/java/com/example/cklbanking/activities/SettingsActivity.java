package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cklbanking.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private LinearLayout btnChangePassword, btnBiometric, btnNotifications, btnLanguage, btnAbout;
    private SwitchMaterial switchBiometric, switchNotifications;
    private MaterialButton btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnBiometric = findViewById(R.id.btnBiometric);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnLanguage = findViewById(R.id.btnLanguage);
        btnAbout = findViewById(R.id.btnAbout);
        switchBiometric = findViewById(R.id.switchBiometric);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnChangePassword.setOnClickListener(v -> 
            Toast.makeText(this, "Chức năng đổi mật khẩu đang phát triển", Toast.LENGTH_SHORT).show()
        );

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Sinh trắc học: " + (isChecked ? "Bật" : "Tắt"), Toast.LENGTH_SHORT).show()
        );

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Thông báo: " + (isChecked ? "Bật" : "Tắt"), Toast.LENGTH_SHORT).show()
        );

        btnLanguage.setOnClickListener(v -> 
            Toast.makeText(this, "Chức năng đổi ngôn ngữ đang phát triển", Toast.LENGTH_SHORT).show()
        );

        btnAbout.setOnClickListener(v -> 
            showAboutDialog()
        );

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("CKL Banking")
                .setMessage("Phiên bản: 1.0.0\n\n" +
                        "Ứng dụng ngân hàng trực tuyến\n" +
                        "© 2025 CKL Banking")
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
