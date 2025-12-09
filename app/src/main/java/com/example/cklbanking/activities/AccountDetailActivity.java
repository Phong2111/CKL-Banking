package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.TransactionAdapter;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.models.Transaction;
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

public class AccountDetailActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextView accountTypeName, accountNumber, accountBalance;
    private TextView interestRateValue, amountDueValue;
    private View additionalInfoLayout, amountDueLayout;
    private MaterialCardView statisticsCard;
    private TextView savingTerm, openDate, maturityDate, expectedInterest;
    private MaterialButton btnTransferFromAccount, btnDepositToAccount, btnWithdrawFromAccount;
    private TextView btnViewAllTransactions;
    private RecyclerView transactionRecyclerView;
    private CircularProgressIndicator progressBar;
    private ImageView accountTypeIcon;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String accountId;
    private String accountType;
    private Account account;
    private List<Transaction> transactions;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get intent data
        accountId = getIntent().getStringExtra("account_id");
        accountType = getIntent().getStringExtra("account_type");

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Listeners
        setupListeners();

        // Load data
        loadAccountDetails();
        loadTransactions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        accountTypeName = findViewById(R.id.accountTypeName);
        accountNumber = findViewById(R.id.accountNumber);
        accountBalance = findViewById(R.id.accountBalance);
        accountTypeIcon = findViewById(R.id.accountTypeIcon);
        
        additionalInfoLayout = findViewById(R.id.additionalInfoLayout);
        interestRateValue = findViewById(R.id.interestRateValue);
        amountDueLayout = findViewById(R.id.amountDueLayout);
        amountDueValue = findViewById(R.id.amountDueValue);
        
        statisticsCard = findViewById(R.id.statisticsCard);
        savingTerm = findViewById(R.id.savingTerm);
        openDate = findViewById(R.id.openDate);
        maturityDate = findViewById(R.id.maturityDate);
        expectedInterest = findViewById(R.id.expectedInterest);
        
        btnTransferFromAccount = findViewById(R.id.btnTransferFromAccount);
        btnDepositToAccount = findViewById(R.id.btnDepositToAccount);
        btnWithdrawFromAccount = findViewById(R.id.btnWithdrawFromAccount);
        btnViewAllTransactions = findViewById(R.id.btnViewAllTransactions);
        
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        transactions = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactions);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnTransferFromAccount.setOnClickListener(v -> openTransferMoney());
        btnDepositToAccount.setOnClickListener(v -> openDeposit());
        btnWithdrawFromAccount.setOnClickListener(v -> openWithdraw());
        btnViewAllTransactions.setOnClickListener(v -> openTransactionHistory());
    }

    private void loadAccountDetails() {
        if (accountId == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        db.collection("accounts")
                .document(accountId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    
                    if (documentSnapshot.exists()) {
                        account = documentSnapshot.toObject(Account.class);
                        updateUI();
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin tài khoản", 
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
        if (account == null) return;

        // Update account info
        accountNumber.setText(account.getAccountNumber());
        accountBalance.setText(formatCurrency(account.getBalance()));

        // Update based on account type
        switch (account.getAccountType()) {
            case "checking":
                accountTypeName.setText("Tài khoản thanh toán");
                toolbar.setBackgroundColor(getColor(R.color.checking_account));
                additionalInfoLayout.setVisibility(View.GONE);
                statisticsCard.setVisibility(View.GONE);
                break;

            case "saving":
                accountTypeName.setText("Tài khoản tiết kiệm");
                toolbar.setBackgroundColor(getColor(R.color.saving_account));
                additionalInfoLayout.setVisibility(View.VISIBLE);
                statisticsCard.setVisibility(View.VISIBLE);
                amountDueLayout.setVisibility(View.GONE);
                
                // Show interest rate
                interestRateValue.setText(String.format("%.1f%%/năm", account.getInterestRate()));
                
                // Show statistics
                savingTerm.setText("12 tháng");
                openDate.setText("01/01/2024");
                maturityDate.setText("01/01/2025");
                double expectedInt = account.getBalance() * account.getInterestRate() / 100;
                expectedInterest.setText(formatCurrency(expectedInt));
                break;

            case "mortgage":
                accountTypeName.setText("Tài khoản vay thế chấp");
                toolbar.setBackgroundColor(getColor(R.color.mortgage_account));
                additionalInfoLayout.setVisibility(View.VISIBLE);
                statisticsCard.setVisibility(View.GONE);
                amountDueLayout.setVisibility(View.VISIBLE);
                
                // Show monthly payment (amountDue not in model)
                amountDueValue.setText(formatCurrency(account.getMonthlyPayment()));
                break;
        }
    }

    private void loadTransactions() {
        if (accountId == null) return;

        db.collection("transactions")
                .whereEqualTo("accountId", accountId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactions.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transactions.add(transaction);
                    }
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải giao dịch: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void openTransferMoney() {
        Intent intent = new Intent(this, TransferMoneyActivity.class);
        intent.putExtra("from_account_id", accountId);
        startActivity(intent);
    }

    private void openDeposit() {
        Toast.makeText(this, "Chức năng nạp tiền đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void openWithdraw() {
        Toast.makeText(this, "Chức năng rút tiền đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void openTransactionHistory() {
        Intent intent = new Intent(this, TransactionHistoryActivity.class);
        intent.putExtra("account_id", accountId);
        startActivity(intent);
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
