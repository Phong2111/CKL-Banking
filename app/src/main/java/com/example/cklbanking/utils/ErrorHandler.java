package com.example.cklbanking.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Utility class để xử lý errors một cách thống nhất
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";
    
    /**
     * Handle error và hiển thị message thân thiện với người dùng
     */
    public static void handleError(Context context, Exception error, String defaultMessage) {
        String userMessage = getErrorMessage(context, error, defaultMessage);
        String logMessage = getLogMessage(error);
        
        // Log chi tiết cho developer
        Log.e(TAG, logMessage, error);
        
        // Hiển thị message thân thiện cho user
        showUserMessage(context, userMessage);
    }
    
    /**
     * Handle error với callback để có thể retry
     */
    public static void handleErrorWithRetry(Context context, Exception error, 
                                            String defaultMessage, Runnable retryAction) {
        String userMessage = getErrorMessage(context, error, defaultMessage);
        String logMessage = getLogMessage(error);
        
        Log.e(TAG, logMessage, error);
        
        // Show Snackbar with retry action
        if (context instanceof android.app.Activity) {
            View rootView = ((android.app.Activity) context).findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar snackbar = Snackbar.make(rootView, userMessage, Snackbar.LENGTH_LONG);
                if (retryAction != null) {
                    snackbar.setAction("Thử lại", v -> retryAction.run());
                }
                snackbar.show();
                return;
            }
        }
        
        // Fallback to Toast
        showUserMessage(context, userMessage);
    }
    
    /**
     * Get user-friendly error message
     */
    private static String getErrorMessage(Context context, Exception error, String defaultMessage) {
        if (error == null) {
            return defaultMessage != null ? defaultMessage : "Đã xảy ra lỗi";
        }
        
        // Handle Firebase exceptions
        if (error instanceof FirebaseNetworkException) {
            return "Không có kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.";
        }
        
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                    return "Bạn không có quyền thực hiện thao tác này.";
                case UNAVAILABLE:
                    return "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.";
                case DEADLINE_EXCEEDED:
                    return "Yêu cầu quá thời gian chờ. Vui lòng thử lại.";
                case UNAUTHENTICATED:
                    return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
                default:
                    return "Lỗi kết nối cơ sở dữ liệu. Vui lòng thử lại.";
            }
        }
        
        if (error instanceof FirebaseException) {
            return "Lỗi kết nối Firebase. Vui lòng thử lại sau.";
        }
        
        // Handle generic exceptions
        String errorMessage = error.getMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            // Don't show technical error messages to users
            if (errorMessage.contains("PERMISSION_DENIED")) {
                return "Bạn không có quyền thực hiện thao tác này.";
            }
            if (errorMessage.contains("network") || errorMessage.contains("Network")) {
                return "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối.";
            }
            if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
                return "Yêu cầu quá thời gian chờ. Vui lòng thử lại.";
            }
        }
        
        return defaultMessage != null ? defaultMessage : "Đã xảy ra lỗi. Vui lòng thử lại.";
    }
    
    /**
     * Get detailed log message for debugging
     */
    private static String getLogMessage(Exception error) {
        if (error == null) {
            return "Unknown error";
        }
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Error: ").append(error.getClass().getSimpleName());
        
        if (error.getMessage() != null) {
            logMessage.append(" - ").append(error.getMessage());
        }
        
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            logMessage.append(" [Code: ").append(firestoreException.getCode()).append("]");
        }
        
        return logMessage.toString();
    }
    
    /**
     * Show message to user
     */
    private static void showUserMessage(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Check if error is network related
     */
    public static boolean isNetworkError(Exception error) {
        return error instanceof FirebaseNetworkException ||
               (error != null && error.getMessage() != null &&
                (error.getMessage().contains("network") || 
                 error.getMessage().contains("Network") ||
                 error.getMessage().contains("timeout") ||
                 error.getMessage().contains("Timeout")));
    }
    
    /**
     * Check if error is permission related
     */
    public static boolean isPermissionError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            return ((FirebaseFirestoreException) error).getCode() == 
                   FirebaseFirestoreException.Code.PERMISSION_DENIED;
        }
        return error != null && error.getMessage() != null &&
               error.getMessage().contains("PERMISSION_DENIED");
    }
}

