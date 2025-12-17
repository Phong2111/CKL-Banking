package com.example.cklbanking.utils;

import android.text.TextUtils;
import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationHelper {
    
    // Vietnamese name pattern: allows Vietnamese characters, spaces, and common name characters
    private static final Pattern VIETNAMESE_NAME_PATTERN = Pattern.compile(
        "^[\\p{L}\\s.'-]+$", 
        Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
    );
    
    // Bank code pattern: typically 3-4 uppercase letters or numbers
    private static final Pattern BANK_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{3,4}$");
    
    // Account number pattern: typically 8-16 digits
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{8,16}$");
    
    /**
     * Validate recipient name for external transfer
     * @param name Recipient name
     * @return ValidationResult with isValid and errorMessage
     */
    public static ValidationResult validateRecipientName(String name) {
        if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
            return new ValidationResult(false, "Tên người nhận không được để trống");
        }
        
        String trimmedName = name.trim();
        
        // Check minimum length
        if (trimmedName.length() < 2) {
            return new ValidationResult(false, "Tên người nhận phải có ít nhất 2 ký tự");
        }
        
        // Check maximum length
        if (trimmedName.length() > 100) {
            return new ValidationResult(false, "Tên người nhận không được vượt quá 100 ký tự");
        }
        
        // Check for valid characters (Vietnamese characters, spaces, dots, hyphens, apostrophes)
        if (!VIETNAMESE_NAME_PATTERN.matcher(trimmedName).matches()) {
            return new ValidationResult(false, 
                "Tên người nhận chỉ được chứa chữ cái, khoảng trắng và các ký tự: . - '");
        }
        
        // Check for consecutive spaces
        if (trimmedName.contains("  ")) {
            return new ValidationResult(false, "Tên người nhận không được có khoảng trắng liên tiếp");
        }
        
        // Check for numbers (names shouldn't contain numbers)
        if (Pattern.compile(".*\\d.*").matcher(trimmedName).matches()) {
            return new ValidationResult(false, "Tên người nhận không được chứa số");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate bank code
     * @param bankCode Bank code
     * @return ValidationResult with isValid and errorMessage
     */
    public static ValidationResult validateBankCode(String bankCode) {
        if (TextUtils.isEmpty(bankCode) || bankCode.trim().isEmpty()) {
            return new ValidationResult(false, "Mã ngân hàng không được để trống");
        }
        
        String trimmedCode = bankCode.trim().toUpperCase();
        
        // Check format
        if (!BANK_CODE_PATTERN.matcher(trimmedCode).matches()) {
            return new ValidationResult(false, 
                "Mã ngân hàng không hợp lệ. Mã ngân hàng phải có 3-4 ký tự (chữ hoa hoặc số)");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate bank name (for dropdown selection)
     * @param bankName Bank name
     * @return ValidationResult with isValid and errorMessage
     */
    public static ValidationResult validateBankName(String bankName) {
        if (TextUtils.isEmpty(bankName) || bankName.trim().isEmpty()) {
            return new ValidationResult(false, "Vui lòng chọn ngân hàng");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate account number
     * @param accountNumber Account number
     * @return ValidationResult with isValid and errorMessage
     */
    public static ValidationResult validateAccountNumber(String accountNumber) {
        if (TextUtils.isEmpty(accountNumber) || accountNumber.trim().isEmpty()) {
            return new ValidationResult(false, "Số tài khoản không được để trống");
        }
        
        String trimmed = accountNumber.trim();
        
        // Check format (8-16 digits)
        if (!ACCOUNT_NUMBER_PATTERN.matcher(trimmed).matches()) {
            return new ValidationResult(false, 
                "Số tài khoản không hợp lệ. Số tài khoản phải có 8-16 chữ số");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Get bank code from bank name (mapping Vietnamese banks to their codes)
     */
    public static String getBankCodeFromName(String bankName) {
        if (TextUtils.isEmpty(bankName)) {
            return null;
        }
        
        // Map Vietnamese bank names to their codes
        String name = bankName.trim().toUpperCase();
        
        if (name.contains("VIETCOMBANK") || name.contains("VCB")) {
            return "VCB";
        } else if (name.contains("TECHCOMBANK") || name.contains("TCB")) {
            return "TCB";
        } else if (name.contains("BIDV")) {
            return "BIDV";
        } else if (name.contains("VIETINBANK") || name.contains("VTB")) {
            return "VTB";
        } else if (name.contains("ACB")) {
            return "ACB";
        } else if (name.contains("MB BANK") || name.contains("MB")) {
            return "MB";
        } else if (name.contains("SACOMBANK") || name.contains("STB")) {
            return "STB";
        } else if (name.contains("VPBANK") || name.contains("VPB")) {
            return "VPB";
        } else if (name.contains("AGRIBANK") || name.contains("VAB")) {
            return "VAB";
        } else if (name.contains("TPBANK") || name.contains("TPB")) {
            return "TPB";
        }
        
        // If no match, return null (will need manual input)
        return null;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

