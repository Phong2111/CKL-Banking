package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.repositories.AccountRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ManageInterestRatesActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private TextView emptyStateText;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AccountRepository accountRepository;

    // Data
    private List<Account> savingAccounts;
    private SavingAccountAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_interest_rates);

        // Check permission - only officers can access
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        accountRepository = new AccountRepository();

        checkUserRole();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadSavingAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        savingAccounts = new ArrayList<>();
        adapter = new SavingAccountAdapter(savingAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void checkUserRole() {
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (!"staff".equals(role) && !"officer".equals(role)) {
                            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    private void loadSavingAccounts() {
        showLoading(true);
        emptyStateText.setVisibility(View.GONE);

        accountRepository.getAllSavingAccounts()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savingAccounts.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
                        account.setAccountId(document.getId());
                        savingAccounts.add(account);
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);

                    if (savingAccounts.isEmpty()) {
                        emptyStateText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải danh sách: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditInterestRateDialog(Account account) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_interest_rate, null);
        TextInputEditText editInterestRate = dialogView.findViewById(R.id.editInterestRate);

        // Hiển thị lãi suất hiện tại
        DecimalFormat df = new DecimalFormat("#.##");
        editInterestRate.setText(df.format(account.getInterestRate()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Chỉnh sửa lãi suất")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialogInterface, which) -> {
                    String rateStr = editInterestRate.getText().toString().trim();
                    if (TextUtils.isEmpty(rateStr)) {
                        Toast.makeText(this, "Vui lòng nhập lãi suất", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double newRate = Double.parseDouble(rateStr);
                        if (newRate < 0 || newRate > 100) {
                            Toast.makeText(this, "Lãi suất phải từ 0% đến 100%", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        updateInterestRate(account, newRate);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .create();

        dialog.show();
    }

    private void updateInterestRate(Account account, double newRate) {
        showLoading(true);

        accountRepository.updateInterestRate(account.getAccountId(), newRate)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Cập nhật lãi suất thành công!", Toast.LENGTH_SHORT).show();
                    // Cập nhật local data
                    account.setInterestRate(newRate);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Adapter for RecyclerView
    private class SavingAccountAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SavingAccountAdapter.ViewHolder> {
        private List<Account> accounts;

        public SavingAccountAdapter(List<Account> accounts) {
            this.accounts = accounts;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_saving_account_rate, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Account account = accounts.get(position);
            holder.bind(account);
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private TextView accountNumber, accountOwner, currentRate, balance;
            private MaterialButton btnEdit;

            public ViewHolder(View itemView) {
                super(itemView);
                accountNumber = itemView.findViewById(R.id.accountNumber);
                accountOwner = itemView.findViewById(R.id.accountOwner);
                currentRate = itemView.findViewById(R.id.currentRate);
                balance = itemView.findViewById(R.id.balance);
                btnEdit = itemView.findViewById(R.id.btnEdit);
            }

            public void bind(Account account) {
                accountNumber.setText("Số tài khoản: " + account.getAccountNumber());
                currentRate.setText(String.format("%.2f%%/năm", account.getInterestRate()));
                balance.setText(formatCurrency(account.getBalance()));

                // Load owner name
                db.collection("users").document(account.getUserId()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String ownerName = documentSnapshot.getString("fullName");
                                accountOwner.setText("Chủ tài khoản: " + (ownerName != null ? ownerName : "N/A"));
                            } else {
                                accountOwner.setText("Chủ tài khoản: N/A");
                            }
                        })
                        .addOnFailureListener(e -> {
                            accountOwner.setText("Chủ tài khoản: N/A");
                        });

                btnEdit.setOnClickListener(v -> showEditInterestRateDialog(account));
            }

            private String formatCurrency(double amount) {
                return String.format("%,.0f ₫", amount);
            }
        }
    }
}





