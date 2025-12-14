package com.example.cklbanking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cklbanking.R;
import com.example.cklbanking.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OfficerEKYCSupportActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final int STORAGE_PERMISSION_REQUEST = 1002;

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputLayout layoutCustomerSearch;
    private TextInputEditText editCustomerSearch;
    private MaterialButton btnSearchCustomer, btnUploadImage, btnVerifyEKYC;
    private TextView customerName, customerEmail, customerPhone, ekycStatusText;
    private ImageView faceImageView;
    private CircularProgressIndicator progressBar;
    private View customerInfoCard;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Data
    private String customerId;
    private User customerUser;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_ekyc_support);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Image Picker
        setupImagePicker();

        // Setup Listeners
        setupListeners();

        // Hide customer info initially
        customerInfoCard.setVisibility(View.GONE);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutCustomerSearch = findViewById(R.id.layoutCustomerSearch);
        editCustomerSearch = findViewById(R.id.editCustomerSearch);
        btnSearchCustomer = findViewById(R.id.btnSearchCustomer);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnVerifyEKYC = findViewById(R.id.btnVerifyEKYC);
        customerName = findViewById(R.id.customerName);
        customerEmail = findViewById(R.id.customerEmail);
        customerPhone = findViewById(R.id.customerPhone);
        ekycStatusText = findViewById(R.id.ekycStatusText);
        faceImageView = findViewById(R.id.faceImageView);
        progressBar = findViewById(R.id.progressBar);
        customerInfoCard = findViewById(R.id.customerInfoCard);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImageUri = imageUri;
                        faceImageView.setImageURI(imageUri);
                        btnUploadImage.setEnabled(true);
                    }
                }
            }
        );
    }

    private void setupListeners() {
        btnSearchCustomer.setOnClickListener(v -> searchCustomer());
        btnUploadImage.setOnClickListener(v -> selectImage());
        btnVerifyEKYC.setOnClickListener(v -> verifyEKYC());
    }

    private void searchCustomer() {
        String searchQuery = editCustomerSearch.getText().toString().trim();
        
        if (searchQuery.isEmpty()) {
            editCustomerSearch.setError("Vui lòng nhập số điện thoại hoặc email");
            editCustomerSearch.requestFocus();
            return;
        }

        showLoading(true);

        // Search by phone or email
        db.collection("users")
                .whereEqualTo("phone", searchQuery)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Found by phone
                        loadCustomerData(queryDocumentSnapshots.getDocuments().get(0).getId());
                    } else {
                        // Try email
                        db.collection("users")
                                .whereEqualTo("email", searchQuery)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(emailSnapshots -> {
                                    if (!emailSnapshots.isEmpty()) {
                                        loadCustomerData(emailSnapshots.getDocuments().get(0).getId());
                                    } else {
                                        showLoading(false);
                                        Toast.makeText(this, "Không tìm thấy khách hàng", 
                                            Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCustomerData(String userId) {
        customerId = userId;
        
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    
                    if (documentSnapshot.exists()) {
                        customerUser = documentSnapshot.toObject(User.class);
                        if (customerUser != null && "customer".equals(customerUser.getRole())) {
                            displayCustomerInfo();
                        } else {
                            Toast.makeText(this, "Người dùng này không phải là khách hàng", 
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin khách hàng", 
                            Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void displayCustomerInfo() {
        if (customerUser == null) return;

        customerName.setText(customerUser.getFullName());
        customerEmail.setText(customerUser.getEmail());
        customerPhone.setText(customerUser.getPhone() != null ? customerUser.getPhone() : "Chưa cập nhật");
        
        String status = customerUser.getEkycStatus();
        if (status == null) status = "pending";
        
        switch (status) {
            case "verified":
                ekycStatusText.setText("Đã xác thực");
                ekycStatusText.setTextColor(getColor(R.color.success));
                btnVerifyEKYC.setEnabled(false);
                break;
            case "failed":
                ekycStatusText.setText("Xác thực thất bại");
                ekycStatusText.setTextColor(getColor(R.color.error));
                btnVerifyEKYC.setEnabled(true);
                break;
            default:
                ekycStatusText.setText("Chưa xác thực");
                ekycStatusText.setTextColor(getColor(R.color.warning));
                btnVerifyEKYC.setEnabled(true);
                break;
        }

        customerInfoCard.setVisibility(View.VISIBLE);
    }

    private void selectImage() {
        if (customerId == null) {
            Toast.makeText(this, "Vui lòng tìm khách hàng trước", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Cần cấp quyền truy cập ảnh", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void verifyEKYC() {
        if (customerId == null) {
            Toast.makeText(this, "Vui lòng tìm khách hàng trước", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh khuôn mặt trước", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Upload image to Firebase Storage
        uploadFaceImage(selectedImageUri);
    }

    private void uploadFaceImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            if (bitmap == null) {
                showLoading(false);
                Toast.makeText(this, "Lỗi đọc ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            // Compress bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            // Upload to Firebase Storage
            String fileName = "ekyc/" + customerId + "/face_officer_" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = storage.getReference().child(fileName);

            UploadTask uploadTask = storageRef.putBytes(imageData);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateEKYCStatus(imageUrl);
                }).addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi lấy URL ảnh: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                showLoading(false);
                Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEKYCStatus(String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("ekycStatus", "verified");
        updates.put("faceImageUrl", imageUrl);
        updates.put("ekycVerifiedAt", com.google.firebase.Timestamp.now());
        updates.put("verifiedByOfficer", true);

        db.collection("users")
                .document(customerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Xác thực eKYC thành công!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Reload customer data
                    loadCustomerData(customerId);
                    
                    // Clear selected image
                    selectedImageUri = null;
                    faceImageView.setImageResource(android.R.color.transparent);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSearchCustomer.setEnabled(!show);
        btnUploadImage.setEnabled(!show && customerId != null);
        btnVerifyEKYC.setEnabled(!show && customerId != null && selectedImageUri != null);
    }
}


