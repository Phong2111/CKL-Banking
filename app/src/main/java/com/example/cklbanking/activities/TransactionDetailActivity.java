package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Transaction;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionDetailActivity extends AppCompatActivity {

    private ImageView statusIcon;
    private TextView statusText, transactionAmount, transactionType;
    private TextView fromAccount, toAccount, recipientName;
    private TextView transactionDateTime, transactionId;
    private TextView transactionFee, transactionMessage;
    private MaterialButton btnShare, btnDownload;
    
    private Transaction transaction;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        initViews();
        setupToolbar();
        loadTransactionData();
        setupButtons();
    }

    private void initViews() {
        statusIcon = findViewById(R.id.statusIcon);
        statusText = findViewById(R.id.statusText);
        transactionAmount = findViewById(R.id.transactionAmount);
        transactionType = findViewById(R.id.transactionType);
        
        fromAccount = findViewById(R.id.fromAccount);
        toAccount = findViewById(R.id.toAccount);
        recipientName = findViewById(R.id.recipientName);
        
        transactionDateTime = findViewById(R.id.transactionDateTime);
        transactionId = findViewById(R.id.transactionId);
        
        transactionFee = findViewById(R.id.transactionFee);
        transactionMessage = findViewById(R.id.transactionMessage);
        
        btnShare = findViewById(R.id.btnShare);
        btnDownload = findViewById(R.id.btnDownload);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadTransactionData() {
        // Get transaction data from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("transaction_id")) {
            String transactionId = intent.getStringExtra("transaction_id");
            // TODO: Load transaction from Firestore using transaction ID
            loadMockData();
        } else {
            loadMockData();
        }
    }

    private void loadMockData() {
        // Mock data for demonstration
        statusIcon.setImageResource(R.drawable.ic_success);
        statusText.setText("Giao dịch thành công");
        
        transactionAmount.setText("-500,000 ₫");
        transactionType.setText("Chuyển khoản");
        
        fromAccount.setText("1234567890");
        toAccount.setText("0987654321");
        recipientName.setText("NGUYEN VAN A");
        
        transactionDateTime.setText("21/11/2025 14:30:25");
        transactionId.setText("TXN202511210001");
        
        transactionFee.setText("0 ₫");
        transactionMessage.setText("Chuyển tiền cho bạn");
    }

    private void setupButtons() {
        btnShare.setOnClickListener(v -> shareTransaction());
        btnDownload.setOnClickListener(v -> downloadReceipt());
    }

    private void shareTransaction() {
        String shareText = "Chi tiết giao dịch\n" +
                "Mã GD: " + transactionId.getText() +
                "\nSố tiền: " + transactionAmount.getText() +
                "\nTrạng thái: " + statusText.getText() +
                "\nThời gian: " + transactionDateTime.getText();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }

    private void downloadReceipt() {
        // TODO: Implement PDF generation and download
        // Show toast for now
        android.widget.Toast.makeText(this, "Chức năng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
    }
}
