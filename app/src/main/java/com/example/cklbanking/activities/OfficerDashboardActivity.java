package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class OfficerDashboardActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextView welcomeName, totalCustomers, totalAccounts;
    private MaterialCardView cardCreateAccount, cardSearchCustomer, cardCustomerList, cardEditCustomer, cardManageInterestRates, cardEKYCSupport;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_dashboard);

        // Check permission - only officers can access
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            finish();
            return;
        }

        // Check role
        checkUserRole();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Listeners
        setupListeners();

        // Load data
        loadUserProfile();
        loadStatistics();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        welcomeName = findViewById(R.id.welcomeName);
        totalCustomers = findViewById(R.id.totalCustomers);
        totalAccounts = findViewById(R.id.totalAccounts);
        cardCreateAccount = findViewById(R.id.cardCreateAccount);
        cardSearchCustomer = findViewById(R.id.cardSearchCustomer);
        cardCustomerList = findViewById(R.id.cardCustomerList);
        cardEditCustomer = findViewById(R.id.cardEditCustomer);
        cardManageInterestRates = findViewById(R.id.cardManageInterestRates);
        cardEKYCSupport = findViewById(R.id.cardEKYCSupport);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupListeners() {
        cardCreateAccount.setOnClickListener(v -> openCreateAccount());
        cardSearchCustomer.setOnClickListener(v -> openSearchCustomer());
        cardCustomerList.setOnClickListener(v -> openCustomerList());
        cardEditCustomer.setOnClickListener(v -> {
            // First search, then edit
            openSearchCustomer();
        });
        cardManageInterestRates.setOnClickListener(v -> openManageInterestRates());
        cardEKYCSupport.setOnClickListener(v -> openEKYCSupport());
    }

    private void checkUserRole() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (!"staff".equals(role) && !"officer".equals(role)) {
                            // Not an officer, redirect to login
                            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            welcomeName.setText(currentUser.getFullName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadStatistics() {
        showLoading(true);

        // Count total customers
        db.collection("users")
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int customerCount = queryDocumentSnapshots.size();
                    totalCustomers.setText(String.valueOf(customerCount));

                    // Count total accounts
                    db.collection("accounts")
                            .get()
                            .addOnSuccessListener(accountSnapshots -> {
                                int accountCount = accountSnapshots.size();
                                totalAccounts.setText(String.valueOf(accountCount));
                                showLoading(false);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Lỗi tải thống kê: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải thống kê: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openCreateAccount() {
        Intent intent = new Intent(this, CreateAccountActivity.class);
        startActivity(intent);
    }

    private void openSearchCustomer() {
        Intent intent = new Intent(this, CustomerSearchActivity.class);
        startActivity(intent);
    }

    private void openCustomerList() {
        Intent intent = new Intent(this, CustomerListActivity.class);
        startActivity(intent);
    }

    private void openManageInterestRates() {
        Intent intent = new Intent(this, ManageInterestRatesActivity.class);
        startActivity(intent);
    }

    private void openEKYCSupport() {
        Intent intent = new Intent(this, OfficerEKYCSupportActivity.class);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload statistics when returning to dashboard
        loadStatistics();
    }
}

