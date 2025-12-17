package com.example.cklbanking.repositories;

import com.example.cklbanking.models.Bill;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository để quản lý hóa đơn điện/nước
 */
public class BillRepository {
    private static final String TAG = "BillRepository";
    private CollectionReference billsCollection;
    
    public BillRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        billsCollection = db.collection("bills");
    }
    
    /**
     * Lấy tất cả hóa đơn chưa thanh toán của một khách hàng
     */
    public Query getUnpaidBillsByCustomerCode(String customerCode) {
        return billsCollection
                .whereEqualTo("customerCode", customerCode)
                .whereEqualTo("status", "unpaid")
                .orderBy("dueDate", Query.Direction.ASCENDING);
    }
    
    /**
     * Lấy tất cả hóa đơn chưa thanh toán (cho demo - không filter theo customerCode)
     */
    public com.google.firebase.firestore.Query getAllUnpaidBills() {
        return billsCollection
                .whereEqualTo("status", "unpaid")
                .orderBy("dueDate", com.google.firebase.firestore.Query.Direction.ASCENDING);
    }
    
    /**
     * Lấy hóa đơn theo billId
     */
    public Query getBillById(String billId) {
        return billsCollection.whereEqualTo("__name__", billId).limit(1);
    }
    
    /**
     * Cập nhật trạng thái hóa đơn thành "paid"
     */
    public void markBillAsPaid(String billId) {
        billsCollection.document(billId)
                .update("status", "paid")
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "Bill marked as paid: " + billId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to mark bill as paid", e);
                });
    }
    
    /**
     * Tạo hóa đơn mới
     */
    public void createBill(Bill bill) {
        billsCollection.add(bill)
                .addOnSuccessListener(documentReference -> {
                    bill.setBillId(documentReference.getId());
                    android.util.Log.d(TAG, "Bill created: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to create bill", e);
                });
    }
}

