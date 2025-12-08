package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WithdrawActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerAccount;
    private TextInputEditText editAmount;
    private TextView textAvailableBalance;
    private RadioGroup radioGroupMethod;
    private MaterialButton btnWithdraw;
    private MaterialButton btn500k, btn1m, btn2m, btn3m, btn5m, btn10m;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<Account> accounts;
    private String selectedAccountId;
    private double availableBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupListeners();
        loadAccounts();
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
        
        db.collection("accounts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accounts = new ArrayList<>();
                    List<String> accountNames = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
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
            availableBalance = accounts.get(position).getBalance();
            textAvailableBalance.setText("Số dư khả dụng: " + formatCurrency(availableBalance));
        }
    }

    private void processWithdraw() {
        String amountStr = editAmount.getText().toString().trim();
        
        if (TextUtils.isEmpty(amountStr)) {
            editAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        
        double amount = Double.parseDouble(amountStr);
        
        if (amount < 100000) {
            editAmount.setError("Số tiền rút tối thiểu là 100.000 VNĐ");
            return;
        }
        
        if (amount > availableBalance) {
            editAmount.setError("Số dư không đủ");
            return;
        }
        
        int selectedPos = spinnerAccount.getSelectedItemPosition();
        if (selectedPos < 0 || accounts.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectedAccountId = accounts.get(selectedPos).getAccountId();
        
        // Create withdraw request
        Map<String, Object> withdrawRequest = new HashMap<>();
        withdrawRequest.put("accountId", selectedAccountId);
        withdrawRequest.put("amount", amount);
        withdrawRequest.put("method", getSelectedMethod());
        withdrawRequest.put("status", "pending");
        withdrawRequest.put("timestamp", System.currentTimeMillis());
        withdrawRequest.put("userId", mAuth.getCurrentUser().getUid());
        
        btnWithdraw.setEnabled(false);
        
        db.collection("withdraw_requests")
                .add(withdrawRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Yêu cầu rút tiền đã được gửi!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnWithdraw.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        return formatter.format(amount);
    }
}
