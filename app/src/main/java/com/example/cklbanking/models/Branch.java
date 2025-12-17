package com.example.cklbanking.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Branch implements Serializable {
    private String branchId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phoneNumber;
    private String openingHours; // e.g., "8:00 - 17:00"
    private boolean isOpen; // Current status
    private String type; // "branch" or "atm"
    private java.util.List<String> services; // Dịch vụ có sẵn: ["ATM", "Gửi tiết kiệm", "Vay vốn", "Tư vấn"]
    private String imageUrl; // URL hình ảnh chi nhánh
    private boolean isFavorite; // Chi nhánh yêu thích (local, không lưu trong Firestore)

    // Constructor
    public Branch() {}

    public Branch(String name, String address, double latitude, double longitude, String phoneNumber, String openingHours, String type) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.openingHours = openingHours;
        this.type = type;
        this.isOpen = true; // Default to open
    }

    // Getters and Setters
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public boolean isOpen() {
        // Nếu đã được set sẵn, dùng giá trị đó
        if (openingHours == null || openingHours.isEmpty()) {
            return isOpen; // Fallback to stored value
        }
        
        // Nếu là 24/7, luôn mở
        if (openingHours.toLowerCase().contains("24/7") || 
            openingHours.toLowerCase().contains("24h")) {
            return true;
        }
        
        // Tính toán dựa trên giờ hiện tại
        return calculateIsOpen();
    }
    
    /**
     * Tính toán xem branch có đang mở cửa không dựa trên openingHours
     */
    private boolean calculateIsOpen() {
        if (openingHours == null || openingHours.isEmpty()) {
            return isOpen; // Fallback
        }
        
        try {
            // Parse openingHours format: "8:00 - 17:00" hoặc "08:00 - 17:00"
            String[] parts = openingHours.split("-");
            if (parts.length != 2) {
                return isOpen; // Fallback nếu format không đúng
            }
            
            String openTimeStr = parts[0].trim();
            String closeTimeStr = parts[1].trim();
            
            // Parse time
            String[] openParts = openTimeStr.split(":");
            String[] closeParts = closeTimeStr.split(":");
            
            if (openParts.length != 2 || closeParts.length != 2) {
                return isOpen; // Fallback
            }
            
            int openHour = Integer.parseInt(openParts[0]);
            int openMinute = Integer.parseInt(openParts[1]);
            int closeHour = Integer.parseInt(closeParts[0]);
            int closeMinute = Integer.parseInt(closeParts[1]);
            
            // Get current time
            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(java.util.Calendar.MINUTE);
            
            // Convert to minutes for easier comparison
            int currentTime = currentHour * 60 + currentMinute;
            int openTime = openHour * 60 + openMinute;
            int closeTime = closeHour * 60 + closeMinute;
            
            // Check if current time is within opening hours
            if (openTime <= closeTime) {
                // Normal case: 8:00 - 17:00
                return currentTime >= openTime && currentTime <= closeTime;
            } else {
                // Overnight case: 22:00 - 6:00 (next day)
                return currentTime >= openTime || currentTime <= closeTime;
            }
        } catch (Exception e) {
            // Nếu có lỗi parse, fallback to stored value
            return isOpen;
        }
    }
    
    public void setOpen(boolean open) { isOpen = open; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public java.util.List<String> getServices() { return services; }
    public void setServices(java.util.List<String> services) { this.services = services; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    
    /**
     * Kiểm tra xem branch có dịch vụ cụ thể không
     */
    public boolean hasService(String serviceName) {
        if (services == null || services.isEmpty()) {
            return false;
        }
        return services.contains(serviceName);
    }
}












