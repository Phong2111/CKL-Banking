package com.example.cklbanking.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model cho hóa đơn điện/nước chưa thanh toán
 */
public class Bill {
    private String billId; // Document ID
    private String customerCode; // Mã khách hàng
    private String provider; // Nhà cung cấp: "EVN", "SAVACO"
    private String billType; // "electricity" hoặc "water"
    private double amount; // Số tiền cần thanh toán
    private String period; // Kỳ thanh toán (ví dụ: "11/2024")
    private Date dueDate; // Hạn thanh toán
    private String status; // "unpaid", "paid", "overdue"
    private String customerName; // Tên khách hàng (nếu có)
    private String address; // Địa chỉ (nếu có)
    private String description; // Mô tả thêm
    
    @ServerTimestamp
    private Date createdAt; // Ngày tạo hóa đơn
    
    // Constructor rỗng - BẮT BUỘC cho Firestore
    public Bill() {}
    
    // Constructor với thông tin cơ bản
    public Bill(String customerCode, String provider, String billType, 
                double amount, String period, Date dueDate) {
        this.customerCode = customerCode;
        this.provider = provider;
        this.billType = billType;
        this.amount = amount;
        this.period = period;
        this.dueDate = dueDate;
        this.status = "unpaid";
    }
    
    // Getters and Setters
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    /**
     * Kiểm tra hóa đơn có quá hạn không
     */
    public boolean isOverdue() {
        if (dueDate == null) return false;
        return dueDate.before(new Date()) && "unpaid".equals(status);
    }
    
    /**
     * Lấy tên hiển thị của loại hóa đơn
     */
    public String getBillTypeDisplayName() {
        return "electricity".equals(billType) ? "Tiền điện" : "Tiền nước";
    }
}

