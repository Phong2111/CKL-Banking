package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.models.Transaction;
import com.example.cklbanking.models.UtilityPayment;
import com.example.cklbanking.repositories.AccountRepository;
import com.example.cklbanking.repositories.TransactionRepository;
import com.example.cklbanking.repositories.UtilityRepository;
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

public class EcommercePaymentActivity extends AppCompatActivity {

    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private Spinner spinnerAccount;
    private TextInputEditText editOrderId;
    private TextInputEditText editMerchantName;
    private TextInputEditText editAmount;
    private TextView textAccountBalance;
    private MaterialButton btnPay;
    private CircularProgressIndicator progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private UtilityRepository utilityRepository;

    private List<Account> accounts;
    private Account selectedAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecommerce_payment);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        utilityRepository = new UtilityRepository();

        initViews();
        setupToolbar();
        setupListeners();
        loadAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerAccount = findViewById(R.id.spinnerAccount);
        editOrderId = findViewById(R.id.editOrderId);
        editMerchantName = findViewById(R.id.editMerchantName);
        editAmount = findViewById(R.id.editAmount);
        textAccountBalance = findViewById(R.id.textAccountBalance);
        btnPay = findViewById(R.id.btnPay);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (accounts != null && position >= 0 && position < accounts.size()) {
                    selectedAccount = accounts.get(position);
                    updateAccountBalanceDisplay();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnPay.setOnClickListener(v -> processPayment());
    }

    private void loadAccounts() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        accountRepository.getAccountsForUser(userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accounts = new ArrayList<>();
                    List<String> accountNames = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
                        account.setAccountId(document.getId());
                        accounts.add(account);
                        accountNames.add(account.getAccountNumber() + " - " + getAccountTypeName(account.getAccountType()));
                    }

                    if (accounts.isEmpty()) {
                        Toast.makeText(this, "Bạn chưa có tài khoản nào", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            accountNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAccount.setAdapter(adapter);

                    // Set first account as default
                    if (!accounts.isEmpty()) {
                        selectedAccount = accounts.get(0);
                        updateAccountBalanceDisplay();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh sách tài khoản: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAccountBalanceDisplay() {
        if (selectedAccount != null) {
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            textAccountBalance.setText("Số dư: " + formatter.format(selectedAccount.getBalance()) + " VNĐ");
        }
    }

    private String getAccountTypeName(String accountType) {
        switch (accountType) {
            case "checking":
                return "Tài khoản thanh toán";
            case "saving":
                return "Tài khoản tiết kiệm";
            case "mortgage":
                return "Tài khoản vay";
            default:
                return accountType;
        }
    }

    private void processPayment() {
        // Validate input
        String orderId = editOrderId.getText().toString().trim();
        String merchantName = editMerchantName.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();

        if (TextUtils.isEmpty(orderId)) {
            editOrderId.setError("Vui lòng nhập mã đơn hàng");
            return;
        }

        if (TextUtils.isEmpty(merchantName)) {
            editMerchantName.setError("Vui lòng nhập tên cửa hàng/sàn TMĐT");
            return;
        }

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

        if (amount <= 0) {
            editAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount < 10000) {
            editAmount.setError("Số tiền tối thiểu là 10.000 VNĐ");
            return;
        }

        if (selectedAccount == null) {
            Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check balance
        if (selectedAccount.getBalance() < amount) {
            Toast.makeText(this, "Số dư tài khoản không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        performPayment(orderId, merchantName, amount);
    }

    private void performPayment(String orderId, String merchantName, double amount) {
        btnPay.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();

        // Calculate new balance
        double newBalance = selectedAccount.getBalance() - amount;

        // Create utility payment
        UtilityPayment utilityPayment = new UtilityPayment();
        utilityPayment.setUserId(userId);
        utilityPayment.setFromAccountId(selectedAccount.getAccountId());
        utilityPayment.setUtilityType("ecommerce");
        utilityPayment.setAmount(amount);
        utilityPayment.setStatus("completed");
        utilityPayment.setOrderId(orderId);
        utilityPayment.setMerchantName(merchantName);
        utilityPayment.setDescription("Thanh toán đơn hàng " + orderId + " tại " + merchantName);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(selectedAccount.getAccountId());
        transaction.setToAccountId(merchantName); // Merchant as recipient
        transaction.setAmount(amount);
        transaction.setType("ecommerce_payment");
        transaction.setStatus("completed");

        // Use batch write for atomicity
        WriteBatch batch = db.batch();

        // Update account balance
        batch.update(db.collection("accounts").document(selectedAccount.getAccountId()), "balance", newBalance);

        // Create utility payment
        String paymentId = db.collection("utility_payments").document().getId();
        utilityPayment.setPaymentId(paymentId);
        batch.set(db.collection("utility_payments").document(paymentId), utilityPayment);

        // Create transaction
        String transactionId = db.collection("transactions").document().getId();
        transaction.setTransactionId(transactionId);
        batch.set(db.collection("transactions").document(transactionId), transaction);

        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Thanh toán thành công cho đơn hàng " + orderId + "!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}





