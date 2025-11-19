package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferMoneyActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private ChipGroup transferTypeChipGroup;
    private Chip chipInternalTransfer, chipExternalTransfer;
    private LinearLayout fromAccountLayout;
    private TextView fromAccountName, fromAccountNumber, fromAccountBalance;
    private TextInputLayout layoutBankName, layoutRecipientAccount, layoutAmount, layoutMessage;
    private AutoCompleteTextView bankNameDropdown;
    private TextInputEditText editRecipientAccount, editAmount, editMessage;
    private TextView recipientName;
    private MaterialButton btnAmount100k, btnAmount500k, btnAmount1M;
    private MaterialButton btnTransfer;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private List<Account> userAccounts;
    private Account selectedFromAccount;
    private String transferType = "internal";
    private String[] vietnameseBanks = {
        "Vietcombank", "Techcombank", "BIDV", "VietinBank", "ACB",
        "MB Bank", "Sacombank", "VPBank", "Agribank", "TPBank"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_money);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Listeners
        setupListeners();

        // Load user accounts
        loadUserAccounts();

        // Setup bank dropdown
        setupBankDropdown();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        transferTypeChipGroup = findViewById(R.id.transferTypeChipGroup);
        chipInternalTransfer = findViewById(R.id.chipInternalTransfer);
        chipExternalTransfer = findViewById(R.id.chipExternalTransfer);
        fromAccountLayout = findViewById(R.id.fromAccountLayout);
        fromAccountName = findViewById(R.id.fromAccountName);
        fromAccountNumber = findViewById(R.id.fromAccountNumber);
        fromAccountBalance = findViewById(R.id.fromAccountBalance);
        layoutBankName = findViewById(R.id.layoutBankName);
        bankNameDropdown = findViewById(R.id.bankNameDropdown);
        layoutRecipientAccount = findViewById(R.id.layoutRecipientAccount);
        editRecipientAccount = findViewById(R.id.editRecipientAccount);
        recipientName = findViewById(R.id.recipientName);
        layoutAmount = findViewById(R.id.layoutAmount);
        editAmount = findViewById(R.id.editAmount);
        layoutMessage = findViewById(R.id.layoutMessage);
        editMessage = findViewById(R.id.editMessage);
        btnAmount100k = findViewById(R.id.btnAmount100k);
        btnAmount500k = findViewById(R.id.btnAmount500k);
        btnAmount1M = findViewById(R.id.btnAmount1M);
        btnTransfer = findViewById(R.id.btnTransfer);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        transferTypeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            if (checkedIds.get(0) == R.id.chipInternalTransfer) {
                transferType = "internal";
                layoutBankName.setVisibility(View.GONE);
            } else {
                transferType = "external";
                layoutBankName.setVisibility(View.VISIBLE);
            }
        });

        fromAccountLayout.setOnClickListener(v -> selectFromAccount());

        btnAmount100k.setOnClickListener(v -> setAmount(100000));
        btnAmount500k.setOnClickListener(v -> setAmount(500000));
        btnAmount1M.setOnClickListener(v -> setAmount(1000000));

        btnTransfer.setOnClickListener(v -> processTransfer());
    }

    private void selectFromAccount() {
        if (userAccounts == null || userAccounts.isEmpty()) {
            Toast.makeText(this, "Chưa có tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }
        // Use first account as default
        if (selectedFromAccount == null && !userAccounts.isEmpty()) {
            selectedFromAccount = userAccounts.get(0);
            updateFromAccountUI();
        }
    }

    private void updateFromAccountUI() {
        if (selectedFromAccount == null) return;
        
        fromAccountNumber.setText(selectedFromAccount.getAccountNumber());
        fromAccountBalance.setText("Số dư: " + formatCurrency(selectedFromAccount.getBalance()));
        
        String typeName = "";
        switch (selectedFromAccount.getAccountType()) {
            case "checking": typeName = "Tài khoản thanh toán"; break;
            case "saving": typeName = "Tài khoản tiết kiệm"; break;
            case "mortgage": typeName = "Tài khoản vay"; break;
        }
        fromAccountName.setText(typeName);
    }

    private void loadUserAccounts() {
        showLoading(true);

        db.collection("accounts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    userAccounts = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
                        userAccounts.add(account);
                    }
                    
                    // Set first account as default
                    if (!userAccounts.isEmpty()) {
                        selectedFromAccount = userAccounts.get(0);
                        updateFromAccountUI();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải tài khoản: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBankDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_dropdown_item_1line, 
            vietnameseBanks
        );
        bankNameDropdown.setAdapter(adapter);
    }

    private void setAmount(double amount) {
        editAmount.setText(String.valueOf((int) amount));
    }

    private void processTransfer() {
        // Validate input
        if (selectedFromAccount == null) {
            Toast.makeText(this, "Vui lòng chọn tài khoản nguồn", Toast.LENGTH_SHORT).show();
            return;
        }

        String toAccount = editRecipientAccount.getText().toString().trim();
        if (toAccount.isEmpty()) {
            editRecipientAccount.setError("Vui lòng nhập số tài khoản người nhận");
            editRecipientAccount.requestFocus();
            return;
        }

        String recipient = recipientName.getText().toString().trim();
        if (recipient.isEmpty()) {
            recipient = "Không xác định";
        }

        String amountStr = editAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            editAmount.setError("Vui lòng nhập số tiền");
            editAmount.requestFocus();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            editAmount.setError("Số tiền phải lớn hơn 0");
            editAmount.requestFocus();
            return;
        }

        if (amount > selectedFromAccount.getBalance()) {
            editAmount.setError("Số dư không đủ");
            editAmount.requestFocus();
            return;
        }

        if (transferType.equals("external")) {
            String bank = bankNameDropdown.getText().toString().trim();
            if (bank.isEmpty()) {
                bankNameDropdown.setError("Vui lòng chọn ngân hàng");
                bankNameDropdown.requestFocus();
                return;
            }
        }

        // Create transaction
        createTransaction(toAccount, recipient, amount);
    }

    private void createTransaction(String toAccount, String recipientName, double amount) {
        showLoading(true);

        // Create transaction document (using original Transaction model fields)
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("fromAccountId", selectedFromAccount.getAccountNumber());
        transaction.put("toAccountId", toAccount);
        transaction.put("type", "transfer");
        transaction.put("amount", amount);
        transaction.put("status", "pending");
        transaction.put("timestamp", com.google.firebase.Timestamp.now());
        
        // Store additional info in separate fields (not in model but in Firestore)
        transaction.put("recipientName", recipientName);
        transaction.put("transferType", transferType);
        transaction.put("description", editMessage.getText().toString().trim());
        transaction.put("requiresOTP", true);
        
        if (transferType.equals("external")) {
            transaction.put("recipientBank", bankNameDropdown.getText().toString().trim());
        }

        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    String transactionId = documentReference.getId();
                    
                    // Navigate to OTP verification
                    Intent intent = new Intent(TransferMoneyActivity.this, 
                        com.example.cklbanking.activities.OTPVerificationActivity.class);
                    intent.putExtra("transaction_id", transactionId);
                    intent.putExtra("amount", amount);
                    intent.putExtra("recipient", recipientName);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnTransfer.setEnabled(!show);
    }
}
