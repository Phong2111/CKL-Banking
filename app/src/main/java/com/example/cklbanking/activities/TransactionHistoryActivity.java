package com.example.cklbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.TransactionAdapter;
import com.example.cklbanking.models.Transaction;
import com.example.cklbanking.repositories.TransactionRepository;
import com.example.cklbanking.utils.ErrorHandler;
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
    private Chip chipAll, chipTransfer, chipDeposit, chipWithdraw, chipUtilities;
    private MaterialButton btnDateFilter;
    private RecyclerView transactionRecyclerView;
    private View emptyStateLayout;
    private CircularProgressIndicator progressBar;
    private View loadingMoreLayout;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TransactionRepository transactionRepository;

    // Data
    private String accountId; // Optional - filter by account
    private List<Transaction> allTransactions;
    private List<Transaction> filteredTransactions;
    private TransactionAdapter adapter;
    private String currentFilter = "all";
    
    // Pagination
    private static final int PAGE_SIZE = 20;
    private com.google.firebase.firestore.QueryDocumentSnapshot lastDocument;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        transactionRepository = new TransactionRepository();

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
        chipUtilities = findViewById(R.id.chipUtilities);
        btnDateFilter = findViewById(R.id.btnDateFilter);
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        loadingMoreLayout = findViewById(R.id.loadingMoreLayout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        allTransactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(this, filteredTransactions);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        transactionRecyclerView.setLayoutManager(layoutManager);
        transactionRecyclerView.setAdapter(adapter);
        
        // Add scroll listener for pagination
        transactionRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // Load more when user scrolls near the end
                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreTransactions();
                    }
                }
            }
        });
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
            } else if (selectedId == R.id.chipUtilities) {
                currentFilter = "utilities";
            }
            
            filterTransactions();
        });

        btnDateFilter.setOnClickListener(v -> showDateFilterDialog());
    }

    private void loadTransactions() {
        if (isLoading) return;
        
        showLoading(true);
        isLoading = true;
        allTransactions.clear();
        lastDocument = null;
        hasMoreData = true;

        // Query transactions using TransactionRepository
        Query query;
        if (accountId != null) {
            // Load transactions for specific account
            query = transactionRepository.getTransactionsByAccount(accountId)
                    .limit(PAGE_SIZE);
        } else {
            // Load all transactions for current user
            String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (userId != null) {
                query = db.collection("transactions")
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(PAGE_SIZE);
            } else {
                query = db.collection("transactions")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(PAGE_SIZE);
            }
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    isLoading = false;
                    
                    String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transaction.setTransactionId(document.getId());
                        allTransactions.add(transaction);
                    }
                    
                    // Update last document for pagination
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    filterTransactions();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    isLoading = false;
                    ErrorHandler.handleError(this, e, "Lỗi tải giao dịch");
                    updateEmptyState();
                });
    }
    
    private void loadMoreTransactions() {
        if (isLoading || !hasMoreData || lastDocument == null) return;
        
        isLoading = true;
        loadingMoreLayout.setVisibility(View.VISIBLE);

        Query query;
        if (accountId != null) {
            query = transactionRepository.getTransactionsByAccount(accountId)
                    .startAfter(lastDocument)
                    .limit(PAGE_SIZE);
        } else {
            String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (userId != null) {
                query = db.collection("transactions")
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .startAfter(lastDocument)
                        .limit(PAGE_SIZE);
            } else {
                query = db.collection("transactions")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .startAfter(lastDocument)
                        .limit(PAGE_SIZE);
            }
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    loadingMoreLayout.setVisibility(View.GONE);
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transaction.setTransactionId(document.getId());
                        allTransactions.add(transaction);
                    }
                    
                    // Update last document for pagination
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    filterTransactions();
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    loadingMoreLayout.setVisibility(View.GONE);
                    ErrorHandler.handleError(this, e, "Lỗi tải thêm giao dịch");
                });
    }

    private void filterTransactions() {
        filteredTransactions.clear();

        if (currentFilter.equals("all")) {
            filteredTransactions.addAll(allTransactions);
        } else if (currentFilter.equals("utilities")) {
            // Filter all utility payment types
            for (Transaction transaction : allTransactions) {
                String type = transaction.getType();
                if (type != null && (type.equals("bill_payment") || 
                    type.equals("phone_recharge") || 
                    type.equals("flight_ticket") || 
                    type.equals("movie_ticket") || 
                    type.equals("hotel_booking") || 
                    type.equals("ecommerce_payment"))) {
                    filteredTransactions.add(transaction);
                }
            }
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
