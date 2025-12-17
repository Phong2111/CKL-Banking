package com.example.cklbanking;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.functions.FirebaseFunctions;

/**
 * Application class để cấu hình Firebase Firestore và Functions
 * Kết nối với Firebase Emulator khi chạy local
 */
public class CKLBankingApplication extends Application {
    
    private static final String TAG = "CKLBankingApp";
    
    // Set to true để sử dụng Firebase Emulator
    // Set to false để sử dụng production Firebase
    private static final boolean USE_EMULATOR = true;
    
    // Emulator host
    // - Android Emulator: 10.0.2.2 (localhost của máy host)
    // - Physical device: IP của máy tính (ví dụ: 192.168.1.100)
    // LƯU Ý: Để dùng với điện thoại thật, cần:
    // 1. Tìm IP máy tính: ipconfig (Windows) hoặc ifconfig (Mac/Linux)
    // 2. Đảm bảo điện thoại và máy tính cùng mạng WiFi
    // 3. Đảm bảo firewall cho phép kết nối đến port 8080 và 5001
    private static final String EMULATOR_HOST = "10.0.2.2"; // Android Emulator
    // private static final String EMULATOR_HOST = "192.168.1.100"; // Physical device - THAY ĐỔI IP NÀY
    
    // Emulator ports
    private static final int FIRESTORE_PORT = 8080;
    private static final int FUNCTIONS_PORT = 5001;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Cấu hình Firestore
        configureFirestore();
        
        // Cấu hình Functions
        configureFunctions();
    }
    
    private void configureFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        if (USE_EMULATOR) {
            // Cấu hình để sử dụng Firestore Emulator
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setHost(EMULATOR_HOST + ":" + FIRESTORE_PORT)
                    .setSslEnabled(false)
                    .setPersistenceEnabled(false) // Tắt persistence khi dùng emulator
                    .build();
            
            db.setFirestoreSettings(settings);
            
            Log.d(TAG, "===========================================");
            Log.d(TAG, "🔥 Firebase Emulator Mode ENABLED");
            Log.d(TAG, "Firestore: " + EMULATOR_HOST + ":" + FIRESTORE_PORT);
            Log.d(TAG, "Functions: " + EMULATOR_HOST + ":" + FUNCTIONS_PORT);
            Log.d(TAG, "===========================================");
        } else {
            // Sử dụng production Firebase (default)
            Log.d(TAG, "🔥 Using Production Firebase");
        }
    }
    
    private void configureFunctions() {
        if (USE_EMULATOR) {
            // Cấu hình Functions để sử dụng emulator
            FirebaseFunctions functions = FirebaseFunctions.getInstance();
            try {
                // Kết nối với Functions emulator
                functions.useEmulator(EMULATOR_HOST, FUNCTIONS_PORT);
                Log.d(TAG, "Functions emulator configured: " + EMULATOR_HOST + ":" + FUNCTIONS_PORT);
            } catch (Exception e) {
                Log.e(TAG, "Error configuring Functions emulator", e);
            }
        }
    }
    
    /**
     * Helper method để thay đổi emulator host cho physical device
     * Gọi method này từ Activity nếu cần thay đổi IP
     */
    public static void setEmulatorHost(String host) {
        if (USE_EMULATOR) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setHost(host + ":" + FIRESTORE_PORT)
                    .setSslEnabled(false)
                    .setPersistenceEnabled(false)
                    .build();
            
            db.setFirestoreSettings(settings);
            
            Log.d(TAG, "Emulator host changed to: " + host);
        }
    }
}

