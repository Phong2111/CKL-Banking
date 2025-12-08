package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
        
        try {
            setContentView(R.layout.activity_customer_dashboard);

            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            
            // Check if user is logged in
            if (mAuth.getCurrentUser() == null) {
                // User not logged in, go back to login
                Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            
            userId = mAuth.getCurrentUser().getUid();

            // Initialize Views
            initViews();
            
            // Check if initViews failed
            if (toolbar == null) {
                Toast.makeText(this, "Lỗi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

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
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Go back to login on error
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void initViews() {
        try {
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
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo giao diện: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void setupToolbar() {
        if (toolbar == null) {
            return;
        }
        try {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, CustomerProfileActivity.class));
            return true;
        } else if (id == R.id.action_notifications) {
            startActivity(new Intent(this, NotificationsActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        if (recentTransactionsRecyclerView == null) {
            return;
        }
        try {
            recentTransactions = new ArrayList<>();
            adapter = new TransactionAdapter(this, recentTransactions);
            adapter.setOnTransactionClickListener(transaction -> {
                if (transaction != null && transaction.getTransactionId() != null) {
                    Intent intent = new Intent(CustomerDashboardActivity.this, TransactionDetailActivity.class);
                    intent.putExtra("transaction_id", transaction.getTransactionId());
                    startActivity(intent);
                }
            });
            recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            recentTransactionsRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        try {
            if (checkingAccountCard != null) {
                checkingAccountCard.setOnClickListener(v -> openAccountDetail("checking"));
            }
            if (savingAccountCard != null) {
                savingAccountCard.setOnClickListener(v -> openAccountDetail("saving"));
            }
            if (mortgageAccountCard != null) {
                mortgageAccountCard.setOnClickListener(v -> openAccountDetail("mortgage"));
            }
            
            if (btnTransfer != null) {
                btnTransfer.setOnClickListener(v -> openTransferMoney());
            }
            if (btnDeposit != null) {
                btnDeposit.setOnClickListener(v -> startActivity(new Intent(this, DepositActivity.class)));
            }
            if (btnWithdraw != null) {
                btnWithdraw.setOnClickListener(v -> startActivity(new Intent(this, WithdrawActivity.class)));
            }
            if (btnMore != null) {
                btnMore.setOnClickListener(v -> openUtilities());
            }
            
            if (btnViewAllTransactions != null) {
                btnViewAllTransactions.setOnClickListener(v -> openTransactionHistory());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserProfile() {
        if (userId == null) {
            return;
        }
        
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null && welcomeName != null) {
                            String name = currentUser.getFullName();
                            if (name != null && !name.isEmpty()) {
                                welcomeName.setText("Xin chào, " + name);
                            } else {
                                welcomeName.setText("Xin chào!");
                            }
                        }
                    } else {
                        if (welcomeName != null) {
                            welcomeName.setText("Xin chào!");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (welcomeName != null) {
                        welcomeName.setText("Xin chào!");
                    }
                    // Don't show error toast for profile loading failure
                });
    }

    private void loadAccounts() {
        if (userId == null) {
            return;
        }
        
        showLoading(true);

        db.collection("accounts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    userAccounts = new ArrayList<>();
                    double total = 0;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Account account = document.toObject(Account.class);
                            if (account != null) {
                                userAccounts.add(account);
                                
                                // Update individual account cards
                                String accountType = account.getAccountType();
                                if (accountType != null) {
                                    switch (accountType) {
                                        case "checking":
                                            if (checkingAccountBalance != null) {
                                                checkingAccountBalance.setText(formatCurrency(account.getBalance()));
                                            }
                                            if (checkingAccountCard != null) {
                                                checkingAccountCard.setVisibility(View.VISIBLE);
                                            }
                                            total += account.getBalance();
                                            break;
                                        case "saving":
                                            if (savingAccountBalance != null) {
                                                savingAccountBalance.setText(formatCurrency(account.getBalance()));
                                            }
                                            if (savingAccountCard != null) {
                                                savingAccountCard.setVisibility(View.VISIBLE);
                                            }
                                            total += account.getBalance();
                                            break;
                                        case "mortgage":
                                            if (mortgageAmountDue != null) {
                                                mortgageAmountDue.setText(formatCurrency(Math.abs(account.getBalance())));
                                            }
                                            if (mortgageAccountCard != null) {
                                                mortgageAccountCard.setVisibility(View.VISIBLE);
                                            }
                                            // Don't add negative balance to total
                                            break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    if (totalBalance != null) {
                        totalBalance.setText(formatCurrency(total));
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    // Don't show error toast, just log it
                    e.printStackTrace();
                });
    }

    private void loadRecentTransactions() {
        if (userId == null || adapter == null) {
            return;
        }
        
        // Get all recent transactions (will filter by user's accounts if needed)
        // For now, just get recent transactions - can be improved later
        db.collection("transactions")
                .whereEqualTo("userId", userId) // QUAN TRỌNG: Chỉ lấy của user này
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (recentTransactions != null) {
                        recentTransactions.clear();
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Transaction transaction = document.toObject(Transaction.class);
                                if (transaction != null) {
                                    recentTransactions.add(transaction);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // If orderBy fails (no index), try without orderBy
                    db.collection("transactions")
                            .limit(5)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (recentTransactions != null) {
                                    recentTransactions.clear();
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        try {
                                            Transaction transaction = document.toObject(Transaction.class);
                                            if (transaction != null) {
                                                recentTransactions.add(transaction);
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    if (adapter != null) {
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                });
    }

    private void openAccountDetail(String accountType) {
        // Find account by type
        if (userAccounts != null && accountType != null) {
            for (Account account : userAccounts) {
                if (account != null && accountType.equals(account.getAccountType())) {
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
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to dashboard
        loadAccounts();
        loadRecentTransactions();
    }
}
