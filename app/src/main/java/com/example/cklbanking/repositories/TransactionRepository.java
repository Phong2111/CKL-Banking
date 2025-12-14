package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.cklbanking.models.Transaction;

public class TransactionRepository {

    private static final String COLLECTION_NAME = "transactions";
    private final CollectionReference transactionCollection;

    public TransactionRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.transactionCollection = db.collection(COLLECTION_NAME);
    }

    // Tạo transaction mới
    public Task<Void> createTransaction(Transaction transaction) {
        String newTransactionId = transactionCollection.document().getId();
        transaction.setTransactionId(newTransactionId);
        return transactionCollection.document(newTransactionId).set(transaction);
    }

    // Lấy tất cả transactions của một account (cả fromAccountId và toAccountId)
    public Query getTransactionsByAccount(String accountId) {
        return transactionCollection
                .whereEqualTo("fromAccountId", accountId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Lấy transactions của account theo loại
    public Query getTransactionsByAccountAndType(String accountId, String type) {
        return transactionCollection
                .whereEqualTo("fromAccountId", accountId)
                .whereEqualTo("type", type)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Lấy transactions của account (bao gồm cả gửi và nhận)
    public Query getAllTransactionsForAccount(String accountId) {
        // Firestore không hỗ trợ OR query trực tiếp, nên cần query riêng
        // Hoặc có thể lưu thêm field accountIds (array) để query dễ hơn
        return transactionCollection
                .whereEqualTo("fromAccountId", accountId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }
}
