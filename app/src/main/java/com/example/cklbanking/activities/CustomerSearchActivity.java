package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.CustomerAdapter;
import com.example.cklbanking.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerSearchActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText editSearch;
    private MaterialButton btnSearch;
    private RecyclerView recyclerViewCustomers;
    private CircularProgressIndicator progressBar;
    private TextView textResultsTitle, textEmpty;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<User> customers;
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_search);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Listeners
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editSearch = findViewById(R.id.editSearch);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerViewCustomers = findViewById(R.id.recyclerViewCustomers);
        progressBar = findViewById(R.id.progressBar);
        textResultsTitle = findViewById(R.id.textResultsTitle);
        textEmpty = findViewById(R.id.textEmpty);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        customers = new ArrayList<>();
        adapter = new CustomerAdapter(this, customers);
        adapter.setOnCustomerClickListener(customer -> {
            // Open customer detail
            Intent intent = new Intent(this, OfficerCustomerDetailActivity.class);
            intent.putExtra("customer_id", customer.getUserId());
            startActivity(intent);
        });
        recyclerViewCustomers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCustomers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> performSearch());
        
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String searchQuery = editSearch.getText().toString().trim();

        if (TextUtils.isEmpty(searchQuery)) {
            editSearch.setError("Vui lòng nhập thông tin tìm kiếm");
            editSearch.requestFocus();
            return;
        }

        showLoading(true);
        textEmpty.setVisibility(View.GONE);
        textResultsTitle.setVisibility(View.GONE);
        customers.clear();
        adapter.notifyDataSetChanged();

        // Try to search by email first
        db.collection("users")
                .whereEqualTo("role", "customer")
                .whereEqualTo("email", searchQuery)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        processSearchResults(queryDocumentSnapshots);
                        return;
                    }

                    // If not found by email, try phone
                    db.collection("users")
                            .whereEqualTo("role", "customer")
                            .whereEqualTo("phone", searchQuery)
                            .get()
                            .addOnSuccessListener(phoneSnapshots -> {
                                if (!phoneSnapshots.isEmpty()) {
                                    processSearchResults(phoneSnapshots);
                                    return;
                                }

                                // If not found by phone, try name (partial match)
                                db.collection("users")
                                        .whereEqualTo("role", "customer")
                                        .get()
                                        .addOnSuccessListener(allSnapshots -> {
                                            List<User> filtered = new ArrayList<>();
                                            String queryLower = searchQuery.toLowerCase();
                                            for (QueryDocumentSnapshot doc : allSnapshots) {
                                                User user = doc.toObject(User.class);
                                                if (user.getFullName() != null && 
                                                    user.getFullName().toLowerCase().contains(queryLower)) {
                                                    filtered.add(user);
                                                }
                                            }
                                            if (!filtered.isEmpty()) {
                                                customers.addAll(filtered);
                                                adapter.notifyDataSetChanged();
                                                textResultsTitle.setVisibility(View.VISIBLE);
                                                textEmpty.setVisibility(View.GONE);
                                            } else {
                                                textEmpty.setText("Không tìm thấy khách hàng nào");
                                                textEmpty.setVisibility(View.VISIBLE);
                                            }
                                            showLoading(false);
                                        })
                                        .addOnFailureListener(e -> {
                                            showLoading(false);
                                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processSearchResults(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        customers.clear();
        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
            User user = doc.toObject(User.class);
            user.setUserId(doc.getId());
            customers.add(user);
        }
        adapter.notifyDataSetChanged();
        textResultsTitle.setVisibility(View.VISIBLE);
        textEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
        if (customers.isEmpty()) {
            textEmpty.setText("Không tìm thấy khách hàng nào");
        }
        showLoading(false);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!show);
    }
}





