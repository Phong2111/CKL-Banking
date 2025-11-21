package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepositActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerAccount;
    private TextInputEditText editAmount;
    private RadioGroup radioGroupMethod;
    private MaterialButton btnDeposit;
    private MaterialButton btn100k, btn500k, btn1m, btn2m, btn5m, btn10m;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<Account> accounts;
    private String selectedAccountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

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
        radioGroupMethod = findViewById(R.id.radioGroupMethod);
        btnDeposit = findViewById(R.id.btnDeposit);
        
        btn100k = findViewById(R.id.btn100k);
        btn500k = findViewById(R.id.btn500k);
        btn1m = findViewById(R.id.btn1m);
        btn2m = findViewById(R.id.btn2m);
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
        btn100k.setOnClickListener(v -> editAmount.setText("100000"));
        btn500k.setOnClickListener(v -> editAmount.setText("500000"));
        btn1m.setOnClickListener(v -> editAmount.setText("1000000"));
        btn2m.setOnClickListener(v -> editAmount.setText("2000000"));
        btn5m.setOnClickListener(v -> editAmount.setText("5000000"));
        btn10m.setOnClickListener(v -> editAmount.setText("10000000"));
        
        btnDeposit.setOnClickListener(v -> processDeposit());
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
                });
    }

    private void processDeposit() {
        String amountStr = editAmount.getText().toString().trim();
        
        if (TextUtils.isEmpty(amountStr)) {
            editAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        
        double amount = Double.parseDouble(amountStr);
        
        if (amount < 10000) {
            editAmount.setError("Số tiền tối thiểu là 10.000 VNĐ");
            return;
        }
        
        int selectedPos = spinnerAccount.getSelectedItemPosition();
        if (selectedPos < 0 || accounts.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectedAccountId = accounts.get(selectedPos).getAccountId();
        
        // Create deposit request
        Map<String, Object> depositRequest = new HashMap<>();
        depositRequest.put("accountId", selectedAccountId);
        depositRequest.put("amount", amount);
        depositRequest.put("method", getSelectedMethod());
        depositRequest.put("status", "pending");
        depositRequest.put("timestamp", System.currentTimeMillis());
        depositRequest.put("userId", mAuth.getCurrentUser().getUid());
        
        btnDeposit.setEnabled(false);
        
        db.collection("deposit_requests")
                .add(depositRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Yêu cầu nạp tiền đã được gửi!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnDeposit.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedMethod() {
        int selectedId = radioGroupMethod.getCheckedRadioButtonId();
        if (selectedId == R.id.radioBankTransfer) {
            return "bank_transfer";
        } else if (selectedId == R.id.radioATM) {
            return "atm";
        } else {
            return "branch";
        }
    }
}
