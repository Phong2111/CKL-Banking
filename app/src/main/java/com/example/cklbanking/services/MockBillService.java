package com.example.cklbanking.services;

import com.example.cklbanking.models.Bill;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * Service để tạo mock data cho hóa đơn điện/nước
 * Sử dụng cho demo/testing
 */
public class MockBillService {
    private static final String TAG = "MockBillService";
    private FirebaseFirestore db;
    
    // Mock customer codes
    private static final String[] MOCK_CUSTOMER_CODES = {
        "EVN001234567", "EVN001234568", "EVN001234569",
        "SAVACO987654", "SAVACO987655", "SAVACO987656"
    };
    
    // Mock customer names
    private static final String[] MOCK_CUSTOMER_NAMES = {
        "Nguyễn Văn A", "Trần Thị B", "Lê Văn C",
        "Phạm Thị D", "Hoàng Văn E", "Vũ Thị F"
    };
    
    // Mock addresses
    private static final String[] MOCK_ADDRESSES = {
        "123 Đường ABC, Quận 1, TP.HCM",
        "456 Đường XYZ, Quận 2, TP.HCM",
        "789 Đường DEF, Quận 3, TP.HCM",
        "321 Đường GHI, Quận 4, TP.HCM",
        "654 Đường JKL, Quận 5, TP.HCM",
        "987 Đường MNO, Quận 6, TP.HCM"
    };
    
    private Random random;
    
    public MockBillService() {
        db = FirebaseFirestore.getInstance();
        random = new Random();
    }
    
    /**
     * Tạo một số hóa đơn mẫu cho demo
     */
    public void generateMockBills(int count) {
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        
        for (int i = 0; i < count; i++) {
            // Random bill type
            boolean isElectricity = random.nextBoolean();
            String billType = isElectricity ? "electricity" : "water";
            String provider = isElectricity ? "EVN" : "SAVACO";
            
            // Random customer code
            String customerCode = MOCK_CUSTOMER_CODES[random.nextInt(MOCK_CUSTOMER_CODES.length)];
            String customerName = MOCK_CUSTOMER_NAMES[random.nextInt(MOCK_CUSTOMER_NAMES.length)];
            String address = MOCK_ADDRESSES[random.nextInt(MOCK_ADDRESSES.length)];
            
            // Random amount (50,000 - 2,000,000 VND)
            double amount = 50000 + random.nextDouble() * 1950000;
            
            // Random period (current month or previous month)
            calendar.setTime(now);
            int monthOffset = random.nextInt(2); // 0 or 1
            calendar.add(Calendar.MONTH, -monthOffset);
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            int year = calendar.get(Calendar.YEAR);
            String period = String.format("%02d/%d", month, year);
            
            // Due date (15-30 days from now)
            calendar.setTime(now);
            calendar.add(Calendar.DAY_OF_MONTH, 15 + random.nextInt(16));
            Date dueDate = calendar.getTime();
            
            // Create bill
            Bill bill = new Bill(customerCode, provider, billType, amount, period, dueDate);
            bill.setCustomerName(customerName);
            bill.setAddress(address);
            bill.setStatus("unpaid");
            bill.setDescription(String.format("Hóa đơn %s kỳ %s", 
                bill.getBillTypeDisplayName(), period));
            
            // Save to Firestore
            db.collection("bills")
                    .add(bill)
                    .addOnSuccessListener(documentReference -> {
                        android.util.Log.d(TAG, "Mock bill created: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e(TAG, "Failed to create mock bill", e);
                    });
            
            // Small delay to avoid rate limiting
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Tạo hóa đơn mẫu với giá trị cao (để test eKYC)
     */
    public void generateHighValueBills(int count) {
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        
        for (int i = 0; i < count; i++) {
            boolean isElectricity = random.nextBoolean();
            String billType = isElectricity ? "electricity" : "water";
            String provider = isElectricity ? "EVN" : "SAVACO";
            
            String customerCode = MOCK_CUSTOMER_CODES[random.nextInt(MOCK_CUSTOMER_CODES.length)];
            String customerName = MOCK_CUSTOMER_NAMES[random.nextInt(MOCK_CUSTOMER_NAMES.length)];
            String address = MOCK_ADDRESSES[random.nextInt(MOCK_ADDRESSES.length)];
            
            // High value amount (10,000,000 - 50,000,000 VND) - requires eKYC
            double amount = 10000000 + random.nextDouble() * 40000000;
            
            calendar.setTime(now);
            int monthOffset = random.nextInt(2);
            calendar.add(Calendar.MONTH, -monthOffset);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);
            String period = String.format("%02d/%d", month, year);
            
            calendar.setTime(now);
            calendar.add(Calendar.DAY_OF_MONTH, 15 + random.nextInt(16));
            Date dueDate = calendar.getTime();
            
            Bill bill = new Bill(customerCode, provider, billType, amount, period, dueDate);
            bill.setCustomerName(customerName);
            bill.setAddress(address);
            bill.setStatus("unpaid");
            bill.setDescription(String.format("Hóa đơn %s kỳ %s (Giá trị cao)", 
                bill.getBillTypeDisplayName(), period));
            
            db.collection("bills")
                    .add(bill)
                    .addOnSuccessListener(documentReference -> {
                        android.util.Log.d(TAG, "High-value mock bill created: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e(TAG, "Failed to create high-value mock bill", e);
                    });
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

