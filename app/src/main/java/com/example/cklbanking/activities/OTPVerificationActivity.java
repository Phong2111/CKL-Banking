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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

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

    // Data
    private String transactionId;
    private double amount;
    private String recipient;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 120000; // 2 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get intent data
        transactionId = getIntent().getStringExtra("transaction_id");
        amount = getIntent().getDoubleExtra("amount", 0);
        recipient = getIntent().getStringExtra("recipient");

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

    private void verifyOTP() {
        String otpCode = getOTPCode();
        
        if (otpCode.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ mã OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // TODO: Verify OTP with backend
        // For now, accept any 6-digit code
        new android.os.Handler().postDelayed(() -> {
            showLoading(false);
            
            if (otpCode.equals("123456") || otpCode.length() == 6) {
                // OTP correct
                updateTransactionStatus("completed");
            } else {
                Toast.makeText(this, "Mã OTP không đúng", Toast.LENGTH_SHORT).show();
            }
        }, 1500);
    }

    private void updateTransactionStatus(String status) {
        if (transactionId == null) {
            Toast.makeText(this, "Giao dịch thành công!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("transactions")
                .document(transactionId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resendOTP() {
        // TODO: Request new OTP from backend
        timeLeftInMillis = 120000;
        startTimer();
        btnResendOtp.setEnabled(false);
        btnVerifyOtp.setEnabled(true);
        clearOTP();
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
