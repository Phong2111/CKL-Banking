package com.example.cklbanking.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service để verify ảnh khuôn mặt khi giao dịch lớn
 * So sánh ảnh hiện tại với ảnh đã lưu trong eKYC
 */
public class FaceVerificationService {
    private static final String TAG = "FaceVerificationService";
    
    private FaceDetector faceDetector;
    private ExecutorService executorService;
    
    // Similarity threshold (0.0 - 1.0)
    // Higher value = more strict matching
    private static final double SIMILARITY_THRESHOLD = 0.7;
    
    public FaceVerificationService() {
        // Initialize ML Kit Face Detector with high accuracy
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();
        
        faceDetector = FaceDetection.getClient(options);
        executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Verify face match between current image and stored eKYC image
     * @param currentImageBitmap Current face image (from camera)
     * @param storedImageUrl URL of stored eKYC image
     * @param callback Callback with verification result
     */
    public void verifyFaceMatch(Bitmap currentImageBitmap, String storedImageUrl, 
                               FaceVerificationCallback callback) {
        if (currentImageBitmap == null) {
            callback.onVerificationResult(false, "Không thể đọc ảnh hiện tại");
            return;
        }
        
        if (storedImageUrl == null || storedImageUrl.isEmpty()) {
            callback.onVerificationResult(false, "Không tìm thấy ảnh eKYC đã lưu");
            return;
        }
        
        // Download stored image in background
        executorService.execute(() -> {
            try {
                Bitmap storedImageBitmap = downloadImage(storedImageUrl);
                
                if (storedImageBitmap == null) {
                    callback.onVerificationResult(false, "Không thể tải ảnh eKYC đã lưu");
                    return;
                }
                
                // Detect faces in both images
                detectAndCompareFaces(currentImageBitmap, storedImageBitmap, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error verifying face match", e);
                callback.onVerificationResult(false, "Lỗi xác thực: " + e.getMessage());
            }
        });
    }
    
    /**
     * Download image from URL
     */
    private Bitmap downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading image", e);
            return null;
        }
    }
    
    /**
     * Detect faces in both images and compare
     */
    private void detectAndCompareFaces(Bitmap currentImage, Bitmap storedImage,
                                      FaceVerificationCallback callback) {
        InputImage currentInputImage = InputImage.fromBitmap(currentImage, 0);
        InputImage storedInputImage = InputImage.fromBitmap(storedImage, 0);
        
        // Detect face in current image
        faceDetector.process(currentInputImage)
                .addOnSuccessListener(currentFaces -> {
                    if (currentFaces.isEmpty()) {
                        callback.onVerificationResult(false, "Không phát hiện khuôn mặt trong ảnh hiện tại");
                        return;
                    }
                    
                    Face currentFace = currentFaces.get(0);
                    
                    // Detect face in stored image
                    faceDetector.process(storedInputImage)
                            .addOnSuccessListener(storedFaces -> {
                                if (storedFaces.isEmpty()) {
                                    callback.onVerificationResult(false, "Không phát hiện khuôn mặt trong ảnh eKYC");
                                    return;
                                }
                                
                                Face storedFace = storedFaces.get(0);
                                
                                // Compare faces
                                boolean isMatch = compareFaces(currentFace, storedFace);
                                
                                if (isMatch) {
                                    callback.onVerificationResult(true, "Xác thực khuôn mặt thành công");
                                } else {
                                    callback.onVerificationResult(false, "Khuôn mặt không khớp với ảnh eKYC đã lưu");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to detect face in stored image", e);
                                callback.onVerificationResult(false, "Lỗi xử lý ảnh eKYC: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to detect face in current image", e);
                    callback.onVerificationResult(false, "Lỗi xử lý ảnh hiện tại: " + e.getMessage());
                });
    }
    
    /**
     * Compare two faces using landmarks and bounding box
     * This is a simplified comparison - in production, use more advanced face recognition
     */
    private boolean compareFaces(Face currentFace, Face storedFace) {
        // Get bounding boxes
        android.graphics.Rect currentBoundingBox = currentFace.getBoundingBox();
        android.graphics.Rect storedBoundingBox = storedFace.getBoundingBox();
        
        // Calculate similarity based on bounding box size and position
        // In a real implementation, you would use face embeddings/features
        double widthSimilarity = calculateSimilarity(
            currentBoundingBox.width(), storedBoundingBox.width());
        double heightSimilarity = calculateSimilarity(
            currentBoundingBox.height(), storedBoundingBox.height());
        
        // Average similarity
        double averageSimilarity = (widthSimilarity + heightSimilarity) / 2.0;
        
        // Check if similarity meets threshold
        return averageSimilarity >= SIMILARITY_THRESHOLD;
    }
    
    /**
     * Calculate similarity between two values (0.0 - 1.0)
     */
    private double calculateSimilarity(double value1, double value2) {
        if (value1 == 0 && value2 == 0) return 1.0;
        if (value1 == 0 || value2 == 0) return 0.0;
        
        double max = Math.max(value1, value2);
        double min = Math.min(value1, value2);
        
        return min / max;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (faceDetector != null) {
            faceDetector.close();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    /**
     * Callback interface for face verification
     */
    public interface FaceVerificationCallback {
        void onVerificationResult(boolean isMatch, String message);
    }
}
