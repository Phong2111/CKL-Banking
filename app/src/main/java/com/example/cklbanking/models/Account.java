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
    private int term; // Kỳ hạn (số tháng)
    private Date openDate; // Ngày mở tài khoản
    private Date maturityDate; // Ngày đáo hạn

    // Dành cho 'mortgage'
    private double monthlyPayment;
    private double biweeklyPayment; // Số tiền trả mỗi 2 tuần
    private double remainingBalance; // Số dư còn lại

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

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public double getBiweeklyPayment() { return biweeklyPayment; }
    public void setBiweeklyPayment(double biweeklyPayment) { this.biweeklyPayment = biweeklyPayment; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }

    public int getTerm() { return term; }
    public void setTerm(int term) { this.term = term; }

    public Date getOpenDate() { return openDate; }
    public void setOpenDate(Date openDate) { this.openDate = openDate; }

    public Date getMaturityDate() { return maturityDate; }
    public void setMaturityDate(Date maturityDate) { this.maturityDate = maturityDate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}