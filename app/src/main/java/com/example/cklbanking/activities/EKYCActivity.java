package com.example.cklbanking.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cklbanking.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EKYCActivity extends AppCompatActivity {

    private static final String TAG = "EKYCActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    // UI Components
    private MaterialToolbar toolbar;
    private View cameraPreview;
    private TextView statusText;
    private MaterialButton btnCapture, btnRetake;
    private CircularProgressIndicator verificationProgress;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private boolean faceDetected = false;
    private String capturedImageUrl;

    // TODO: CameraX and ML Kit will be initialized here
    // private PreviewView previewView;
    // private ProcessCameraProvider cameraProvider;
    // private FaceDetector faceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekyc);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

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
        cameraPreview = findViewById(R.id.cameraPreview);
        statusText = findViewById(R.id.statusText);
        btnCapture = findViewById(R.id.btnCapture);
        btnRetake = findViewById(R.id.btnRetake);
        verificationProgress = findViewById(R.id.verificationProgress);

        // Initial button states
        btnRetake.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnCapture.setOnClickListener(v -> captureImage());
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
        // TODO: Initialize CameraX
        /*
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
        */

        // Placeholder implementation
        Toast.makeText(this, "Camera đã sẵn sàng. Chức năng đang phát triển.", 
            Toast.LENGTH_SHORT).show();
        statusText.setText("Đặt khuôn mặt vào khung hình");
        statusText.setVisibility(View.VISIBLE);
    }

    private void bindCameraPreview() {
        // TODO: Bind camera preview and face detection
        /*
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), 
            imageProxy -> {
                // Process image with ML Kit Face Detection
                detectFace(imageProxy);
            });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
        */
    }

    private void detectFace(Object imageProxy) {
        // TODO: Implement face detection with ML Kit
        /*
        InputImage image = InputImage.fromMediaImage(
            imageProxy.getImage(), 
            imageProxy.getImageInfo().getRotationDegrees());

        faceDetector.process(image)
            .addOnSuccessListener(faces -> {
                if (!faces.isEmpty()) {
                    faceDetected = true;
                    statusText.setText("Khuôn mặt đã được phát hiện!");
                    statusText.setTextColor(getColor(R.color.success));
                } else {
                    faceDetected = false;
                    statusText.setText("Không phát hiện khuôn mặt");
                    statusText.setTextColor(getColor(R.color.error));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Face detection failed", e);
            })
            .addOnCompleteListener(task -> {
                imageProxy.close();
            });
        */
    }

    private void captureImage() {
        // TODO: Capture image and upload to Firebase Storage
        showLoading(true);

        // Simulate capture
        new android.os.Handler().postDelayed(() -> {
            showLoading(false);
            faceDetected = true;
            
            statusText.setText("Ảnh đã được chụp thành công!");
            statusText.setTextColor(getColor(R.color.success));
            statusText.setVisibility(View.VISIBLE);
            
            btnCapture.setVisibility(View.GONE);
            btnRetake.setVisibility(View.VISIBLE);
            
            // Auto submit after capture
            submitEKYC();
            
            // Placeholder URL
            capturedImageUrl = "https://storage.googleapis.com/placeholder/face_" + 
                System.currentTimeMillis() + ".jpg";
        }, 2000);
    }

    private void retryCapture() {
        statusText.setVisibility(View.GONE);
        statusText.setText("Đặt khuôn mặt vào giữa khung hình");
        
        btnCapture.setVisibility(View.VISIBLE);
        btnRetake.setVisibility(View.GONE);
        
        faceDetected = false;
        capturedImageUrl = null;
    }

    private void submitEKYC() {
        if (!faceDetected || capturedImageUrl == null) {
            Toast.makeText(this, "Vui lòng chụp ảnh khuôn mặt trước", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

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
        // TODO: Clean up camera and face detector
        /*
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
        */
    }
}
