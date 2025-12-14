package com.example.cklbanking.services;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OTPService {
    private static final String TAG = "OTPService";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_TIME = 120000; // 2 minutes in milliseconds
    private static final String OTP_COLLECTION = "otps";
    
    private FirebaseFirestore db;
    
    public OTPService() {
        db = FirebaseFirestore.getInstance();
    }

    public String generateOTP(String transactionId, String userId, String userEmail) {
        // Generate 6-digit OTP
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 100000 to 999999
        String otpCode = String.valueOf(otp);
        
        // Store OTP in Firestore
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("transactionId", transactionId);
        otpData.put("userId", userId);
        otpData.put("otpCode", otpCode);
        otpData.put("createdAt", com.google.firebase.Timestamp.now());
        otpData.put("expiresAt", com.google.firebase.Timestamp.now().toDate().getTime() + OTP_EXPIRY_TIME);
        otpData.put("isUsed", false);
        otpData.put("userEmail", userEmail);
        otpData.put("status", "pending"); // pending, sent, failed
        
        // Use transactionId as document ID for easy lookup
        db.collection(OTP_COLLECTION)
                .document(transactionId)
                .set(otpData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "OTP generated and stored for transaction: " + transactionId);
                    // Trigger email sending via Cloud Function
                    sendOTPEmail(transactionId, userEmail, otpCode);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to store OTP", e);
                });
        
        return otpCode;
    }
    
    /**
     * Send OTP via email using Firestore trigger (Cloud Function will handle email sending)
     * Alternative: Can use HTTP call to email service API
     */
    private void sendOTPEmail(String transactionId, String userEmail, String otpCode) {
        if (userEmail == null || userEmail.isEmpty()) {
            Log.w(TAG, "User email is empty, cannot send OTP email");
            return;
        }
        
        // Log OTP để test (CHỈ DÙNG CHO DEVELOPMENT - XÓA TRONG PRODUCTION)
        Log.d(TAG, "===========================================");
        Log.d(TAG, "OTP CODE FOR TESTING: " + otpCode);
        Log.d(TAG, "Email: " + userEmail);
        Log.d(TAG, "Transaction ID: " + transactionId);
        Log.d(TAG, "===========================================");
        
        // Create email request document in Firestore
        // Cloud Function will listen to this collection and send email
        Map<String, Object> emailRequest = new HashMap<>();
        emailRequest.put("transactionId", transactionId);
        emailRequest.put("toEmail", userEmail);
        emailRequest.put("otpCode", otpCode);
        emailRequest.put("subject", "Mã OTP xác thực giao dịch - CKL Banking");
        emailRequest.put("createdAt", com.google.firebase.Timestamp.now());
        emailRequest.put("status", "pending");
        
        // Store in email_requests collection for Cloud Function to process
        db.collection("email_requests")
                .document(transactionId)
                .set(emailRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Email request created for transaction: " + transactionId);
                    Log.d(TAG, "Cloud Function will send email to: " + userEmail);
                    // Update OTP document status to pending (will be updated by Cloud Function)
                    db.collection(OTP_COLLECTION)
                            .document(transactionId)
                            .update("status", "pending")
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "OTP status updated to pending, waiting for Cloud Function");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create email request", e);
                    // Update OTP document status to failed
                    db.collection(OTP_COLLECTION)
                            .document(transactionId)
                            .update("status", "failed")
                            .addOnFailureListener(e2 -> Log.e(TAG, "Failed to update OTP status", e2));
                });
    }
    
    public void verifyOTP(String transactionId, String otpCode, OTPVerificationCallback callback) {
        if (transactionId == null || otpCode == null || otpCode.length() != OTP_LENGTH) {
            callback.onVerificationResult(false, "Mã OTP không hợp lệ");
            return;
        }
        
        db.collection(OTP_COLLECTION)
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onVerificationResult(false, "Không tìm thấy mã OTP");
                        return;
                    }
                    
                    String storedOTP = documentSnapshot.getString("otpCode");
                    boolean isUsed = Boolean.TRUE.equals(documentSnapshot.getBoolean("isUsed"));
                    long expiresAt = documentSnapshot.getLong("expiresAt");
                    long currentTime = System.currentTimeMillis();
                    
                    // Check if OTP is already used
                    if (isUsed) {
                        callback.onVerificationResult(false, "Mã OTP đã được sử dụng");
                        return;
                    }
                    
                    // Check if OTP is expired
                    if (currentTime > expiresAt) {
                        callback.onVerificationResult(false, "Mã OTP đã hết hạn");
                        return;
                    }
                    
                    // Verify OTP code
                    if (otpCode.equals(storedOTP)) {
                        // Mark OTP as used
                        documentSnapshot.getReference().update("isUsed", true)
                                .addOnSuccessListener(aVoid -> {
                                    callback.onVerificationResult(true, "Xác thực thành công");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to mark OTP as used", e);
                                    callback.onVerificationResult(true, "Xác thực thành công");
                                });
                    } else {
                        callback.onVerificationResult(false, "Mã OTP không đúng");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to verify OTP", e);
                    callback.onVerificationResult(false, "Lỗi xác thực: " + e.getMessage());
                });
    }
    
    public String resendOTP(String transactionId, String userId, String userEmail) {
        // Get current OTP to get email if not provided
        if (userEmail == null || userEmail.isEmpty()) {
            db.collection(OTP_COLLECTION)
                    .document(transactionId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("userEmail");
                            if (email != null) {
                                // Delete old OTP and email request
                                db.collection(OTP_COLLECTION).document(transactionId).delete();
                                db.collection("email_requests").document(transactionId).delete();
                                // Generate new OTP
                                generateOTP(transactionId, userId, email);
                            }
                        }
                    });
            return "";
        }
        
        // Delete old OTP and email request
        db.collection(OTP_COLLECTION)
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old OTP deleted");
                });
        db.collection("email_requests")
                .document(transactionId)
                .delete();
        
        // Generate new OTP
        return generateOTP(transactionId, userId, userEmail);
    }
    
    /**
     * Callback interface for OTP verification
     */
    public interface OTPVerificationCallback {
        void onVerificationResult(boolean success, String message);
    }
}

