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
import java.util.regex.Pattern;

public class PhoneRechargeActivity extends AppCompatActivity {

    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private Spinner spinnerTelecom;
    private Spinner spinnerAccount;
    private TextInputEditText editPhoneNumber;
    private TextInputEditText editAmount;
    private TextView textAccountBalance;
    private MaterialButton btnRecharge;
    private MaterialButton btn50k, btn100k, btn200k, btn500k, btn1m, btn2m;
    private CircularProgressIndicator progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private UtilityRepository utilityRepository;

    private List<Account> accounts;
    private Account selectedAccount;
    private String selectedTelecom;

    private static final String[] TELECOM_PROVIDERS = {"Viettel", "VinaPhone", "Mobifone"};
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0[3|5|7|8|9])+([0-9]{8})$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_recharge);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        utilityRepository = new UtilityRepository();

        initViews();
        setupToolbar();
        setupTelecomSpinner();
        setupListeners();
        loadAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerTelecom = findViewById(R.id.spinnerTelecom);
        spinnerAccount = findViewById(R.id.spinnerAccount);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editAmount = findViewById(R.id.editAmount);
        textAccountBalance = findViewById(R.id.textAccountBalance);
        btnRecharge = findViewById(R.id.btnRecharge);
        btn50k = findViewById(R.id.btn50k);
        btn100k = findViewById(R.id.btn100k);
        btn200k = findViewById(R.id.btn200k);
        btn500k = findViewById(R.id.btn500k);
        btn1m = findViewById(R.id.btn1m);
        btn2m = findViewById(R.id.btn2m);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTelecomSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TELECOM_PROVIDERS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTelecom.setAdapter(adapter);
        spinnerTelecom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTelecom = TELECOM_PROVIDERS[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTelecom = TELECOM_PROVIDERS[0];
            }
        });
        selectedTelecom = TELECOM_PROVIDERS[0];
    }

    private void setupListeners() {
        // Quick amount buttons
        btn50k.setOnClickListener(v -> editAmount.setText("50000"));
        btn100k.setOnClickListener(v -> editAmount.setText("100000"));
        btn200k.setOnClickListener(v -> editAmount.setText("200000"));
        btn500k.setOnClickListener(v -> editAmount.setText("500000"));
        btn1m.setOnClickListener(v -> editAmount.setText("1000000"));
        btn2m.setOnClickListener(v -> editAmount.setText("2000000"));

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

        btnRecharge.setOnClickListener(v -> processRecharge());
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

    private void processRecharge() {
        // Validate input
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            editPhoneNumber.setError("Vui lòng nhập số điện thoại");
            return;
        }

        // Validate phone number format (Vietnamese phone: 10 digits starting with 0)
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            editPhoneNumber.setError("Số điện thoại không hợp lệ. Ví dụ: 0912345678");
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
            editAmount.setError("Số tiền nạp tối thiểu là 10.000 VNĐ");
            return;
        }

        if (amount > 5000000) {
            editAmount.setError("Số tiền nạp tối đa là 5.000.000 VNĐ");
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

        performRecharge(phoneNumber, amount);
    }

    private void performRecharge(String phoneNumber, double amount) {
        btnRecharge.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();

        // Calculate new balance
        double newBalance = selectedAccount.getBalance() - amount;

        // Create utility payment
        UtilityPayment utilityPayment = new UtilityPayment();
        utilityPayment.setUserId(userId);
        utilityPayment.setFromAccountId(selectedAccount.getAccountId());
        utilityPayment.setUtilityType("phone_recharge");
        utilityPayment.setAmount(amount);
        utilityPayment.setStatus("completed");
        utilityPayment.setPhoneNumber(phoneNumber);
        utilityPayment.setTelecomProvider(selectedTelecom);
        utilityPayment.setDescription("Nạp tiền " + selectedTelecom + " - SĐT: " + phoneNumber);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(selectedAccount.getAccountId());
        transaction.setToAccountId(selectedTelecom); // Telecom provider as recipient
        transaction.setAmount(amount);
        transaction.setType("phone_recharge");
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
                    Toast.makeText(this, "Nạp tiền thành công cho số " + phoneNumber + "!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRecharge.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}












