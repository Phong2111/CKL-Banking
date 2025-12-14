package com.example.cklbanking.services;

import android.util.Log;
import com.example.cklbanking.models.Account;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TransactionService {
    private static final String TAG = "TransactionService";
    private static final double DAILY_TRANSFER_LIMIT = 50000000; // 50 million VND per day
    private static final double SINGLE_TRANSACTION_LIMIT = 100000000; // 100 million VND per transaction
    private static final double MIN_TRANSACTION_AMOUNT = 10000; // 10,000 VND minimum
    
    private FirebaseFirestore db;
    
    public TransactionService() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Verify if a transaction is allowed and valid before persisting
     * @param fromAccountNumber Source account number
     * @param toAccountNumber Destination account number
     * @param amount Transaction amount
     * @param userId User ID
     * @param transferType "internal" or "external"
     * @param callback Callback with verification result
     */
    public void verifyTransaction(String fromAccountNumber, String toAccountNumber, 
                                  double amount, String userId, String transferType,
                                  TransactionVerificationCallback callback) {
        
        // Validate basic requirements
        if (fromAccountNumber == null || fromAccountNumber.isEmpty()) {
            callback.onVerificationResult(false, "Số tài khoản nguồn không hợp lệ", null);
            return;
        }
        
        if (toAccountNumber == null || toAccountNumber.isEmpty()) {
            callback.onVerificationResult(false, "Số tài khoản đích không hợp lệ", null);
            return;
        }
        
        if (amount < MIN_TRANSACTION_AMOUNT) {
            callback.onVerificationResult(false, 
                "Số tiền tối thiểu là " + formatCurrency(MIN_TRANSACTION_AMOUNT), null);
            return;
        }
        
        if (amount > SINGLE_TRANSACTION_LIMIT) {
            callback.onVerificationResult(false, 
                "Số tiền vượt quá giới hạn giao dịch: " + formatCurrency(SINGLE_TRANSACTION_LIMIT), null);
            return;
        }
        
        if (fromAccountNumber.equals(toAccountNumber)) {
            callback.onVerificationResult(false, "Không thể chuyển tiền vào cùng tài khoản", null);
            return;
        }
        
        // Load source account
        db.collection("accounts")
                .whereEqualTo("accountNumber", fromAccountNumber)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onVerificationResult(false, "Không tìm thấy tài khoản nguồn", null);
                        return;
                    }
                    
                    DocumentSnapshot fromAccountDoc = queryDocumentSnapshots.getDocuments().get(0);
                    Account fromAccount = fromAccountDoc.toObject(Account.class);
                    
                    if (fromAccount == null) {
                        callback.onVerificationResult(false, "Lỗi đọc thông tin tài khoản nguồn", null);
                        return;
                    }
                    
                    // Check account balance
                    if (fromAccount.getBalance() < amount) {
                        callback.onVerificationResult(false, 
                            "Số dư không đủ. Số dư hiện tại: " + formatCurrency(fromAccount.getBalance()), null);
                        return;
                    }
                    
                    // Check daily transfer limit
                    checkDailyTransferLimit(userId, amount, (dailyTotal, limitExceeded) -> {
                        if (limitExceeded) {
                            callback.onVerificationResult(false, 
                                "Vượt quá giới hạn chuyển tiền trong ngày. Đã chuyển: " + 
                                formatCurrency(dailyTotal) + " / " + formatCurrency(DAILY_TRANSFER_LIMIT), null);
                            return;
                        }
                        
                        // For internal transfers, verify destination account exists
                        if ("internal".equals(transferType)) {
                            verifyInternalAccount(toAccountNumber, (accountExists, toAccount) -> {
                                if (!accountExists) {
                                    callback.onVerificationResult(false, 
                                        "Tài khoản đích không tồn tại trong hệ thống", null);
                                    return;
                                }
                                
                                // All validations passed
                                Map<String, Object> verificationData = new HashMap<>();
                                verificationData.put("fromAccount", fromAccount);
                                verificationData.put("toAccount", toAccount);
                                verificationData.put("dailyTotal", dailyTotal);
                                
                                callback.onVerificationResult(true, "Giao dịch hợp lệ", verificationData);
                            });
                        } else {
                            // External transfer - no need to verify destination account
                            Map<String, Object> verificationData = new HashMap<>();
                            verificationData.put("fromAccount", fromAccount);
                            verificationData.put("dailyTotal", dailyTotal);
                            
                            callback.onVerificationResult(true, "Giao dịch hợp lệ", verificationData);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to verify transaction", e);
                    callback.onVerificationResult(false, "Lỗi xác thực giao dịch: " + e.getMessage(), null);
                });
    }
    
    /**
     * Check daily transfer limit for a user
     */
    private void checkDailyTransferLimit(String userId, double newAmount, 
                                         DailyLimitCallback callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();
        com.google.firebase.Timestamp startTimestamp = new com.google.firebase.Timestamp(startOfDay);
        
        // Get all completed transfers today
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "transfer")
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double dailyTotal = 0;
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double amount = doc.getDouble("amount");
                        if (amount != null) {
                            dailyTotal += amount;
                        }
                    }
                    
                    // Add new transaction amount
                    double newDailyTotal = dailyTotal + newAmount;
                    boolean limitExceeded = newDailyTotal > DAILY_TRANSFER_LIMIT;
                    
                    callback.onDailyLimitChecked(dailyTotal, limitExceeded);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check daily limit", e);
                    // On error, allow transaction but log warning
                    callback.onDailyLimitChecked(0, false);
                });
    }
    
    /**
     * Verify internal account exists
     */
    private void verifyInternalAccount(String accountNumber, AccountVerificationCallback callback) {
        db.collection("accounts")
                .whereEqualTo("accountNumber", accountNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onAccountVerified(false, null);
                        return;
                    }
                    
                    Account account = queryDocumentSnapshots.getDocuments().get(0).toObject(Account.class);
                    callback.onAccountVerified(true, account);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to verify account", e);
                    callback.onAccountVerified(false, null);
                });
    }
    
    /**
     * Persist transaction to database after verification and payment
     * This should only be called after OTP verification and payment processing
     */
    public void persistTransaction(String transactionId, String fromAccountId, String toAccountId,
                                   double amount, String type, String transferType, 
                                   String recipientName, String description, String recipientBank,
                                   TransactionPersistenceCallback callback) {
        
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("fromAccountId", fromAccountId);
        transaction.put("toAccountId", toAccountId);
        transaction.put("amount", amount);
        transaction.put("type", type);
        transaction.put("status", "completed");
        transaction.put("timestamp", com.google.firebase.Timestamp.now());
        transaction.put("recipientName", recipientName);
        transaction.put("transferType", transferType);
        transaction.put("description", description);
        transaction.put("requiresOTP", true);
        transaction.put("otpVerified", true);
        
        if (recipientBank != null) {
            transaction.put("recipientBank", recipientBank);
        }
        
        // Update transaction document
        db.collection("transactions")
                .document(transactionId)
                .update(transaction)
                .addOnSuccessListener(aVoid -> {
                    // Update account balances
                    updateAccountBalances(fromAccountId, toAccountId, amount, transferType, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to persist transaction", e);
                    callback.onPersistenceResult(false, "Lỗi lưu giao dịch: " + e.getMessage());
                });
    }
    
    /**
     * Update account balances after transaction
     */
    private void updateAccountBalances(String fromAccountId, String toAccountId, 
                                      double amount, String transferType,
                                      TransactionPersistenceCallback callback) {
        // Debit from source account
        db.collection("accounts")
                .whereEqualTo("accountNumber", fromAccountId)
                .limit(1)
                .get()
                .addOnSuccessListener(fromQuery -> {
                    if (fromQuery.isEmpty()) {
                        callback.onPersistenceResult(false, "Không tìm thấy tài khoản nguồn");
                        return;
                    }
                    
                    DocumentSnapshot fromDoc = fromQuery.getDocuments().get(0);
                    double currentBalance = fromDoc.getDouble("balance");
                    double newBalance = currentBalance - amount;
                    
                    fromDoc.getReference().update("balance", newBalance)
                            .addOnSuccessListener(aVoid -> {
                                // For internal transfers, credit to destination account
                                if ("internal".equals(transferType)) {
                                    db.collection("accounts")
                                            .whereEqualTo("accountNumber", toAccountId)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(toQuery -> {
                                                if (toQuery.isEmpty()) {
                                                    // Log warning but don't fail transaction
                                                    Log.w(TAG, "Destination account not found for internal transfer");
                                                    callback.onPersistenceResult(true, "Giao dịch thành công");
                                                    return;
                                                }
                                                
                                                DocumentSnapshot toDoc = toQuery.getDocuments().get(0);
                                                double toCurrentBalance = toDoc.getDouble("balance");
                                                double toNewBalance = toCurrentBalance + amount;
                                                
                                                toDoc.getReference().update("balance", toNewBalance)
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            callback.onPersistenceResult(true, "Giao dịch thành công");
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Failed to update destination account", e);
                                                            callback.onPersistenceResult(true, 
                                                                "Giao dịch thành công nhưng có lỗi cập nhật tài khoản đích");
                                                        });
                                            });
                                } else {
                                    // External transfer - no need to update destination
                                    callback.onPersistenceResult(true, "Giao dịch thành công");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update source account", e);
                                callback.onPersistenceResult(false, "Lỗi cập nhật số dư tài khoản");
                            });
                });
    }
    
    private String formatCurrency(double amount) {
        return String.format("%,.0f ₫", amount);
    }
    
    /**
     * Callback interfaces
     */
    public interface TransactionVerificationCallback {
        void onVerificationResult(boolean isValid, String message, Map<String, Object> verificationData);
    }
    
    private interface DailyLimitCallback {
        void onDailyLimitChecked(double dailyTotal, boolean limitExceeded);
    }
    
    private interface AccountVerificationCallback {
        void onAccountVerified(boolean exists, Account account);
    }
    
    public interface TransactionPersistenceCallback {
        void onPersistenceResult(boolean success, String message);
    }
}

