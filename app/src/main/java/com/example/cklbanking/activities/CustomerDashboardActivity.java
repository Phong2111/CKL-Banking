package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.TransactionAdapter;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.models.Transaction;
import com.example.cklbanking.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextView welcomeName, totalBalance;
    private MaterialCardView checkingAccountCard, savingAccountCard, mortgageAccountCard;
    private TextView checkingAccountBalance, savingAccountBalance, mortgageAmountDue;
    private LinearLayout btnTransfer, btnDeposit, btnWithdraw, btnMore;
    private TextView btnViewAllTransactions;
    private RecyclerView recentTransactionsRecyclerView;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;
    private List<Account> userAccounts;
    private List<Transaction> recentTransactions;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Listeners
        setupListeners();

        // Load data
        loadUserProfile();
        loadAccounts();
        loadRecentTransactions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        welcomeName = findViewById(R.id.welcomeName);
        totalBalance = findViewById(R.id.totalBalance);
        
        checkingAccountCard = findViewById(R.id.checkingAccountCard);
        savingAccountCard = findViewById(R.id.savingAccountCard);
        mortgageAccountCard = findViewById(R.id.mortgageAccountCard);
        
        checkingAccountBalance = findViewById(R.id.checkingAccountBalance);
        savingAccountBalance = findViewById(R.id.savingAccountBalance);
        mortgageAmountDue = findViewById(R.id.mortgageAmountDue);
        
        btnTransfer = findViewById(R.id.btnTransfer);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnMore = findViewById(R.id.btnMore);
        
        btnViewAllTransactions = findViewById(R.id.btnViewAllTransactions);
        recentTransactionsRecyclerView = findViewById(R.id.recentTransactionsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.menu_dashboard);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        recentTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(this, recentTransactions);
        adapter.setOnTransactionClickListener(transaction -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, TransactionDetailActivity.class);
            intent.putExtra("transaction_id", transaction.getTransactionId());
            startActivity(intent);
        });
        recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTransactionsRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        checkingAccountCard.setOnClickListener(v -> openAccountDetail("checking"));
        savingAccountCard.setOnClickListener(v -> openAccountDetail("saving"));
        mortgageAccountCard.setOnClickListener(v -> openAccountDetail("mortgage"));
        
        btnTransfer.setOnClickListener(v -> openTransferMoney());
        btnDeposit.setOnClickListener(v -> startActivity(new Intent(this, DepositActivity.class)));
        btnWithdraw.setOnClickListener(v -> startActivity(new Intent(this, WithdrawActivity.class)));
        btnMore.setOnClickListener(v -> openUtilities());
        
        btnViewAllTransactions.setOnClickListener(v -> openTransactionHistory());
    }

    private void loadUserProfile() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            welcomeName.setText("Xin chào, " + currentUser.getFullName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAccounts() {
        showLoading(true);

        db.collection("accounts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    userAccounts = new ArrayList<>();
                    double total = 0;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
                        userAccounts.add(account);
                        
                        // Update individual account cards
                        switch (account.getAccountType()) {
                            case "checking":
                                checkingAccountBalance.setText(formatCurrency(account.getBalance()));
                                checkingAccountCard.setVisibility(View.VISIBLE);
                                total += account.getBalance();
                                break;
                            case "saving":
                                savingAccountBalance.setText(formatCurrency(account.getBalance()));
                                savingAccountCard.setVisibility(View.VISIBLE);
                                total += account.getBalance();
                                break;
                            case "mortgage":
                                mortgageAmountDue.setText(formatCurrency(Math.abs(account.getBalance())));
                                mortgageAccountCard.setVisibility(View.VISIBLE);
                                // Don't add negative balance to total
                                break;
                        }
                    }
                    
                    totalBalance.setText(formatCurrency(total));
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải tài khoản: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRecentTransactions() {
        // Get all transactions
        db.collection("transactions")
                .whereEqualTo("userId", userId) // QUAN TRỌNG: Chỉ lấy của user này
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recentTransactions.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        recentTransactions.add(transaction);
                    }
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải giao dịch: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void openAccountDetail(String accountType) {
        // Find account by type
        if (userAccounts != null) {
            for (Account account : userAccounts) {
                if (account.getAccountType().equals(accountType)) {
                    Intent intent = new Intent(this, AccountDetailActivity.class);
                    intent.putExtra("account_id", account.getAccountNumber());
                    intent.putExtra("account_type", accountType);
                    startActivity(intent);
                    return;
                }
            }
        }
        Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
    }

    private void openTransferMoney() {
        Intent intent = new Intent(this, TransferMoneyActivity.class);
        startActivity(intent);
    }

    private void openUtilities() {
        Intent intent = new Intent(this, UtilitiesActivity.class);
        startActivity(intent);
    }

    private void openTransactionHistory() {
        Intent intent = new Intent(this, TransactionHistoryActivity.class);
        startActivity(intent);
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to dashboard
        loadAccounts();
        loadRecentTransactions();
    }
}
