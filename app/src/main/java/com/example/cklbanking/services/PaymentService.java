package com.example.cklbanking.services;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PaymentService {
    private static final String TAG = "PaymentService";
    public static final String VNPAY_PAYMENT_METHOD = "vnpay";
    public static final String STRIPE_PAYMENT_METHOD = "stripe";
    
    private FirebaseFirestore db;
    
    public PaymentService() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Process payment for external transfer using payment gateway
     * @param transactionId Transaction ID
     * @param amount Amount to pay
     * @param paymentMethod "vnpay" or "stripe"
     * @param recipientBank Recipient bank name
     * @param callback Payment processing callback
     */
    public void processPayment(String transactionId, double amount, String paymentMethod,
                              String recipientBank, PaymentCallback callback) {
        
        if (transactionId == null || transactionId.isEmpty()) {
            callback.onPaymentResult(false, "Transaction ID không hợp lệ", null);
            return;
        }
        
        if (amount <= 0) {
            callback.onPaymentResult(false, "Số tiền không hợp lệ", null);
            return;
        }
        
        // Create payment request
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("transactionId", transactionId);
        paymentRequest.put("amount", amount);
        paymentRequest.put("paymentMethod", paymentMethod);
        paymentRequest.put("recipientBank", recipientBank);
        paymentRequest.put("status", "pending");
        paymentRequest.put("createdAt", com.google.firebase.Timestamp.now());
        
        // Store payment request - Cloud Function will process it
        db.collection("payment_requests")
                .document(transactionId)
                .set(paymentRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment request created for transaction: " + transactionId);
                    
                    // For now, simulate payment processing
                    // In production, this should be handled by Cloud Function
                    // which calls VNPay/Stripe API
                    processPaymentGateway(transactionId, amount, paymentMethod, recipientBank, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create payment request", e);
                    callback.onPaymentResult(false, "Lỗi tạo yêu cầu thanh toán: " + e.getMessage(), null);
                });
    }
    
    /**
     * Process payment through payment gateway (VNPay or Stripe)
     * In production, this should be done via Cloud Function
     */
    private void processPaymentGateway(String transactionId, double amount, String paymentMethod,
                                      String recipientBank, PaymentCallback callback) {
        
        if (VNPAY_PAYMENT_METHOD.equals(paymentMethod)) {
            processVNPayPayment(transactionId, amount, recipientBank, callback);
        } else if (STRIPE_PAYMENT_METHOD.equals(paymentMethod)) {
            processStripePayment(transactionId, amount, recipientBank, callback);
        } else {
            callback.onPaymentResult(false, "Phương thức thanh toán không được hỗ trợ", null);
        }
    }
    
    /**
     * Process VNPay payment
     * Note: VNPay integration should be done via Cloud Function to avoid 403 errors
     * This method creates payment request that Cloud Function will process
     */
    private void processVNPayPayment(String transactionId, double amount, String recipientBank,
                                     PaymentCallback callback) {
        Log.d(TAG, "Creating VNPay payment request for transaction: " + transactionId);
        
        // VNPay payment should be processed via Cloud Function to avoid 403 Forbidden
        // The Cloud Function will have proper authentication and IP whitelisting
        // For now, mark as pending and let Cloud Function handle it
        
        Map<String, Object> update = new HashMap<>();
        update.put("status", "processing");
        update.put("paymentGateway", "VNPay");
        update.put("requestedAt", com.google.firebase.Timestamp.now());
        update.put("error", null); // Clear any previous errors
        
        db.collection("payment_requests")
                .document(transactionId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "VNPay payment request created, waiting for Cloud Function to process");
                    
                    // For development/testing: simulate success after delay
                    // In production, Cloud Function will process and update status
                    new android.os.Handler().postDelayed(() -> {
                        // Check if Cloud Function has processed the payment
                        checkPaymentStatus(transactionId, new PaymentStatusCallback() {
                            @Override
                            public void onStatusChecked(boolean isCompleted, String message, 
                                                       Map<String, Object> paymentData) {
                                if (isCompleted) {
                                    callback.onPaymentResult(true, "Thanh toán VNPay thành công", paymentData);
                                } else {
                                    // If still processing, show message
                                    callback.onPaymentResult(false, 
                                        "Đang xử lý thanh toán. Vui lòng đợi...", null);
                                }
                            }
                        });
                    }, 2000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create VNPay payment request", e);
                    callback.onPaymentResult(false, 
                        "Lỗi tạo yêu cầu thanh toán VNPay: " + e.getMessage(), null);
                });
    }
    
    /**
     * Process Stripe payment
     * Note: This is a placeholder. In production, use Stripe SDK or API
     */
    private void processStripePayment(String transactionId, double amount, String recipientBank,
                                     PaymentCallback callback) {
        // TODO: Integrate with Stripe API
        // For now, simulate successful payment
        Log.d(TAG, "Processing Stripe payment for transaction: " + transactionId);
        
        // Update payment request status
        Map<String, Object> update = new HashMap<>();
        update.put("status", "completed");
        update.put("completedAt", com.google.firebase.Timestamp.now());
        update.put("paymentGateway", "Stripe");
        update.put("paymentReference", "STRIPE_" + System.currentTimeMillis());
        
        db.collection("payment_requests")
                .document(transactionId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("paymentMethod", STRIPE_PAYMENT_METHOD);
                    paymentData.put("paymentReference", update.get("paymentReference"));
                    paymentData.put("gateway", "Stripe");
                    
                    callback.onPaymentResult(true, "Thanh toán Stripe thành công", paymentData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Stripe payment status", e);
                    callback.onPaymentResult(false, "Lỗi cập nhật trạng thái thanh toán", null);
                });
    }
    
    /**
     * Check payment status
     */
    public void checkPaymentStatus(String transactionId, PaymentStatusCallback callback) {
        db.collection("payment_requests")
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onStatusChecked(false, "Không tìm thấy yêu cầu thanh toán", null);
                        return;
                    }
                    
                    String status = documentSnapshot.getString("status");
                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("status", status);
                    paymentData.put("paymentMethod", documentSnapshot.getString("paymentMethod"));
                    paymentData.put("paymentReference", documentSnapshot.getString("paymentReference"));
                    
                    boolean isCompleted = "completed".equals(status);
                    callback.onStatusChecked(isCompleted, 
                        isCompleted ? "Thanh toán đã hoàn tất" : "Thanh toán đang xử lý", 
                        paymentData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check payment status", e);
                    callback.onStatusChecked(false, "Lỗi kiểm tra trạng thái thanh toán", null);
                });
    }
    
    /**
     * Callback interfaces
     */
    public interface PaymentCallback {
        void onPaymentResult(boolean success, String message, Map<String, Object> paymentData);
    }
    
    public interface PaymentStatusCallback {
        void onStatusChecked(boolean isCompleted, String message, Map<String, Object> paymentData);
    }
}

