package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.models.Transaction;
import com.example.cklbanking.repositories.AccountRepository;
import com.example.cklbanking.repositories.TransactionRepository;
import com.example.cklbanking.services.TransactionService;
import com.example.cklbanking.utils.ErrorHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WithdrawActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerAccount;
    private TextInputEditText editAmount;
    private TextView textAvailableBalance;
    private RadioGroup radioGroupMethod;
    private MaterialButton btnWithdraw;
    private MaterialButton btn500k, btn1m, btn2m, btn3m, btn5m, btn10m;
    private CircularProgressIndicator progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private TransactionService transactionService;
    private List<Account> accounts;
    private String selectedAccountId;
    private Account selectedAccount;
    private double availableBalance = 0;
    private String userId;
    private String ekycStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        transactionService = new TransactionService();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        initViews();
        setupToolbar();
        setupListeners();
        
        // Load user info (eKYC status)
        loadUserInfo();
        
        // Kiểm tra xem có account_id từ intent không
        String accountIdFromIntent = getIntent().getStringExtra("account_id");
        if (accountIdFromIntent != null) {
            loadSpecificAccount(accountIdFromIntent);
        } else {
            loadAccounts();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerAccount = findViewById(R.id.spinnerAccount);
        editAmount = findViewById(R.id.editAmount);
        textAvailableBalance = findViewById(R.id.textAvailableBalance);
        radioGroupMethod = findViewById(R.id.radioGroupMethod);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        
        btn500k = findViewById(R.id.btn500k);
        btn1m = findViewById(R.id.btn1m);
        btn2m = findViewById(R.id.btn2m);
        btn3m = findViewById(R.id.btn3m);
        btn5m = findViewById(R.id.btn5m);
        btn10m = findViewById(R.id.btn10m);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void loadSpecificAccount(String accountId) {
        accountRepository.getAccountById(accountId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedAccount = documentSnapshot.toObject(Account.class);
                        if (selectedAccount != null) {
                            selectedAccount.setAccountId(documentSnapshot.getId());
                            selectedAccountId = accountId;
                            availableBalance = selectedAccount.getBalance();
                            updateAvailableBalanceDisplay();
                            spinnerAccount.setVisibility(View.GONE);
                        }
                    } else {
                        loadAccounts();
                    }
                })
                .addOnFailureListener(e -> {
                    loadAccounts();
                });
    }
    
    private void updateAvailableBalanceDisplay() {
        textAvailableBalance.setText("Số dư khả dụng: " + formatCurrency(availableBalance));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btn500k.setOnClickListener(v -> editAmount.setText("500000"));
        btn1m.setOnClickListener(v -> editAmount.setText("1000000"));
        btn2m.setOnClickListener(v -> editAmount.setText("2000000"));
        btn3m.setOnClickListener(v -> editAmount.setText("3000000"));
        btn5m.setOnClickListener(v -> editAmount.setText("5000000"));
        btn10m.setOnClickListener(v -> editAmount.setText("10000000"));
        
        btnWithdraw.setOnClickListener(v -> processWithdraw());
    }

    private void loadAccounts() {
        String userId = mAuth.getCurrentUser().getUid();
        
        accountRepository.getAccountsForUser(userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accounts = new ArrayList<>();
                    List<String> accountNames = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
                        account.setAccountId(document.getId());
                        accounts.add(account);
                        accountNames.add(account.getAccountNumber() + " - " + account.getAccountType());
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, accountNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAccount.setAdapter(adapter);
                    
                    if (!accounts.isEmpty()) {
                        updateAvailableBalance(0);
                    }
                })
                .addOnFailureListener(e -> {
                    ErrorHandler.handleError(this, e, "Lỗi tải danh sách tài khoản");
                });
        
        spinnerAccount.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                updateAvailableBalance(position);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateAvailableBalance(int position) {
        if (accounts != null && position < accounts.size()) {
            selectedAccount = accounts.get(position);
            selectedAccountId = selectedAccount.getAccountId();
            availableBalance = selectedAccount.getBalance();
            updateAvailableBalanceDisplay();
        }
    }

    private void processWithdraw() {
        String amountStr = editAmount.getText().toString().trim();
        
        if (TextUtils.isEmpty(amountStr)) {
            editAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            editAmount.setError("Số tiền không hợp lệ");
            return;
        }
        
        if (amount < 100000) {
            editAmount.setError("Số tiền rút tối thiểu là 100.000 VNĐ");
            return;
        }
        
        if (amount > availableBalance) {
            editAmount.setError("Số dư không đủ");
            return;
        }
        
        // Lấy account được chọn
        if (selectedAccountId == null) {
            int selectedPos = spinnerAccount.getSelectedItemPosition();
            if (selectedPos < 0 || accounts == null || accounts.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedAccount = accounts.get(selectedPos);
            selectedAccountId = selectedAccount.getAccountId();
        }
        
        if (selectedAccount == null) {
            // Load account info
            accountRepository.getAccountById(selectedAccountId)
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            selectedAccount = documentSnapshot.toObject(Account.class);
                            if (selectedAccount != null) {
                                selectedAccount.setAccountId(documentSnapshot.getId());
                                checkEkycAndWithdraw(amount);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        ErrorHandler.handleError(this, e, "Lỗi tải thông tin tài khoản");
                    });
            return;
        }
        
        checkEkycAndWithdraw(amount);
    }
    
    private void loadUserInfo() {
        if (userId == null) return;
        
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ekycStatus = documentSnapshot.getString("ekycStatus");
                    }
                });
    }
    
    private void checkEkycAndWithdraw(double amount) {
        // Check eKYC for high-value withdrawals
        transactionService.checkEkycRequirement(userId, amount, 
            (ekycRequired, status, message) -> {
                if (ekycRequired) {
                    // eKYC is required - show message and navigate to eKYC
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, EKYCActivity.class);
                    intent.putExtra("pending_withdraw_amount", amount);
                    intent.putExtra("pending_withdraw_account_id", selectedAccountId);
                    startActivity(intent);
                } else {
                    // eKYC not required or already verified - proceed with withdraw
                    performWithdraw(amount);
                }
            });
    }
    
    private void performWithdraw(double amount) {
        // Tính số dư mới
        double newBalance = selectedAccount.getBalance() - amount;
        
        if (newBalance < 0) {
            editAmount.setError("Số dư không đủ");
            return;
        }
        
        // Tạo transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(selectedAccountId);
        transaction.setToAccountId(selectedAccountId); // Withdraw từ chính account đó
        transaction.setAmount(amount);
        transaction.setType("withdraw");
        transaction.setStatus("completed");
        
        btnWithdraw.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        // Sử dụng batch write để đảm bảo atomicity
        WriteBatch batch = db.batch();
        
        // Update balance
        batch.update(db.collection("accounts").document(selectedAccountId), "balance", newBalance);
        
        // Create transaction
        String transactionId = db.collection("transactions").document().getId();
        transaction.setTransactionId(transactionId);
        batch.set(db.collection("transactions").document(transactionId), transaction);
        
        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Rút tiền thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnWithdraw.setEnabled(true);
                    ErrorHandler.handleErrorWithRetry(this, e, "Lỗi rút tiền", 
                        () -> performWithdraw(amount));
                });
    }

    private String getSelectedMethod() {
        int selectedId = radioGroupMethod.getCheckedRadioButtonId();
        if (selectedId == R.id.radioATM) {
            return "atm";
        } else {
            return "branch";
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
