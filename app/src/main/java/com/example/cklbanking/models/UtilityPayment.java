package com.example.cklbanking.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class UtilityPayment {
    private String paymentId; // Document ID
    private String userId;
    private String fromAccountId;
    private String utilityType; // 'electricity', 'water', 'internet', 'phone_recharge', 'flight_ticket', 'movie_ticket', 'hotel_booking', 'ecommerce'
    private double amount;
    private String status; // 'completed', 'pending', 'failed'
    
    // Thông tin thanh toán hóa đơn
    private String customerCode; // Mã khách hàng
    private String provider; // Nhà cung cấp (EVN, SAVACO, etc.)
    
    // Thông tin nạp tiền điện thoại
    private String phoneNumber;
    private String telecomProvider; // 'Viettel', 'VinaPhone', 'Mobifone'
    
    // Thông tin đặt vé/booking
    private String bookingReference; // Mã đặt chỗ
    private String bookingDetails; // Chi tiết đặt chỗ (JSON string hoặc object)
    
    // Thông tin ecommerce
    private String orderId; // Mã đơn hàng
    private String merchantName; // Tên cửa hàng/sàn TMĐT
    
    private String description; // Mô tả giao dịch
    
    @ServerTimestamp
    private Date timestamp;
    
    // Constructor rỗng - BẮT BUỘC cho Firestore
    public UtilityPayment() {}
    
    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }
    
    public String getUtilityType() { return utilityType; }
    public void setUtilityType(String utilityType) { this.utilityType = utilityType; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getTelecomProvider() { return telecomProvider; }
    public void setTelecomProvider(String telecomProvider) { this.telecomProvider = telecomProvider; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public String getBookingDetails() { return bookingDetails; }
    public void setBookingDetails(String bookingDetails) { this.bookingDetails = bookingDetails; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}












