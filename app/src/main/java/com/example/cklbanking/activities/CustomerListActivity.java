package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.CustomerAdapter;
import com.example.cklbanking.models.User;
import com.example.cklbanking.utils.ErrorHandler;
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
    
    // Pagination
    private static final int PAGE_SIZE = 20;
    private com.google.firebase.firestore.QueryDocumentSnapshot lastDocument;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewCustomers.setLayoutManager(layoutManager);
        recyclerViewCustomers.setAdapter(adapter);
        
        // Add scroll listener for pagination
        recyclerViewCustomers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // Load more when user scrolls near the end
                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreCustomers();
                    }
                }
            }
        });
    }

    private void loadCustomers() {
        if (isLoading) return;
        
        showLoading(true);
        isLoading = true;
        textEmpty.setVisibility(View.GONE);
        customers.clear();
        lastDocument = null;
        hasMoreData = true;

        db.collection("users")
                .whereEqualTo("role", "customer")
                .orderBy("fullName")
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    showLoading(false);
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());
                        customers.add(user);
                    }
                    
                    // Update last document for pagination
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    adapter.notifyDataSetChanged();
                    textEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    showLoading(false);
                    ErrorHandler.handleError(this, e, "Lỗi tải danh sách khách hàng");
                    textEmpty.setVisibility(View.VISIBLE);
                });
    }
    
    private void loadMoreCustomers() {
        if (isLoading || !hasMoreData || lastDocument == null) return;
        
        isLoading = true;

        db.collection("users")
                .whereEqualTo("role", "customer")
                .orderBy("fullName")
                .startAfter(lastDocument)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());
                        customers.add(user);
                    }
                    
                    // Update last document for pagination
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    adapter.notifyDataSetChanged();
                    textEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    ErrorHandler.handleError(this, e, "Lỗi tải thêm khách hàng");
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
}











