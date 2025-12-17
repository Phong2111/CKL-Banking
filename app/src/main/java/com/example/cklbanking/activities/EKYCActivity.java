package com.example.cklbanking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cklbanking.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.example.cklbanking.services.FaceVerificationService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EKYCActivity extends AppCompatActivity {

    private static final String TAG = "EKYCActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    // UI Components
    private MaterialToolbar toolbar;
    private PreviewView previewView;
    private TextView statusText;
    private MaterialButton btnCapture, btnRetake;
    private CircularProgressIndicator verificationProgress;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ExecutorService executorService;

    // ML Kit
    private FaceDetector faceDetector;
    
    // Face Verification Service
    private FaceVerificationService faceVerificationService;

    // Data
    private String userId;
    private boolean faceDetected = false;
    private String capturedImageUrl;
    private boolean isCapturing = false;
    
    // Pending transaction data (if coming from high-value transaction)
    private double pendingTransactionAmount;
    private String pendingTransactionType; // "transfer", "deposit", "withdraw", "bill_payment"
    private String pendingTransactionAccountId;
    private String pendingTransactionToAccount;
    private String pendingTransactionRecipient;
    
    // Pending bill payment data
    private String pendingBillPaymentBillId;
    private String pendingBillPaymentCustomerCode;
    private String pendingBillPaymentType; // "electricity" or "water"
    private String pendingBillPaymentProvider; // "EVN" or "SAVACO"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekyc);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Initialize ML Kit Face Detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();
        faceDetector = FaceDetection.getClient(options);
        
        // Initialize Face Verification Service
        faceVerificationService = new FaceVerificationService();

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
        executorService = Executors.newSingleThreadExecutor();
        
        // Get pending transaction data if any
        pendingTransactionAmount = getIntent().getDoubleExtra("pending_transaction_amount", 0);
        pendingTransactionType = getIntent().getStringExtra("pending_transaction_type");
        pendingTransactionAccountId = getIntent().getStringExtra("pending_transaction_account_id");
        pendingTransactionToAccount = getIntent().getStringExtra("pending_transaction_to_account");
        pendingTransactionRecipient = getIntent().getStringExtra("pending_transaction_recipient");
        
        // Also check for deposit/withdraw pending data
        if (pendingTransactionAmount == 0) {
            pendingTransactionAmount = getIntent().getDoubleExtra("pending_deposit_amount", 0);
            if (pendingTransactionAmount > 0) {
                pendingTransactionType = "deposit";
                pendingTransactionAccountId = getIntent().getStringExtra("pending_deposit_account_id");
            } else {
                pendingTransactionAmount = getIntent().getDoubleExtra("pending_withdraw_amount", 0);
                if (pendingTransactionAmount > 0) {
                    pendingTransactionType = "withdraw";
                    pendingTransactionAccountId = getIntent().getStringExtra("pending_withdraw_account_id");
                } else {
                    // Check for bill payment
                    pendingTransactionAmount = getIntent().getDoubleExtra("pending_bill_payment_amount", 0);
                    if (pendingTransactionAmount > 0) {
                        pendingTransactionType = "bill_payment";
                        pendingTransactionAccountId = getIntent().getStringExtra("pending_bill_payment_account_id");
                        pendingBillPaymentBillId = getIntent().getStringExtra("pending_bill_payment_bill_id");
                        pendingBillPaymentCustomerCode = getIntent().getStringExtra("pending_bill_payment_customer_code");
                        pendingBillPaymentType = getIntent().getStringExtra("pending_bill_payment_type");
                        pendingBillPaymentProvider = getIntent().getStringExtra("pending_bill_payment_provider");
                    }
                }
            }
        }

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Listeners
        setupListeners();

        // Check camera permission
        checkCameraPermission();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusText);
        btnCapture = findViewById(R.id.btnCapture);
        btnRetake = findViewById(R.id.btnRetake);
        verificationProgress = findViewById(R.id.verificationProgress);

        // Initial button states
        btnRetake.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        
        // Show appropriate message based on context
        if (pendingTransactionAmount > 0) {
            statusText.setText("Giao dịch lớn yêu cầu xác thực eKYC\nĐặt khuôn mặt vào khung hình");
            statusText.setTextColor(getColor(R.color.warning));
        } else {
            statusText.setText("Đặt khuôn mặt vào khung hình");
            statusText.setTextColor(getColor(R.color.white));
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnCapture.setOnClickListener(v -> {
            if (faceDetected && !isCapturing) {
                captureImage();
            } else if (!faceDetected) {
                Toast.makeText(this, "Vui lòng đợi phát hiện khuôn mặt", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        btnRetake.setOnClickListener(v -> retryCapture());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để sử dụng eKYC", 
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Lỗi khởi tạo camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview() {
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // ImageCapture
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // ImageAnalysis for face detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                detectFace(imageProxy);
            });

        // Camera selector - use front camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalysis, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void detectFace(ImageProxy imageProxy) {
        InputImage image = InputImage.fromMediaImage(
            imageProxy.getImage(), 
            imageProxy.getImageInfo().getRotationDegrees());

        faceDetector.process(image)
            .addOnSuccessListener(faces -> {
                runOnUiThread(() -> {
                    if (!faces.isEmpty() && !isCapturing) {
                        faceDetected = true;
                        if (pendingTransactionAmount > 0) {
                            statusText.setText("✓ Khuôn mặt đã được phát hiện!\nNhấn nút để xác thực");
                        } else {
                            statusText.setText("✓ Khuôn mặt đã được phát hiện!\nNhấn nút để chụp");
                        }
                        statusText.setTextColor(getColor(R.color.success));
                        btnCapture.setEnabled(true);
                    } else if (faces.isEmpty() && !isCapturing) {
                        faceDetected = false;
                        if (pendingTransactionAmount > 0) {
                            statusText.setText("Giao dịch lớn yêu cầu xác thực eKYC\nĐặt khuôn mặt vào khung hình");
                            statusText.setTextColor(getColor(R.color.warning));
                        } else {
                            statusText.setText("Đặt khuôn mặt vào khung hình");
                            statusText.setTextColor(getColor(R.color.white));
                        }
                        btnCapture.setEnabled(false);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Face detection failed", e);
            })
            .addOnCompleteListener(task -> {
                imageProxy.close();
            });
    }

    private void captureImage() {
        if (imageCapture == null || isCapturing) {
            return;
        }

        isCapturing = true;
        showLoading(true);
        btnCapture.setEnabled(false);

        // Create output file options
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
            new File(getCacheDir(), "ekyc_capture_" + System.currentTimeMillis() + ".jpg")
        ).build();

        // Capture image
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                    File savedFile = new File(output.getSavedUri().getPath());
                    uploadFaceImage(savedFile);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    runOnUiThread(() -> {
                        isCapturing = false;
                        showLoading(false);
                        btnCapture.setEnabled(true);
                        Toast.makeText(EKYCActivity.this, 
                            "Lỗi chụp ảnh: " + exception.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                    Log.e(TAG, "Image capture failed", exception);
                }
            }
        );
    }

    private void uploadFaceImage(File imageFile) {
        // Read file as bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            runOnUiThread(() -> {
                isCapturing = false;
            showLoading(false);
                btnCapture.setEnabled(true);
                Toast.makeText(this, "Lỗi đọc ảnh", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Compress bitmap
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        // Upload to Firebase Storage
        String fileName = "ekyc/" + userId + "/face_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        UploadTask uploadTask = storageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                capturedImageUrl = uri.toString();
                runOnUiThread(() -> {
                    if (pendingTransactionAmount > 0) {
                        statusText.setText("Đang xác thực khuôn mặt...");
                        statusText.setTextColor(getColor(R.color.info));
                    } else {
                        statusText.setText("Ảnh đã được chụp thành công!");
                        statusText.setTextColor(getColor(R.color.success));
                    }
                    btnCapture.setVisibility(View.GONE);
                    btnRetake.setVisibility(View.VISIBLE);
                    showLoading(true); // Keep loading during verification
                    isCapturing = false;
            
                    // Auto submit after capture
                    submitEKYC();
                });
            }).addOnFailureListener(e -> {
                runOnUiThread(() -> {
                    isCapturing = false;
                    showLoading(false);
                    btnCapture.setEnabled(true);
                    Toast.makeText(EKYCActivity.this, 
                        "Lỗi lấy URL ảnh: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Failed to get download URL", e);
            });
        }).addOnFailureListener(e -> {
            runOnUiThread(() -> {
                isCapturing = false;
                showLoading(false);
                btnCapture.setEnabled(true);
                Toast.makeText(EKYCActivity.this, 
                    "Lỗi upload ảnh: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
            Log.e(TAG, "Failed to upload image", e);
        });
    }

    private void retryCapture() {
        statusText.setText("Đặt khuôn mặt vào khung hình");
        statusText.setTextColor(getColor(R.color.text_primary));
        
        btnCapture.setVisibility(View.VISIBLE);
        btnRetake.setVisibility(View.GONE);
        btnCapture.setEnabled(false);
        
        faceDetected = false;
        capturedImageUrl = null;
        isCapturing = false;
    }

    private void submitEKYC() {
        if (!faceDetected || capturedImageUrl == null) {
            Toast.makeText(this, "Vui lòng chụp ảnh khuôn mặt trước", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        
        // If this is for high-value transaction verification, verify face match first
        if (pendingTransactionAmount > 0) {
            verifyFaceForTransaction();
        } else {
            // Regular eKYC submission
            updateEkycStatus();
        }
    }
    
    /**
     * Verify face match for high-value transaction
     */
    private void verifyFaceForTransaction() {
        // Get stored face image URL
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showLoading(false);
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String storedFaceImageUrl = documentSnapshot.getString("faceImageUrl");
                    
                    if (storedFaceImageUrl == null || storedFaceImageUrl.isEmpty()) {
                        // No stored image - this is first time eKYC
                        updateEkycStatus();
                        return;
                    }
                    
                    // Load current captured image
                    loadAndVerifyFace(capturedImageUrl, storedFaceImageUrl);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Load and verify face match
     */
    private void loadAndVerifyFace(String currentImageUrl, String storedImageUrl) {
        // Download current image
        executorService.execute(() -> {
            try {
                Bitmap currentBitmap = downloadImage(currentImageUrl);
                
                if (currentBitmap == null) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(this, "Không thể tải ảnh hiện tại", 
                            Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Verify face match
                faceVerificationService.verifyFaceMatch(currentBitmap, storedImageUrl, 
                    (isMatch, message) -> {
                        runOnUiThread(() -> {
                            if (isMatch) {
                                // Face matches - update eKYC and continue transaction
                                statusText.setText("✓ Xác thực thành công!\nĐang cập nhật...");
                                statusText.setTextColor(getColor(R.color.success));
                                updateEkycStatus();
                            } else {
                                // Face doesn't match - show error
                                showLoading(false);
                                statusText.setText("✗ Xác thực thất bại\n" + message);
                                statusText.setTextColor(getColor(R.color.error));
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                btnRetake.setVisibility(View.VISIBLE);
                                btnCapture.setVisibility(View.GONE);
                            }
                        });
                    });
                    
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi xác thực: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Download image from URL
     */
    private Bitmap downloadImage(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            
            java.io.InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading image", e);
            return null;
        }
    }
    
    /**
     * Update eKYC status in Firestore
     */
    private void updateEkycStatus() {
        // Update user eKYC status
        Map<String, Object> updates = new HashMap<>();
        updates.put("ekycStatus", "verified");
        updates.put("faceImageUrl", capturedImageUrl);
        updates.put("ekycVerifiedAt", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Xác thực eKYC thành công!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // If there's a pending transaction, return to it
                    if (pendingTransactionAmount > 0) {
                        // Return to previous activity with success flag
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("ekyc_verified", true);
                        if ("bill_payment".equals(pendingTransactionType)) {
                            // For bill payment, return with bill info
                            resultIntent.putExtra("bill_id", pendingBillPaymentBillId);
                            resultIntent.putExtra("customer_code", pendingBillPaymentCustomerCode);
                            resultIntent.putExtra("amount", pendingTransactionAmount);
                            resultIntent.putExtra("account_id", pendingTransactionAccountId);
                        }
                        setResult(RESULT_OK, resultIntent);
                    }
                    
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        verificationProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
        if (faceVerificationService != null) {
            faceVerificationService.cleanup();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
