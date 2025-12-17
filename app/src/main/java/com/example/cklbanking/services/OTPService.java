package com.example.cklbanking.services;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OTPService {
    private static final String TAG = "OTPService";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_TIME = 120000; // 2 minutes in milliseconds
    private static final String OTP_COLLECTION = "otps";
    private static final String OTP_ATTEMPTS_COLLECTION = "otp_attempts";
    private static final int MAX_OTP_ATTEMPTS_PER_HOUR = 5; // Giới hạn 5 lần gửi OTP trong 1 giờ
    private static final long ONE_HOUR_IN_MILLIS = 3600000; // 1 giờ = 3600000 milliseconds
    private static final int MAX_FAILED_ATTEMPTS = 3; // Sau 3 lần sai → lock
    private static final long LOCK_DURATION_MILLIS = 900000; // 15 phút = 900000 milliseconds
    
    private FirebaseFirestore db;
    
    public OTPService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Generate OTP with rate limiting check
     */
    public void generateOTP(String transactionId, String userId, String userEmail, OTPGenerationCallback callback) {
        // Check rate limiting first
        checkRateLimit(userId, new RateLimitCallback() {
            @Override
            public void onRateLimitChecked(boolean allowed, String message) {
                if (!allowed) {
                    callback.onOTPGenerated(null, false, message);
                    return;
                }
                
                // Rate limit OK, generate OTP
                Random random = new Random();
                int otp = 100000 + random.nextInt(900000); // 100000 to 999999
                String otpCode = String.valueOf(otp);
                
                // Store OTP in Firestore
                Map<String, Object> otpData = new HashMap<>();
                otpData.put("transactionId", transactionId);
                otpData.put("userId", userId);
                otpData.put("otpCode", otpCode);
                otpData.put("createdAt", Timestamp.now());
                otpData.put("expiresAt", Timestamp.now().toDate().getTime() + OTP_EXPIRY_TIME);
                otpData.put("isUsed", false);
                otpData.put("userEmail", userEmail);
                otpData.put("status", "pending"); // pending, sent, failed
                
                // Use transactionId as document ID for easy lookup
                db.collection(OTP_COLLECTION)
                        .document(transactionId)
                        .set(otpData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "OTP generated and stored for transaction: " + transactionId);
                            
                            // Record OTP attempt for rate limiting
                            recordOTPAttempt(userId, transactionId);
                            
                            // Trigger email sending via Cloud Function
                            sendOTPEmail(transactionId, userEmail, otpCode);
                            
                            callback.onOTPGenerated(otpCode, true, "OTP đã được gửi");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to store OTP", e);
                            callback.onOTPGenerated(null, false, "Lỗi tạo OTP: " + e.getMessage());
                        });
            }
        });
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public String generateOTP(String transactionId, String userId, String userEmail) {
        // This method is deprecated but kept for backward compatibility
        // New code should use generateOTP with callback
        generateOTP(transactionId, userId, userEmail, new OTPGenerationCallback() {
            @Override
            public void onOTPGenerated(String otpCode, boolean success, String message) {
                // This is async, so the return value won't be available immediately
                // This method should not be used in new code
            }
        });
        return ""; // Return empty string as this is async
    }
    
    /**
     * Check rate limiting: giới hạn số lần gửi OTP trong 1 giờ
     */
    private void checkRateLimit(String userId, RateLimitCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onRateLimitChecked(false, "User ID không hợp lệ");
            return;
        }
        
        long oneHourAgo = System.currentTimeMillis() - ONE_HOUR_IN_MILLIS;
        Timestamp oneHourAgoTimestamp = new Timestamp(oneHourAgo / 1000, (int) ((oneHourAgo % 1000) * 1000000));
        
        db.collection(OTP_ATTEMPTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThan("createdAt", oneHourAgoTimestamp)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int attemptCount = querySnapshot.size();
                    
                    if (attemptCount >= MAX_OTP_ATTEMPTS_PER_HOUR) {
                        long nextAllowedTime = oneHourAgo + ONE_HOUR_IN_MILLIS;
                        long waitMinutes = (nextAllowedTime - System.currentTimeMillis()) / 60000;
                        callback.onRateLimitChecked(false, 
                            "Bạn đã gửi quá nhiều mã OTP. Vui lòng đợi " + waitMinutes + " phút nữa.");
                    } else {
                        callback.onRateLimitChecked(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking rate limit", e);
                    // Allow OTP generation if rate limit check fails (fail open)
                    callback.onRateLimitChecked(true, null);
                });
    }
    
    /**
     * Record OTP attempt for rate limiting
     */
    private void recordOTPAttempt(String userId, String transactionId) {
        Map<String, Object> attemptData = new HashMap<>();
        attemptData.put("userId", userId);
        attemptData.put("transactionId", transactionId);
        attemptData.put("createdAt", Timestamp.now());
        
        db.collection(OTP_ATTEMPTS_COLLECTION)
                .add(attemptData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "OTP attempt recorded: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to record OTP attempt", e);
                });
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
    
    public void verifyOTP(String transactionId, String otpCode, String userId, OTPVerificationCallback callback) {
        if (transactionId == null || otpCode == null || otpCode.length() != OTP_LENGTH) {
            callback.onVerificationResult(false, "Mã OTP không hợp lệ", false);
            return;
        }
        
        // Check if account is locked
        checkAccountLock(userId, new AccountLockCallback() {
            @Override
            public void onLockChecked(boolean isLocked, String message) {
                if (isLocked) {
                    callback.onVerificationResult(false, message, false);
                    return;
                }
                
                // Account not locked, proceed with OTP verification
                db.collection(OTP_COLLECTION)
                        .document(transactionId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (!documentSnapshot.exists()) {
                                handleFailedAttempt(userId, callback, "Không tìm thấy mã OTP");
                                return;
                            }
                            
                            String storedOTP = documentSnapshot.getString("otpCode");
                            boolean isUsed = Boolean.TRUE.equals(documentSnapshot.getBoolean("isUsed"));
                            long expiresAt = documentSnapshot.getLong("expiresAt");
                            long currentTime = System.currentTimeMillis();
                            
                            // Check if OTP is already used
                            if (isUsed) {
                                callback.onVerificationResult(false, "Mã OTP đã được sử dụng", false);
                                return;
                            }
                            
                            // Check if OTP is expired
                            if (currentTime > expiresAt) {
                                callback.onVerificationResult(false, "Mã OTP đã hết hạn", false);
                                return;
                            }
                            
                            // Verify OTP code
                            if (otpCode.equals(storedOTP)) {
                                // OTP correct - reset failed attempts and mark OTP as used
                                resetFailedAttempts(userId);
                                documentSnapshot.getReference().update("isUsed", true)
                                        .addOnSuccessListener(aVoid -> {
                                            callback.onVerificationResult(true, "Xác thực thành công", false);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to mark OTP as used", e);
                                            callback.onVerificationResult(true, "Xác thực thành công", false);
                                        });
                            } else {
                                // OTP incorrect - increment failed attempts
                                handleFailedAttempt(userId, callback, "Mã OTP không đúng");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to verify OTP", e);
                            callback.onVerificationResult(false, "Lỗi xác thực: " + e.getMessage(), false);
                        });
            }
        });
    }
    
    /**
     * Check if user account is locked
     */
    private void checkAccountLock(String userId, AccountLockCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onLockChecked(false, null);
            return;
        }
        
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onLockChecked(false, null);
                        return;
                    }
                    
                    Object lockedUntilObj = documentSnapshot.get("lockedUntil");
                    if (lockedUntilObj == null) {
                        callback.onLockChecked(false, null);
                        return;
                    }
                    
                    long lockedUntilMillis;
                    if (lockedUntilObj instanceof Timestamp) {
                        lockedUntilMillis = ((Timestamp) lockedUntilObj).toDate().getTime();
                    } else if (lockedUntilObj instanceof Long) {
                        lockedUntilMillis = (Long) lockedUntilObj;
                    } else {
                        callback.onLockChecked(false, null);
                        return;
                    }
                    
                    long currentTime = System.currentTimeMillis();
                    if (currentTime < lockedUntilMillis) {
                        // Account is still locked
                        long remainingMinutes = (lockedUntilMillis - currentTime) / 60000;
                        callback.onLockChecked(true, 
                            "Tài khoản đã bị khóa do nhập sai OTP nhiều lần. Vui lòng đợi " + 
                            remainingMinutes + " phút nữa.");
                    } else {
                        // Lock expired, unlock account
                        unlockAccount(userId);
                        callback.onLockChecked(false, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking account lock", e);
                    // Fail open - allow verification if check fails
                    callback.onLockChecked(false, null);
                });
    }
    
    /**
     * Handle failed OTP attempt - increment counter and lock if needed
     */
    private void handleFailedAttempt(String userId, OTPVerificationCallback callback, String errorMessage) {
        if (userId == null || userId.isEmpty()) {
            callback.onVerificationResult(false, errorMessage, false);
            return;
        }
        
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    int currentAttempts = 0;
                    if (documentSnapshot.exists()) {
                        Object attemptsObj = documentSnapshot.get("failedAttempts");
                        if (attemptsObj instanceof Integer) {
                            currentAttempts = (Integer) attemptsObj;
                        } else if (attemptsObj instanceof Long) {
                            currentAttempts = ((Long) attemptsObj).intValue();
                        }
                    }
                    
                    int newAttempts = currentAttempts + 1;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("failedAttempts", newAttempts);
                    
                    boolean shouldLock = newAttempts >= MAX_FAILED_ATTEMPTS;
                    if (shouldLock) {
                        long lockUntil = System.currentTimeMillis() + LOCK_DURATION_MILLIS;
                        Timestamp lockUntilTimestamp = new Timestamp(lockUntil / 1000, (int) ((lockUntil % 1000) * 1000000));
                        updates.put("lockedUntil", lockUntilTimestamp);
                    }
                    
                    db.collection("users")
                            .document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                if (shouldLock) {
                                    callback.onVerificationResult(false, 
                                        "Bạn đã nhập sai OTP " + MAX_FAILED_ATTEMPTS + " lần. " +
                                        "Tài khoản đã bị khóa trong 15 phút.", true);
                                } else {
                                    int remainingAttempts = MAX_FAILED_ATTEMPTS - newAttempts;
                                    callback.onVerificationResult(false, 
                                        errorMessage + " (Còn " + remainingAttempts + " lần thử)", false);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update failed attempts", e);
                                callback.onVerificationResult(false, errorMessage, false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error handling failed attempt", e);
                    callback.onVerificationResult(false, errorMessage, false);
                });
    }
    
    /**
     * Reset failed attempts after successful OTP verification
     */
    private void resetFailedAttempts(String userId) {
        if (userId == null || userId.isEmpty()) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("failedAttempts", 0);
        updates.put("lockedUntil", null);
        
        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Failed attempts reset for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reset failed attempts", e);
                });
    }
    
    /**
     * Unlock account when lock period expires
     */
    private void unlockAccount(String userId) {
        if (userId == null || userId.isEmpty()) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("lockedUntil", null);
        updates.put("failedAttempts", 0);
        
        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Account unlocked for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to unlock account", e);
                });
    }
    
    public void resendOTP(String transactionId, String userId, String userEmail, OTPGenerationCallback callback) {
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
                                // Generate new OTP with rate limiting
                                generateOTP(transactionId, userId, email, callback);
                            } else {
                                callback.onOTPGenerated(null, false, "Không tìm thấy email");
                            }
                        } else {
                            callback.onOTPGenerated(null, false, "Không tìm thấy OTP cũ");
                        }
                    });
            return;
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
        
        // Generate new OTP with rate limiting
        generateOTP(transactionId, userId, userEmail, callback);
    }
    
    /**
     * Callback interface for OTP verification
     */
    public interface OTPVerificationCallback {
        void onVerificationResult(boolean success, String message, boolean isLocked);
    }
    
    /**
     * Callback interface for OTP generation
     */
    public interface OTPGenerationCallback {
        void onOTPGenerated(String otpCode, boolean success, String message);
    }
    
    /**
     * Callback interface for rate limit check
     */
    private interface RateLimitCallback {
        void onRateLimitChecked(boolean allowed, String message);
    }
    
    /**
     * Callback interface for account lock check
     */
    private interface AccountLockCallback {
        void onLockChecked(boolean isLocked, String message);
    }
}

