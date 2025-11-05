package com.example.cklbanking.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Transaction {
    private String transactionId; // Document ID
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private String type; // 'transfer', 'deposit', 'withdraw', 'bill_payment'
    private String status; // 'completed', 'pending', 'failed'

    @ServerTimestamp
    private Date timestamp;

    // Constructor rỗng - BẮT BUỘC cho Firestore
    public Transaction() {}

    // --- Bổ sung Getter và Setter ---
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }

    public String getToAccountId() { return toAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}