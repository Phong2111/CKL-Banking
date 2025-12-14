package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.repositories.AccountRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateAccountActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText editCustomerEmail, editAccountType, editAccountNumber, editBalance;
    private TextInputEditText editInterestRate, editMonthlyPayment;
    private TextInputLayout layoutInterestRate, layoutMonthlyPayment;
    private MaterialButton btnSearchCustomer, btnCreateAccount;
    private CircularProgressIndicator progressBar;
    private android.widget.TextView textCustomerInfo;

    // Firebase
    private FirebaseFirestore db;
    private AccountRepository accountRepository;

    // Data
    private String selectedCustomerId;
    private String selectedCustomerName;

    private static final String[] ACCOUNT_TYPES = {"checking", "saving", "mortgage"};
    private static final String[] ACCOUNT_TYPE_LABELS = {"Tài khoản thanh toán", "Tài khoản tiết kiệm", "Tài khoản vay thế chấp"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        accountRepository = new AccountRepository();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Listeners
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editCustomerEmail = findViewById(R.id.editCustomerEmail);
        editAccountType = findViewById(R.id.editAccountType);
        editAccountNumber = findViewById(R.id.editAccountNumber);
        editBalance = findViewById(R.id.editBalance);
        editInterestRate = findViewById(R.id.editInterestRate);
        editMonthlyPayment = findViewById(R.id.editMonthlyPayment);
        layoutInterestRate = findViewById(R.id.layoutInterestRate);
        layoutMonthlyPayment = findViewById(R.id.layoutMonthlyPayment);
        btnSearchCustomer = findViewById(R.id.btnSearchCustomer);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressBar = findViewById(R.id.progressBar);
        textCustomerInfo = findViewById(R.id.textCustomerInfo);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnSearchCustomer.setOnClickListener(v -> searchCustomer());
        editAccountType.setOnClickListener(v -> showAccountTypeDialog());
        btnCreateAccount.setOnClickListener(v -> createAccount());
    }

    private void searchCustomer() {
        String email = editCustomerEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editCustomerEmail.setError("Vui lòng nhập email khách hàng");
            editCustomerEmail.requestFocus();
            return;
        }

        showLoading(true);

        db.collection("users")
                .whereEqualTo("role", "customer")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy khách hàng với email này", Toast.LENGTH_SHORT).show();
                        selectedCustomerId = null;
                        selectedCustomerName = null;
                        textCustomerInfo.setVisibility(View.GONE);
                        btnCreateAccount.setEnabled(false);
                    } else {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        selectedCustomerId = doc.getId();
                        selectedCustomerName = doc.getString("fullName");
                        textCustomerInfo.setVisibility(View.VISIBLE);
                        textCustomerInfo.setBackgroundColor(getResources().getColor(R.color.success, null));
                        textCustomerInfo.setText("Khách hàng: " + selectedCustomerName);
                        btnCreateAccount.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAccountTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn loại tài khoản");
        builder.setItems(ACCOUNT_TYPE_LABELS, (dialog, which) -> {
            editAccountType.setText(ACCOUNT_TYPE_LABELS[which]);
            String accountType = ACCOUNT_TYPES[which];

            // Show/hide fields based on account type
            if ("saving".equals(accountType)) {
                layoutInterestRate.setVisibility(View.VISIBLE);
                layoutMonthlyPayment.setVisibility(View.GONE);
            } else if ("mortgage".equals(accountType)) {
                layoutInterestRate.setVisibility(View.GONE);
                layoutMonthlyPayment.setVisibility(View.VISIBLE);
            } else {
                layoutInterestRate.setVisibility(View.GONE);
                layoutMonthlyPayment.setVisibility(View.GONE);
            }
        });
        builder.show();
    }

    private void createAccount() {
        // Validate inputs
        if (selectedCustomerId == null) {
            Toast.makeText(this, "Vui lòng tìm kiếm và chọn khách hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String accountType = getAccountTypeFromLabel(editAccountType.getText().toString().trim());
        if (TextUtils.isEmpty(accountType)) {
            editAccountType.setError("Vui lòng chọn loại tài khoản");
            editAccountType.requestFocus();
            return;
        }

        String accountNumber = editAccountNumber.getText().toString().trim();
        if (TextUtils.isEmpty(accountNumber)) {
            editAccountNumber.setError("Vui lòng nhập số tài khoản");
            editAccountNumber.requestFocus();
            return;
        }

        String balanceStr = editBalance.getText().toString().trim();
        if (TextUtils.isEmpty(balanceStr)) {
            editBalance.setError("Vui lòng nhập số dư ban đầu");
            editBalance.requestFocus();
            return;
        }

        double balance = Double.parseDouble(balanceStr);
        if (balance < 0) {
            editBalance.setError("Số dư không được âm");
            editBalance.requestFocus();
            return;
        }

        // Validate specific fields
        double interestRate = 0;
        if ("saving".equals(accountType)) {
            String interestRateStr = editInterestRate.getText().toString().trim();
            if (TextUtils.isEmpty(interestRateStr)) {
                editInterestRate.setError("Vui lòng nhập lãi suất");
                editInterestRate.requestFocus();
                return;
            }
            interestRate = Double.parseDouble(interestRateStr);
            if (interestRate < 0 || interestRate > 100) {
                editInterestRate.setError("Lãi suất phải từ 0 đến 100%");
                editInterestRate.requestFocus();
                return;
            }
        }

        double monthlyPayment = 0;
        if ("mortgage".equals(accountType)) {
            String monthlyPaymentStr = editMonthlyPayment.getText().toString().trim();
            if (TextUtils.isEmpty(monthlyPaymentStr)) {
                editMonthlyPayment.setError("Vui lòng nhập tiền trả hàng tháng");
                editMonthlyPayment.requestFocus();
                return;
            }
            monthlyPayment = Double.parseDouble(monthlyPaymentStr);
            if (monthlyPayment <= 0) {
                editMonthlyPayment.setError("Tiền trả hàng tháng phải lớn hơn 0");
                editMonthlyPayment.requestFocus();
                return;
            }
            // Mortgage balance should be negative
            balance = -Math.abs(balance);
        }

        showLoading(true);

        // Create account object
        Account account = new Account();
        account.setUserId(selectedCustomerId);
        account.setAccountNumber(accountNumber);
        account.setAccountType(accountType);
        account.setBalance(balance);
        account.setInterestRate(interestRate);
        account.setMonthlyPayment(monthlyPayment);

        // Save to Firestore
        accountRepository.createAccount(account)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getAccountTypeFromLabel(String label) {
        for (int i = 0; i < ACCOUNT_TYPE_LABELS.length; i++) {
            if (ACCOUNT_TYPE_LABELS[i].equals(label)) {
                return ACCOUNT_TYPES[i];
            }
        }
        return "";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCreateAccount.setEnabled(!show && selectedCustomerId != null);
        btnSearchCustomer.setEnabled(!show);
    }
}

