package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;

public class OfficerCustomerDetailActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextView profileName, profileEmail, profilePhone, profileDob;
    private TextView profileIdNumber, profileAddress;
    private MaterialButton btnEditCustomer;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private String customerId;
    private User customer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_customer_detail);

        // Get customer ID from intent
        customerId = getIntent().getStringExtra("customer_id");
        if (customerId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin khách hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Listeners
        setupListeners();

        // Load customer data
        loadCustomerData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        profileDob = findViewById(R.id.profileDob);
        profileIdNumber = findViewById(R.id.profileIdNumber);
        profileAddress = findViewById(R.id.profileAddress);
        btnEditCustomer = findViewById(R.id.btnEditCustomer);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnEditCustomer.setOnClickListener(v -> openEditCustomer());
    }

    private void loadCustomerData() {
        showLoading(true);

        db.collection("users")
                .document(customerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        customer = documentSnapshot.toObject(User.class);
                        if (customer != null) {
                            customer.setUserId(customerId);
                            updateUI();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin khách hàng",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        if (customer == null) return;

        profileName.setText(customer.getFullName() != null ? customer.getFullName() : "N/A");
        profileEmail.setText(customer.getEmail() != null ? customer.getEmail() : "N/A");
        profilePhone.setText(customer.getPhone() != null ? customer.getPhone() : "Chưa cập nhật");
        profileDob.setText("Chưa cập nhật");
        profileIdNumber.setText("Chưa cập nhật");
        profileAddress.setText("Chưa cập nhật");
    }

    private void openEditCustomer() {
        Intent intent = new Intent(this, EditCustomerDataActivity.class);
        intent.putExtra("customer_id", customerId);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload customer data when returning from edit
        if (customerId != null) {
            loadCustomerData();
        }
    }
}





