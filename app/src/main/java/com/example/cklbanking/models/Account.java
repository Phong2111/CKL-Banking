package com.example.cklbanking.models;// Thay bằng package của bạn

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Account {
    private String accountId; // Document ID
    private String userId; // Chủ sở hữu
    private String accountNumber;
    private String accountType; // 'checking', 'saving', 'mortgage'
    private double balance;

    // Dành cho 'saving'
    private double interestRate;

    // Dành cho 'mortgage'
    private double monthlyPayment;

    @ServerTimestamp
    private Date createdAt;

    // Constructor rỗng - BẮT BUỘC cho Firestore
    public Account() {}

    // --- Bổ sung Getter và Setter ---
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // ...Tương tự cho các trường còn lại...


}