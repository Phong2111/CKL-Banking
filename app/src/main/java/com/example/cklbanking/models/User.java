package com.example.cklbanking.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    private String userId; // Sẽ dùng để lưu Document ID (từ Firebase Auth)
    private String fullName;
    private String email;
    private String phone;
    private String role; // 'customer' hoặc 'officer'
    private String ekycStatus; // 'pending', 'verified', 'failed'
    private String faceImageUrl;

    @ServerTimestamp // Tự động lấy giờ server khi tạo
    private Date createdAt;

    // Constructor rỗng - BẮT BUỘC cho Firestore
    public User() {}

    // Constructor để bạn dùng khi Đăng ký
    public User(String userId, String fullName, String email, String phone) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = "customer"; // Mặc định là khách hàng
        this.ekycStatus = "pending"; // Mặc định là chờ xác thực
    }

    // --- Bổ sung đầy đủ Getter và Setter cho tất cả các trường ---
    // (Ví dụ)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFaceImageUrl() { return faceImageUrl; }
    public void setFaceImageUrl(String faceImageUrl) { this.faceImageUrl = faceImageUrl; }

    public String getEkycStatus() { return ekycStatus;}
    public void setEkycStatus(String ekycStatus) { this.ekycStatus = ekycStatus; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}