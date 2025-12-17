package com.example.cklbanking.utils;

/**
 * Utility class to calculate transaction fees
 */
public class TransactionFeeCalculator {
    
    // Fee rates
    private static final double INTERNAL_TRANSFER_FEE_RATE = 0.0; // Free for internal transfers
    private static final double EXTERNAL_TRANSFER_FEE_RATE = 0.001; // 0.1% for external transfers
    private static final double MIN_EXTERNAL_FEE = 5000; // Minimum 5,000 VND
    private static final double MAX_EXTERNAL_FEE = 50000; // Maximum 50,000 VND
    
    /**
     * Calculate transaction fee based on transfer type and amount
     * @param amount Transaction amount
     * @param transferType "internal" or "external"
     * @return Transaction fee amount
     */
    public static double calculateFee(double amount, String transferType) {
        if (amount <= 0) {
            return 0;
        }
        
        if ("internal".equals(transferType)) {
            // Internal transfers are free
            return 0;
        } else if ("external".equals(transferType)) {
            // External transfers: 0.1% of amount, min 5,000 VND, max 50,000 VND
            double fee = amount * EXTERNAL_TRANSFER_FEE_RATE;
            
            if (fee < MIN_EXTERNAL_FEE) {
                fee = MIN_EXTERNAL_FEE;
            } else if (fee > MAX_EXTERNAL_FEE) {
                fee = MAX_EXTERNAL_FEE;
            }
            
            return fee;
        }
        
        return 0;
    }
    
    /**
     * Calculate total amount (amount + fee)
     * @param amount Transaction amount
     * @param transferType "internal" or "external"
     * @return Total amount including fee
     */
    public static double calculateTotalAmount(double amount, String transferType) {
        return amount + calculateFee(amount, transferType);
    }
    
    /**
     * Format fee for display
     * @param fee Fee amount
     * @return Formatted fee string
     */
    public static String formatFee(double fee) {
        if (fee == 0) {
            return "Miễn phí";
        }
        return String.format("%,.0f ₫", fee);
    }
}

