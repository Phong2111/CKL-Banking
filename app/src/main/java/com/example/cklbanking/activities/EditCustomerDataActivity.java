package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.User;
import com.example.cklbanking.repositories.UserRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditCustomerDataActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText editFullName, editEmail, editPhone;
    private TextInputEditText editDateOfBirth, editIdNumber, editAddress;
    private MaterialButton btnSave;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseFirestore db;
    private UserRepository userRepository;

    // Data
    private String customerId;
    private User customer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer_data);

        // Get customer ID from intent
        customerId = getIntent().getStringExtra("customer_id");
        if (customerId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin khách hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();

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
        editFullName = findViewById(R.id.editFullName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editDateOfBirth = findViewById(R.id.editDateOfBirth);
        editIdNumber = findViewById(R.id.editIdNumber);
        editAddress = findViewById(R.id.editAddress);
        btnSave = findViewById(R.id.btnSave);
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
        btnSave.setOnClickListener(v -> saveCustomerData());
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
                            populateFields();
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

    private void populateFields() {
        if (customer == null) return;

        editFullName.setText(customer.getFullName());
        editEmail.setText(customer.getEmail());
        editPhone.setText(customer.getPhone());
        // Extended fields - these might not be in User model yet
        editDateOfBirth.setText("");
        editIdNumber.setText("");
        editAddress.setText("");
    }

    private void saveCustomerData() {
        // Validate inputs
        String fullName = editFullName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            editFullName.setError("Họ tên không được để trống");
            editFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editPhone.setError("Số điện thoại không được để trống");
            editPhone.requestFocus();
            return;
        }

        showLoading(true);

        // Update user data
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        // Add extended fields if needed
        String dateOfBirth = editDateOfBirth.getText().toString().trim();
        String idNumber = editIdNumber.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (!TextUtils.isEmpty(dateOfBirth)) {
            updates.put("dateOfBirth", dateOfBirth);
        }
        if (!TextUtils.isEmpty(idNumber)) {
            updates.put("idNumber", idNumber);
        }
        if (!TextUtils.isEmpty(address)) {
            updates.put("address", address);
        }

        db.collection("users")
                .document(customerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Cập nhật thông tin khách hàng thành công!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }
}












