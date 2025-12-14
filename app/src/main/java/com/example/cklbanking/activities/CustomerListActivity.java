package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerListActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewCustomers;
    private CircularProgressIndicator progressBar;
    private TextView textEmpty;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<User> customers;
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Load customers
        loadCustomers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewCustomers = findViewById(R.id.recyclerViewCustomers);
        progressBar = findViewById(R.id.progressBar);
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

    private void loadCustomers() {
        showLoading(true);
        textEmpty.setVisibility(View.GONE);

        db.collection("users")
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customers.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());
                        customers.add(user);
                    }
                    adapter.notifyDataSetChanged();
                    textEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    textEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}





