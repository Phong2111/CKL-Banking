package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
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
}
