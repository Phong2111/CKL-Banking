package com.example.cklbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.TransactionAdapter;
import com.example.cklbanking.models.Transaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipTransfer, chipDeposit, chipWithdraw, chipBillPayment;
    private MaterialButton btnDateFilter;
    private RecyclerView transactionRecyclerView;
    private View emptyStateLayout;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String accountId; // Optional - filter by account
    private List<Transaction> allTransactions;
    private List<Transaction> filteredTransactions;
    private TransactionAdapter adapter;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get intent data
        accountId = getIntent().getStringExtra("account_id");

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Listeners
        setupListeners();

        // Load transactions
        loadTransactions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipTransfer = findViewById(R.id.chipTransfer);
        chipDeposit = findViewById(R.id.chipDeposit);
        chipWithdraw = findViewById(R.id.chipWithdraw);
        chipBillPayment = findViewById(R.id.chipBillPayment);
        btnDateFilter = findViewById(R.id.btnDateFilter);
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        allTransactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(this, filteredTransactions);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int selectedId = checkedIds.get(0);
            if (selectedId == R.id.chipAll) {
                currentFilter = "all";
            } else if (selectedId == R.id.chipTransfer) {
                currentFilter = "transfer";
            } else if (selectedId == R.id.chipDeposit) {
                currentFilter = "deposit";
            } else if (selectedId == R.id.chipWithdraw) {
                currentFilter = "withdraw";
            } else if (selectedId == R.id.chipBillPayment) {
                currentFilter = "payment";
            }
            
            filterTransactions();
        });

        btnDateFilter.setOnClickListener(v -> showDateFilterDialog());
    }

    private void loadTransactions() {
        showLoading(true);

        // Query transactions
        Query query = db.collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Filter by account if specified
        if (accountId != null) {
            query = query.whereEqualTo("accountId", accountId);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    allTransactions.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        allTransactions.add(transaction);
                    }
                    
                    filterTransactions();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải giao dịch: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void filterTransactions() {
        filteredTransactions.clear();

        if (currentFilter.equals("all")) {
            filteredTransactions.addAll(allTransactions);
        } else {
            for (Transaction transaction : allTransactions) {
                if (transaction.getType().equals(currentFilter)) {
                    filteredTransactions.add(transaction);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            transactionRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            transactionRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showDateFilterDialog() {
        // TODO: Implement date range picker
        Toast.makeText(this, "Chức năng lọc theo ngày đang phát triển", 
            Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
