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
    private MaterialButton btnUpdateAll;

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
        btnUpdateAll = findViewById(R.id.btnUpdateAll);
        
        // Setup update all button
        if (btnUpdateAll != null) {
            btnUpdateAll.setOnClickListener(v -> showUpdateAllDialog());
        }
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
                        // Validation: Lãi suất phải > 0 và < 20%
                        if (newRate <= 0 || newRate >= 20) {
                            Toast.makeText(this, "Lãi suất phải lớn hơn 0% và nhỏ hơn 20%", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Cảnh báo khi thay đổi lớn (> 2%)
                        double oldRate = account.getInterestRate();
                        double change = Math.abs(newRate - oldRate);
                        if (change > 2.0) {
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Cảnh báo")
                                    .setMessage(String.format("Bạn đang thay đổi lãi suất từ %.2f%% lên %.2f%% (thay đổi %.2f%%).\n\nThay đổi này có thể ảnh hưởng lớn đến khách hàng. Bạn có chắc chắn muốn tiếp tục?", 
                                            oldRate, newRate, change))
                                    .setPositiveButton("Xác nhận", (dialogInterface2, which2) -> {
                                        updateInterestRate(account, newRate);
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
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
        
        // Lưu lịch sử thay đổi (optional)
        saveInterestRateHistory(account, account.getInterestRate(), newRate);

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
    
    /**
     * Hiển thị dialog để cập nhật lãi suất cho tất cả Saving accounts
     */
    private void showUpdateAllDialog() {
        if (savingAccounts == null || savingAccounts.isEmpty()) {
            Toast.makeText(this, "Không có tài khoản tiết kiệm nào", Toast.LENGTH_SHORT).show();
            return;
        }
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_interest_rate, null);
        TextInputEditText editInterestRate = dialogView.findViewById(R.id.editInterestRate);
        
        // Hiển thị lãi suất hiện tại (lấy từ account đầu tiên làm mẫu)
        DecimalFormat df = new DecimalFormat("#.##");
        if (!savingAccounts.isEmpty()) {
            editInterestRate.setText(df.format(savingAccounts.get(0).getInterestRate()));
        }
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật lãi suất cho tất cả tài khoản tiết kiệm")
                .setMessage("Lãi suất mới sẽ được áp dụng cho tất cả " + savingAccounts.size() + " tài khoản tiết kiệm.")
                .setView(dialogView)
                .setPositiveButton("Cập nhật tất cả", (dialogInterface, which) -> {
                    String rateStr = editInterestRate.getText().toString().trim();
                    if (TextUtils.isEmpty(rateStr)) {
                        Toast.makeText(this, "Vui lòng nhập lãi suất", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    try {
                        double newRate = Double.parseDouble(rateStr);
                        // Validation: Lãi suất phải > 0 và < 20%
                        if (newRate <= 0 || newRate >= 20) {
                            Toast.makeText(this, "Lãi suất phải lớn hơn 0% và nhỏ hơn 20%", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Cảnh báo khi cập nhật tất cả
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Xác nhận")
                                .setMessage(String.format("Bạn có chắc chắn muốn cập nhật lãi suất %.2f%% cho tất cả %d tài khoản tiết kiệm?", 
                                        newRate, savingAccounts.size()))
                                .setPositiveButton("Xác nhận", (dialogInterface2, which2) -> {
                                    updateAllInterestRates(newRate);
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .create();
        
        dialog.show();
    }
    
    /**
     * Cập nhật lãi suất cho tất cả Saving accounts
     */
    private void updateAllInterestRates(double newRate) {
        showLoading(true);
        
        int totalAccounts = savingAccounts.size();
        final int[] successCount = {0};
        final int[] failCount = {0};
        
        for (Account account : savingAccounts) {
            double oldRate = account.getInterestRate();
            
            // Lưu lịch sử
            saveInterestRateHistory(account, oldRate, newRate);
            
            accountRepository.updateInterestRate(account.getAccountId(), newRate)
                    .addOnSuccessListener(aVoid -> {
                        account.setInterestRate(newRate);
                        successCount[0]++;
                        
                        // Kiểm tra xem đã cập nhật hết chưa
                        if (successCount[0] + failCount[0] == totalAccounts) {
                            showLoading(false);
                            adapter.notifyDataSetChanged();
                            
                            if (failCount[0] == 0) {
                                Toast.makeText(this, 
                                        String.format("Đã cập nhật lãi suất thành công cho %d tài khoản!", successCount[0]), 
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, 
                                        String.format("Đã cập nhật %d/%d tài khoản. %d tài khoản gặp lỗi.", 
                                                successCount[0], totalAccounts, failCount[0]), 
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        failCount[0]++;
                        
                        // Kiểm tra xem đã cập nhật hết chưa
                        if (successCount[0] + failCount[0] == totalAccounts) {
                            showLoading(false);
                            
                            if (successCount[0] > 0) {
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, 
                                        String.format("Đã cập nhật %d/%d tài khoản. %d tài khoản gặp lỗi.", 
                                                successCount[0], totalAccounts, failCount[0]), 
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, 
                                        "Lỗi cập nhật tất cả tài khoản: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
    
    /**
     * Lưu lịch sử thay đổi lãi suất vào Firestore (optional)
     */
    private void saveInterestRateHistory(Account account, double oldRate, double newRate) {
        if (mAuth.getCurrentUser() == null) return;
        
        java.util.Map<String, Object> history = new java.util.HashMap<>();
        history.put("accountId", account.getAccountId());
        history.put("accountNumber", account.getAccountNumber());
        history.put("oldRate", oldRate);
        history.put("newRate", newRate);
        history.put("changedBy", mAuth.getCurrentUser().getUid());
        history.put("timestamp", com.google.firebase.Timestamp.now());
        
        db.collection("interest_rate_history")
                .add(history)
                .addOnFailureListener(e -> {
                    // Silent fail - không ảnh hưởng đến flow chính
                    android.util.Log.e("ManageInterestRates", "Failed to save interest rate history", e);
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











