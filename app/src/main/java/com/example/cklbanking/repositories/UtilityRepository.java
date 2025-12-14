package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.cklbanking.models.UtilityPayment;

public class UtilityRepository {

    private static final String COLLECTION_NAME = "utility_payments";
    private final CollectionReference utilityCollection;

    public UtilityRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.utilityCollection = db.collection(COLLECTION_NAME);
    }

    // Tạo utility payment mới
    public Task<Void> createUtilityPayment(UtilityPayment payment) {
        String newPaymentId = utilityCollection.document().getId();
        payment.setPaymentId(newPaymentId);
        return utilityCollection.document(newPaymentId).set(payment);
    }

    // Lấy utility payment theo ID
    public Task<DocumentSnapshot> getUtilityPaymentById(String paymentId) {
        return utilityCollection.document(paymentId).get();
    }

    // Lấy tất cả utility payments của một user
    public Query getUtilityPaymentsByUser(String userId) {
        return utilityCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Lấy utility payments của user theo loại
    public Query getUtilityPaymentsByUserAndType(String userId, String utilityType) {
        return utilityCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("utilityType", utilityType)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Lấy utility payments của một tài khoản
    public Query getUtilityPaymentsByAccount(String accountId) {
        return utilityCollection
                .whereEqualTo("fromAccountId", accountId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Cập nhật trạng thái payment
    public Task<Void> updatePaymentStatus(String paymentId, String status) {
        return utilityCollection.document(paymentId).update("status", status);
    }
}





