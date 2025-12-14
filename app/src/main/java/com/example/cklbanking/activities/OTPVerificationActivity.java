package com.example.cklbanking.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.services.OTPService;
import com.example.cklbanking.services.PaymentService;
import com.example.cklbanking.services.TransactionService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;

public class OTPVerificationActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText otp1, otp2, otp3, otp4, otp5, otp6;
    private TextView otpTimer, transactionAmount, transactionRecipient, btnResendOtp;
    private MaterialButton btnVerifyOtp;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Services
    private OTPService otpService;
    private PaymentService paymentService;
    private TransactionService transactionService;

    // Data
    private String transactionId;
    private String userId;
    private double amount;
    private String recipient;
    private String userEmail;
    private String transferType;
    private String fromAccountId;
    private String toAccountId;
    private String recipientBank;
    private String description;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 120000; // 2 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        otpService = new OTPService();
        paymentService = new PaymentService();
        transactionService = new TransactionService();

        // Get intent data
        transactionId = getIntent().getStringExtra("transaction_id");
        amount = getIntent().getDoubleExtra("amount", 0);
        recipient = getIntent().getStringExtra("recipient");
        transferType = getIntent().getStringExtra("transfer_type");
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        // Load transaction details
        loadTransactionDetails();

        // Load user email
        loadUserEmail();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Listeners
        setupListeners();

        // Update transaction info
        updateTransactionInfo();

        // Start timer
        startTimer();
        
        // Load OTP for testing (hiển thị OTP ngay trong app)
        loadOTPForTesting();
        
        // Kiểm tra trạng thái email
        checkEmailStatus();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        otpTimer = findViewById(R.id.otpTimer);
        transactionAmount = findViewById(R.id.transactionAmount);
        transactionRecipient = findViewById(R.id.transactionRecipient);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResendOtp = findViewById(R.id.btnResendOtp);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        // Auto-focus to next OTP field
        otp1.addTextChangedListener(new OTPTextWatcher(otp1, otp2));
        otp2.addTextChangedListener(new OTPTextWatcher(otp2, otp3));
        otp3.addTextChangedListener(new OTPTextWatcher(otp3, otp4));
        otp4.addTextChangedListener(new OTPTextWatcher(otp4, otp5));
        otp5.addTextChangedListener(new OTPTextWatcher(otp5, otp6));
        otp6.addTextChangedListener(new OTPTextWatcher(otp6, null));

        btnVerifyOtp.setOnClickListener(v -> verifyOTP());
        btnResendOtp.setOnClickListener(v -> resendOTP());
    }

    private void updateTransactionInfo() {
        if (amount > 0) {
            transactionAmount.setText(formatCurrency(amount));
        }
        if (recipient != null) {
            transactionRecipient.setText(recipient);
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                otpTimer.setText("00:00");
                btnVerifyOtp.setEnabled(false);
                btnResendOtp.setEnabled(true);
                Toast.makeText(OTPVerificationActivity.this, 
                    "Mã OTP đã hết hạn", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        otpTimer.setText(timeFormatted);
    }

    private void loadUserEmail() {
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userEmail = documentSnapshot.getString("email");
                        // Fallback to Firebase Auth email
                        if (userEmail == null || userEmail.isEmpty()) {
                            userEmail = mAuth.getCurrentUser() != null ? 
                                mAuth.getCurrentUser().getEmail() : null;
                        }
                    } else {
                        // Fallback to Firebase Auth email
                        userEmail = mAuth.getCurrentUser() != null ? 
                            mAuth.getCurrentUser().getEmail() : null;
                    }
                    
                    // Update OTP message with email
                    if (userEmail != null && !userEmail.isEmpty()) {
                        TextView otpMessage = findViewById(R.id.otpMessage);
                        if (otpMessage != null) {
                            // Mask email for privacy
                            String maskedEmail = maskEmail(userEmail);
                            otpMessage.setText("Mã OTP đã được gửi đến email\n" + maskedEmail);
                        }
                    }
                });
    }
    
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) return "***";
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return "***" + domain;
        }
        
        String masked = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        return masked + domain;
    }
    
    // CHỈ DÙNG CHO TEST - XÓA TRONG PRODUCTION
    private void loadOTPForTesting() {
        if (transactionId == null) return;
        
        // Đợi 1 giây để OTP được tạo xong
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            db.collection("otps")
                    .document(transactionId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String otpCode = documentSnapshot.getString("otpCode");
                            String status = documentSnapshot.getString("status");
                            
                            android.util.Log.d("OTPVerification", "===========================================");
                            android.util.Log.d("OTPVerification", "OTP CODE FOR TESTING: " + otpCode);
                            android.util.Log.d("OTPVerification", "OTP Status: " + status);
                            android.util.Log.d("OTPVerification", "===========================================");
                            
                            // Hiển thị OTP trong AlertDialog (CHỈ CHO DEVELOPMENT)
                            if (otpCode != null) {
                                showOTPDialog(otpCode, status);
                            }
                        } else {
                            // Nếu chưa có OTP, thử lại sau 2 giây
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                loadOTPForTesting();
                            }, 2000);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("OTPVerification", "Error loading OTP", e);
                    });
        }, 1000);
    }
    
    private void showOTPDialog(String otpCode, String status) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🔐 Mã OTP (Chế độ Test)");
        builder.setMessage(
            "Mã OTP của bạn là:\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n" +
            "      " + otpCode + "\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Trạng thái email: " + (status != null ? status : "pending") + "\n\n" +
            "⚠️ Lưu ý: Đây là chế độ test.\n" +
            "Trong production, mã OTP sẽ được gửi qua email."
        );
        builder.setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }
    
    private void checkEmailStatus() {
        if (transactionId == null) return;
        
        // Kiểm tra trạng thái email request
        db.collection("email_requests")
                .document(transactionId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("OTPVerification", "Error listening to email status", e);
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        String error = snapshot.getString("error");
                        
                        if ("sent".equals(status)) {
                            android.util.Log.d("OTPVerification", "✅ Email đã được gửi thành công");
                        } else if ("failed".equals(status)) {
                            android.util.Log.e("OTPVerification", "❌ Email gửi thất bại: " + error);
                            runOnUiThread(() -> {
                                Toast.makeText(this, 
                                    "Email gửi thất bại. Vui lòng kiểm tra OTP trong dialog hoặc Logcat.", 
                                    Toast.LENGTH_LONG).show();
                            });
                        } else {
                            android.util.Log.d("OTPVerification", "⏳ Email đang được xử lý...");
                        }
                    }
                });
    }

    private void verifyOTP() {
        String otpCode = getOTPCode();
        
        if (otpCode.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ mã OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (transactionId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin giao dịch", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Verify OTP using OTPService
        otpService.verifyOTP(transactionId, otpCode, new OTPService.OTPVerificationCallback() {
            @Override
            public void onVerificationResult(boolean success, String message) {
                runOnUiThread(() -> {
            showLoading(false);
            
                    if (success) {
                        // OTP verified, now process payment and persist transaction
                        processTransactionAfterOTP();
            } else {
                        Toast.makeText(OTPVerificationActivity.this, message, 
                            Toast.LENGTH_SHORT).show();
            }
                });
            }
        });
    }

    private void loadTransactionDetails() {
        if (transactionId == null) return;

        db.collection("transactions")
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        fromAccountId = documentSnapshot.getString("fromAccountId");
                        toAccountId = documentSnapshot.getString("toAccountId");
                        transferType = documentSnapshot.getString("transferType");
                        recipientBank = documentSnapshot.getString("recipientBank");
                        description = documentSnapshot.getString("description");
                    }
                });
    }
    
    private void processTransactionAfterOTP() {
        showLoading(true);
        
        // For external transfers, process payment first
        if ("external".equals(transferType) && recipientBank != null) {
            // Process payment via payment gateway
            paymentService.processPayment(transactionId, amount, 
                PaymentService.VNPAY_PAYMENT_METHOD, recipientBank,
                new PaymentService.PaymentCallback() {
                    @Override
                    public void onPaymentResult(boolean success, String message, 
                                              Map<String, Object> paymentData) {
                        runOnUiThread(() -> {
                            if (!success) {
                                showLoading(false);
                                Toast.makeText(OTPVerificationActivity.this, 
                                    "Lỗi thanh toán: " + message, Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            // Payment successful, now persist transaction
                            persistTransaction();
                        });
                    }
                });
        } else {
            // Internal transfer - no payment needed, just persist
            persistTransaction();
        }
    }
    
    private void persistTransaction() {
        // Use TransactionService to persist transaction and update balances
        transactionService.persistTransaction(
            transactionId,
            fromAccountId,
            toAccountId,
            amount,
            "transfer",
            transferType != null ? transferType : "internal",
            recipient != null ? recipient : "Unknown",
            description != null ? description : "",
            recipientBank,
            new TransactionService.TransactionPersistenceCallback() {
                @Override
                public void onPersistenceResult(boolean success, String message) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (success) {
                            Toast.makeText(OTPVerificationActivity.this, 
                                "Giao dịch thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(OTPVerificationActivity.this, 
                                "Lỗi: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        );
    }

    private void resendOTP() {
        if (transactionId == null || userId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin giao dịch", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy email. Vui lòng cập nhật email trong hồ sơ.", 
                Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        btnResendOtp.setEnabled(false);

        // Resend OTP using OTPService (via email)
        String newOTP = otpService.resendOTP(transactionId, userId, userEmail);
        
        // Reset timer
        timeLeftInMillis = 120000;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        startTimer();
        
        btnResendOtp.setEnabled(false);
        btnVerifyOtp.setEnabled(true);
        clearOTP();
        
        showLoading(false);
        Toast.makeText(this, "Đã gửi lại mã OTP", Toast.LENGTH_SHORT).show();
    }

    private String getOTPCode() {
        return otp1.getText().toString() +
               otp2.getText().toString() +
               otp3.getText().toString() +
               otp4.getText().toString() +
               otp5.getText().toString() +
               otp6.getText().toString();
    }

    private void clearOTP() {
        otp1.setText("");
        otp2.setText("");
        otp3.setText("");
        otp4.setText("");
        otp5.setText("");
        otp6.setText("");
        otp1.requestFocus();
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f ₫", amount);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnVerifyOtp.setEnabled(!show);
    }

    // TextWatcher for auto-focus
    private class OTPTextWatcher implements TextWatcher {
        private final TextInputEditText currentView;
        private final TextInputEditText nextView;

        public OTPTextWatcher(TextInputEditText currentView, TextInputEditText nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
