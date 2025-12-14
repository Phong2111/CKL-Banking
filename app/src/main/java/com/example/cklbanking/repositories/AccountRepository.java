package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.cklbanking.models.Account; // Import model Account

public class AccountRepository {

    private static final String COLLECTION_NAME = "accounts";
    private final CollectionReference accountCollection;

    public AccountRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.accountCollection = db.collection(COLLECTION_NAME);
    }

    // Dùng cho TV3 (Cán bộ) tạo tài khoản
    public Task<Void> createAccount(Account account) {
        // Tự tạo ID ngẫu nhiên
        String newAccountId = accountCollection.document().getId();
        account.setAccountId(newAccountId);
        return accountCollection.document(newAccountId).set(account);
    }

    // Dùng cho TV2 (Khách hàng) xem danh sách tài khoản
    public Query getAccountsForUser(String uid) {
        return accountCollection.whereEqualTo("userId", uid);
    }

    // Lấy account theo ID
    public Task<DocumentSnapshot> getAccountById(String accountId) {
        return accountCollection.document(accountId).get();
    }

    // Cập nhật số dư tài khoản
    public Task<Void> updateBalance(String accountId, double newBalance) {
        return accountCollection.document(accountId).update("balance", newBalance);
    }

    // Cập nhật lãi suất (dành cho banking officer)
    public Task<Void> updateInterestRate(String accountId, double newInterestRate) {
        return accountCollection.document(accountId).update("interestRate", newInterestRate);
    }

    // Cập nhật thông tin account (generic update)
    public Task<Void> updateAccount(String accountId, Account account) {
        return accountCollection.document(accountId).set(account);
    }

    // Lấy tất cả saving accounts (để officer quản lý interest rates)
    public Query getAllSavingAccounts() {
        return accountCollection.whereEqualTo("accountType", "saving");
    }
}
