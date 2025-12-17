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
import com.example.cklbanking.repositories.AccountRepository;
import com.example.cklbanking.repositories.TransactionRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountDetailActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextView accountTypeName, accountNumber, accountBalance;
    private TextView balanceLabel; // Label for balance (changes for mortgage)
    private TextView interestRateValue, interestRateLabel, biweeklyPaymentLabel, amountDueValue;
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
    
    // Repositories
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;

    // Data
    private String accountId;
    private String accountType;
    private Account account;
    private List<Transaction> transactions;
    private TransactionAdapter adapter;
    
    // Date formatter
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize Repositories
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        
        // Initialize Date Formatter
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
        balanceLabel = findViewById(R.id.balanceLabel);
        accountTypeIcon = findViewById(R.id.accountTypeIcon);
        
        additionalInfoLayout = findViewById(R.id.additionalInfoLayout);
        interestRateValue = findViewById(R.id.interestRateValue);
        interestRateLabel = findViewById(R.id.interestRateLabel);
        biweeklyPaymentLabel = findViewById(R.id.biweeklyPaymentLabel);
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

        accountRepository.getAccountById(accountId)
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    
                    if (documentSnapshot.exists()) {
                        account = documentSnapshot.toObject(Account.class);
                        if (account != null) {
                            account.setAccountId(documentSnapshot.getId());
                            updateUI();
                        }
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
        
        // Update based on account type
        switch (account.getAccountType()) {
            case "checking":
                accountTypeName.setText("Tài khoản vãng lai");
                toolbar.setBackgroundColor(getColor(R.color.checking_account));
                additionalInfoLayout.setVisibility(View.GONE);
                statisticsCard.setVisibility(View.GONE);
                // Show balance for checking account
                if (balanceLabel != null) {
                    balanceLabel.setText("Số dư");
                }
                accountBalance.setText(formatCurrency(account.getBalance()));
                break;

            case "saving":
                accountTypeName.setText("Tài khoản tiết kiệm");
                toolbar.setBackgroundColor(getColor(R.color.saving_account));
                additionalInfoLayout.setVisibility(View.VISIBLE);
                statisticsCard.setVisibility(View.VISIBLE);
                amountDueLayout.setVisibility(View.GONE);
                
                // Show balance for saving account
                if (balanceLabel != null) {
                    balanceLabel.setText("Số dư");
                }
                accountBalance.setText(formatCurrency(account.getBalance()));
                
                // Show interest rate
                if (interestRateLabel != null) {
                    interestRateLabel.setText("Lãi suất");
                    interestRateLabel.setVisibility(View.VISIBLE);
                }
                if (biweeklyPaymentLabel != null) {
                    biweeklyPaymentLabel.setVisibility(View.GONE);
                }
                if (account.getInterestRate() > 0) {
                    interestRateValue.setText(String.format(Locale.getDefault(), "%.2f%%/năm", account.getInterestRate()));
                } else {
                    interestRateValue.setText("Chưa có");
                }
                
                // Show statistics - sử dụng dữ liệu thực từ model
                if (account.getTerm() > 0) {
                    savingTerm.setText(account.getTerm() + " tháng");
                } else {
                    savingTerm.setText("Chưa xác định");
                }
                
                if (account.getOpenDate() != null) {
                    openDate.setText(dateFormatter.format(account.getOpenDate()));
                } else {
                    openDate.setText("Chưa có");
                }
                
                if (account.getMaturityDate() != null) {
                    maturityDate.setText(dateFormatter.format(account.getMaturityDate()));
                } else {
                    maturityDate.setText("Chưa có");
                }
                
                // Tính lợi nhuận hàng tháng = (Balance × Interest Rate) / 12 / 100
                double monthlyProfit = calculateMonthlyProfit(account);
                double totalProfit = calculateTotalProfit(account);
                
                // Hiển thị lợi nhuận/tháng và tổng lợi nhuận tích lũy
                String profitText = formatCurrency(monthlyProfit) + "/tháng";
                if (totalProfit > 0) {
                    profitText += "\n(Tổng tích lũy: " + formatCurrency(totalProfit) + ")";
                }
                expectedInterest.setText(profitText);
                break;

            case "mortgage":
                accountTypeName.setText("Tài khoản vay thế chấp");
                toolbar.setBackgroundColor(getColor(R.color.mortgage_account));
                additionalInfoLayout.setVisibility(View.VISIBLE);
                statisticsCard.setVisibility(View.GONE);
                amountDueLayout.setVisibility(View.VISIBLE);
                
                // Hiển thị số dư nợ còn lại (remainingBalance) thay vì balance
                if (balanceLabel != null) {
                    balanceLabel.setText("Số dư nợ còn lại");
                }
                if (account.getRemainingBalance() > 0) {
                    accountBalance.setText(formatCurrency(account.getRemainingBalance()));
                } else {
                    // Fallback to balance if remainingBalance is not set
                    accountBalance.setText(formatCurrency(account.getBalance()));
                }
                
                // Show monthly payment
                if (account.getMonthlyPayment() > 0) {
                    amountDueValue.setText(formatCurrency(account.getMonthlyPayment()) + "/tháng");
                } else {
                    amountDueValue.setText("Chưa có");
                }
                
                // Ẩn interest rate label, hiển thị biweekly payment label
                if (interestRateLabel != null) {
                    interestRateLabel.setVisibility(View.GONE);
                }
                if (biweeklyPaymentLabel != null) {
                    biweeklyPaymentLabel.setVisibility(View.VISIBLE);
                }
                
                // Hiển thị bi-weekly payment nếu có
                if (account.getBiweeklyPayment() > 0) {
                    interestRateValue.setText(formatCurrency(account.getBiweeklyPayment()) + "/2 tuần");
                } else {
                    interestRateValue.setText("Chưa có");
                }
                break;
        }
    }

    private void loadTransactions() {
        if (accountId == null) return;

        // Sử dụng TransactionRepository - query theo fromAccountId
        transactionRepository.getTransactionsByAccount(accountId)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactions.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transaction.setTransactionId(document.getId());
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
        Intent intent = new Intent(this, DepositActivity.class);
        intent.putExtra("account_id", accountId);
        startActivity(intent);
    }

    private void openWithdraw() {
        Intent intent = new Intent(this, WithdrawActivity.class);
        intent.putExtra("account_id", accountId);
        startActivity(intent);
    }

    private void openTransactionHistory() {
        Intent intent = new Intent(this, TransactionHistoryActivity.class);
        intent.putExtra("account_id", accountId);
        startActivity(intent);
    }

    /**
     * Calculate monthly profit for saving account
     * Formula: balance * interestRate / 12 / 100
     * @param account Account object
     * @return Monthly profit amount
     */
    private double calculateMonthlyProfit(Account account) {
        if (account == null || !"saving".equals(account.getAccountType())) {
            return 0;
        }
        
        if (account.getInterestRate() <= 0 || account.getBalance() <= 0) {
            return 0;
        }
        
        // Formula: balance * interestRate / 12 / 100
        return (account.getBalance() * account.getInterestRate()) / 12.0 / 100.0;
    }
    
    /**
     * Calculate total accumulated profit for saving account
     * Formula: balance * interestRate * months / 12 / 100
     * @param account Account object
     * @return Total accumulated profit
     */
    private double calculateTotalProfit(Account account) {
        if (account == null || !"saving".equals(account.getAccountType())) {
            return 0;
        }
        
        if (account.getInterestRate() <= 0 || account.getBalance() <= 0) {
            return 0;
        }
        
        if (account.getOpenDate() == null) {
            return 0;
        }
        
        // Calculate months from openDate to now
        long openTime = account.getOpenDate().getTime();
        long currentTime = System.currentTimeMillis();
        long diffTime = currentTime - openTime;
        double months = diffTime / (1000.0 * 60 * 60 * 24 * 30.44); // Average days per month
        
        // Formula: balance * interestRate * months / 12 / 100
        return (account.getBalance() * account.getInterestRate() * months) / 12.0 / 100.0;
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
