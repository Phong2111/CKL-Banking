package com.example.cklbanking.utils;

import android.location.Location;
import com.example.cklbanking.models.Branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Helper class để tính toán khoảng cách và sắp xếp branches
 */
public class BranchDistanceHelper {
    
    /**
     * Tính khoảng cách từ user location đến branch (meters)
     */
    public static double calculateDistance(Location userLocation, Branch branch) {
        if (userLocation == null || branch == null) {
            return Double.MAX_VALUE;
        }
        
        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(), userLocation.getLongitude(),
                branch.getLatitude(), branch.getLongitude(),
                results
        );
        
        return results[0]; // meters
    }
    
    /**
     * Sắp xếp danh sách branches theo khoảng cách (gần nhất trước)
     */
    public static void sortByDistance(List<Branch> branches, Location userLocation) {
        if (branches == null || userLocation == null) {
            return;
        }
        
        Collections.sort(branches, new Comparator<Branch>() {
            @Override
            public int compare(Branch b1, Branch b2) {
                double distance1 = calculateDistance(userLocation, b1);
                double distance2 = calculateDistance(userLocation, b2);
                return Double.compare(distance1, distance2);
            }
        });
    }
    
    /**
     * Lọc branches theo khoảng cách tối đa (meters)
     */
    public static List<Branch> filterByDistance(List<Branch> branches, Location userLocation, double maxDistanceMeters) {
        if (branches == null || userLocation == null) {
            return new ArrayList<>();
        }
        
        List<Branch> filtered = new ArrayList<>();
        for (Branch branch : branches) {
            double distance = calculateDistance(userLocation, branch);
            if (distance <= maxDistanceMeters) {
                filtered.add(branch);
            }
        }
        return filtered;
    }
    
    /**
     * Lọc branches theo loại
     */
    public static List<Branch> filterByType(List<Branch> branches, String type) {
        if (branches == null || type == null || type.isEmpty() || "all".equals(type)) {
            return new ArrayList<>(branches);
        }
        
        List<Branch> filtered = new ArrayList<>();
        for (Branch branch : branches) {
            if (type.equals(branch.getType())) {
                filtered.add(branch);
            }
        }
        return filtered;
    }
    
    /**
     * Lọc branches chỉ hiển thị đang mở cửa
     */
    public static List<Branch> filterByOpenStatus(List<Branch> branches, boolean onlyOpen) {
        if (branches == null || !onlyOpen) {
            return new ArrayList<>(branches);
        }
        
        List<Branch> filtered = new ArrayList<>();
        for (Branch branch : branches) {
            if (branch.isOpen()) {
                filtered.add(branch);
            }
        }
        return filtered;
    }
    
    /**
     * Format khoảng cách thành text hiển thị
     */
    public static String formatDistance(double distanceInMeters) {
        if (distanceInMeters < 1000) {
            return String.format("%.0f m", distanceInMeters);
        } else {
            return String.format("%.2f km", distanceInMeters / 1000);
        }
    }
}

