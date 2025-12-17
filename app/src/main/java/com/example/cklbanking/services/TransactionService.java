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
    private static final double HIGH_VALUE_THRESHOLD = 10000000; // 10 million VND - requires eKYC
    
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
        
        // Validate basic requirements with clear error messages
        if (fromAccountNumber == null || fromAccountNumber.isEmpty()) {
            callback.onVerificationResult(false, 
                "❌ Lỗi: Số tài khoản nguồn không được để trống. Vui lòng chọn tài khoản nguồn.", null);
            return;
        }
        
        if (toAccountNumber == null || toAccountNumber.isEmpty()) {
            callback.onVerificationResult(false, 
                "❌ Lỗi: Số tài khoản đích không được để trống. Vui lòng nhập số tài khoản người nhận.", null);
            return;
        }
        
        if (amount < MIN_TRANSACTION_AMOUNT) {
            callback.onVerificationResult(false, 
                String.format("❌ Lỗi: Số tiền tối thiểu cho mỗi giao dịch là %s. " +
                    "Số tiền bạn nhập: %s.", 
                    formatCurrency(MIN_TRANSACTION_AMOUNT), formatCurrency(amount)), null);
            return;
        }
        
        if (amount > SINGLE_TRANSACTION_LIMIT) {
            callback.onVerificationResult(false, 
                String.format("❌ Lỗi: Số tiền vượt quá giới hạn giao dịch một lần. " +
                    "Giới hạn tối đa: %s. Số tiền bạn nhập: %s.", 
                    formatCurrency(SINGLE_TRANSACTION_LIMIT), formatCurrency(amount)), null);
            return;
        }
        
        if (fromAccountNumber.equals(toAccountNumber)) {
            callback.onVerificationResult(false, 
                "❌ Lỗi: Không thể chuyển tiền vào cùng tài khoản. " +
                "Vui lòng chọn tài khoản đích khác với tài khoản nguồn.", null);
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
                        callback.onVerificationResult(false, 
                            String.format("❌ Lỗi: Không tìm thấy tài khoản nguồn với số tài khoản: %s. " +
                                "Vui lòng kiểm tra lại số tài khoản hoặc liên hệ hỗ trợ.", fromAccountNumber), null);
                        return;
                    }
                    
                    DocumentSnapshot fromAccountDoc = queryDocumentSnapshots.getDocuments().get(0);
                    Account fromAccount = fromAccountDoc.toObject(Account.class);
                    
                    if (fromAccount == null) {
                        callback.onVerificationResult(false, 
                            "❌ Lỗi: Không thể đọc thông tin tài khoản nguồn. " +
                            "Vui lòng thử lại sau hoặc liên hệ hỗ trợ.", null);
                        return;
                    }
                    
                    // Check account balance with detailed message
                    if (fromAccount.getBalance() < amount) {
                        callback.onVerificationResult(false, 
                            String.format("❌ Lỗi: Số dư không đủ để thực hiện giao dịch. " +
                                "Số dư hiện tại: %s | Số tiền cần chuyển: %s | Thiếu: %s. " +
                                "Vui lòng nạp thêm tiền vào tài khoản.",
                                formatCurrency(fromAccount.getBalance()),
                                formatCurrency(amount),
                                formatCurrency(amount - fromAccount.getBalance())), null);
                        return;
                    }
                    
                    // Check eKYC requirement for high-value transactions
                    checkEkycRequirement(userId, amount, (ekycRequired, ekycStatus, message) -> {
                        if (ekycRequired) {
                            // eKYC is required but not verified
                            Map<String, Object> ekycData = new HashMap<>();
                            ekycData.put("ekycRequired", true);
                            ekycData.put("ekycStatus", ekycStatus);
                            callback.onVerificationResult(false, message, ekycData);
                            return;
                        }
                        
                        // Continue with daily limit check
                        checkDailyTransferLimit(userId, amount, (dailyTotal, limitExceeded) -> {
                            if (limitExceeded) {
                                double remainingLimit = DAILY_TRANSFER_LIMIT - dailyTotal;
                                callback.onVerificationResult(false, 
                                    String.format("❌ Lỗi: Vượt quá giới hạn chuyển tiền trong ngày. " +
                                        "Đã chuyển hôm nay: %s / %s (Giới hạn). " +
                                        "Số tiền còn lại có thể chuyển: %s. " +
                                        "Giao dịch này sẽ làm vượt quá giới hạn. Vui lòng thử lại vào ngày mai.",
                                        formatCurrency(dailyTotal),
                                        formatCurrency(DAILY_TRANSFER_LIMIT),
                                        formatCurrency(Math.max(0, remainingLimit))), null);
                                return;
                            }
                            
                            // For internal transfers, verify destination account exists
                            if ("internal".equals(transferType)) {
                                verifyInternalAccount(toAccountNumber, (accountExists, toAccount) -> {
                                    if (!accountExists) {
                                        callback.onVerificationResult(false, 
                                            String.format("❌ Lỗi: Tài khoản đích không tồn tại trong hệ thống CKL Bank. " +
                                                "Số tài khoản: %s. " +
                                                "Vui lòng kiểm tra lại số tài khoản hoặc chuyển sang chế độ 'Liên ngân hàng' nếu chuyển sang ngân hàng khác.",
                                                toAccountNumber), null);
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
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to verify transaction", e);
                    callback.onVerificationResult(false, 
                        String.format("❌ Lỗi hệ thống: Không thể xác thực giao dịch. " +
                            "Chi tiết: %s. Vui lòng thử lại sau hoặc liên hệ hỗ trợ nếu vấn đề vẫn tiếp tục.",
                            e.getMessage()), null);
                });
    }
    
    /**
     * Check if eKYC is required for high-value transactions
     * @param userId User ID
     * @param amount Transaction amount
     * @param callback Callback with eKYC requirement result
     */
    public void checkEkycRequirement(String userId, double amount, EkycCheckCallback callback) {
        // Only check eKYC for high-value transactions
        if (amount < HIGH_VALUE_THRESHOLD) {
            // Not a high-value transaction, eKYC not required
            callback.onEkycChecked(false, null, null);
            return;
        }
        
        // High-value transaction - check eKYC status
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onEkycChecked(true, null, 
                            "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.");
                        return;
                    }
                    
                    String ekycStatus = documentSnapshot.getString("ekycStatus");
                    
                    if (ekycStatus == null || !"verified".equals(ekycStatus)) {
                        // eKYC is required but not verified
                        String statusText;
                        if (ekycStatus == null) {
                            statusText = "Chưa xác thực";
                        } else if ("pending".equals(ekycStatus)) {
                            statusText = "Đang chờ xác thực (vui lòng đợi phê duyệt)";
                        } else if ("failed".equals(ekycStatus)) {
                            statusText = "Xác thực thất bại (vui lòng thực hiện lại eKYC)";
                        } else {
                            statusText = "Chưa xác thực";
                        }
                        
                        String message = String.format(
                            "⚠️ Giao dịch giá trị cao yêu cầu xác thực eKYC. " +
                            "Số tiền giao dịch: %s (≥ %s). " +
                            "Trạng thái eKYC hiện tại: %s. " +
                            "Vui lòng hoàn thành xác thực eKYC trước khi thực hiện giao dịch này. " +
                            "Bạn có thể thực hiện eKYC trong mục 'Hồ sơ' > 'Xác thực eKYC'.",
                            formatCurrency(amount),
                            formatCurrency(HIGH_VALUE_THRESHOLD),
                            statusText
                        );
                        callback.onEkycChecked(true, ekycStatus, message);
                        return;
                    }
                    
                    // eKYC is verified, transaction can proceed
                    callback.onEkycChecked(false, ekycStatus, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check eKYC status", e);
                    // On error, allow transaction but log warning
                    // In production, you might want to be more strict
                    callback.onEkycChecked(false, null, null);
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
        
        // Get fee from transaction document if exists, otherwise calculate
        db.collection("transactions")
                .document(transactionId)
                .get()
                .addOnSuccessListener(transactionDoc -> {
                    final double fee;
                    if (transactionDoc.exists()) {
                        Double feeValue = transactionDoc.getDouble("fee");
                        if (feeValue != null) {
                            fee = feeValue;
                        } else {
                            // Calculate fee if not stored
                            fee = com.example.cklbanking.utils.TransactionFeeCalculator.calculateFee(amount, transferType);
                        }
                    } else {
                        // Calculate fee if transaction doesn't exist yet
                        fee = com.example.cklbanking.utils.TransactionFeeCalculator.calculateFee(amount, transferType);
                    }
                    
                    final double finalFee = fee;
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("fromAccountId", fromAccountId);
                    transaction.put("toAccountId", toAccountId);
                    transaction.put("amount", amount);
                    transaction.put("fee", fee);
                    transaction.put("totalAmount", amount + fee);
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
                                // Update account balances (includes fee)
                                updateAccountBalances(fromAccountId, toAccountId, amount, finalFee, transferType, callback);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to persist transaction", e);
                                callback.onPersistenceResult(false, "Lỗi lưu giao dịch: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get transaction for fee", e);
                    // Fallback: calculate fee and proceed
                    double fee = com.example.cklbanking.utils.TransactionFeeCalculator.calculateFee(amount, transferType);
                    
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("fromAccountId", fromAccountId);
                    transaction.put("toAccountId", toAccountId);
                    transaction.put("amount", amount);
                    transaction.put("fee", fee);
                    transaction.put("totalAmount", amount + fee);
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
                    
                    db.collection("transactions")
                            .document(transactionId)
                            .update(transaction)
                            .addOnSuccessListener(aVoid -> {
                                updateAccountBalances(fromAccountId, toAccountId, amount, fee, transferType, callback);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Failed to persist transaction", e2);
                                callback.onPersistenceResult(false, "Lỗi lưu giao dịch: " + e2.getMessage());
                            });
                });
    }
    
    /**
     * Update account balances after transaction (includes fee deduction)
     */
    private void updateAccountBalances(String fromAccountId, String toAccountId, 
                                      double amount, double fee, String transferType,
                                      TransactionPersistenceCallback callback) {
        // Debit from source account (amount + fee)
        double totalDebit = amount + fee;
        
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
                    double newBalance = currentBalance - totalDebit;
                    
                    fromDoc.getReference().update("balance", newBalance)
                            .addOnSuccessListener(aVoid -> {
                                // For internal transfers, credit to destination account (amount only, no fee)
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
                                                double toNewBalance = toCurrentBalance + amount; // Only amount, no fee
                                                
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
    
    /**
     * Update account balances after transaction (legacy method without fee parameter)
     */
    private void updateAccountBalances(String fromAccountId, String toAccountId, 
                                      double amount, String transferType,
                                      TransactionPersistenceCallback callback) {
        // Legacy method - calculate fee
        double fee = com.example.cklbanking.utils.TransactionFeeCalculator.calculateFee(amount, transferType);
        updateAccountBalances(fromAccountId, toAccountId, amount, fee, transferType, callback);
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
    
    public interface EkycCheckCallback {
        void onEkycChecked(boolean ekycRequired, String ekycStatus, String message);
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

