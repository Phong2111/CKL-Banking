package com.example.cklbanking.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerProfileActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private ShapeableImageView profileAvatar;
    private TextView profileName, profileEmail, profilePhone, profileDob;
    private TextView profileIdNumber, profileAddress;
    private MaterialButton btnEditProfile;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;

    // Image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Initialize Views
        initViews();

        // Setup Toolbar
        setupToolbar();

        // Setup Image Picker
        setupImagePicker();

        // Setup Listeners
        setupListeners();

        // Load user data
        loadUserProfile();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        profileAvatar = findViewById(R.id.profileAvatar);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        profileDob = findViewById(R.id.profileDob);
        profileIdNumber = findViewById(R.id.profileIdNumber);
        profileAddress = findViewById(R.id.profileAddress);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        progressBar = findViewById(R.id.progressBar);
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
                        profileAvatar.setImageURI(imageUri);
                        // TODO: Upload to Firebase Storage
                        uploadProfileImage(imageUri);
                    }
                }
            }
        );
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> openEditProfile());
        profileAvatar.setOnClickListener(v -> selectImage());
    }

    private void loadUserProfile() {
        showLoading(true);

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        updateUI();
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", 
                            Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        if (currentUser == null) return;

        profileName.setText(currentUser.getFullName());
        profileEmail.setText(currentUser.getEmail());
        profilePhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "Chưa cập nhật");
        // Extended fields not in User model
        profileDob.setText("Chưa cập nhật");
        profileIdNumber.setText("Chưa cập nhật");
        profileAddress.setText("Chưa cập nhật");

        // TODO: Load avatar from URL using Glide
        // if (currentUser.getAvatarUrl() != null) {
        //     Glide.with(this)
        //         .load(currentUser.getAvatarUrl())
        //         .into(profileAvatar);
        // }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        // TODO: Upload to Firebase Storage
        Toast.makeText(this, "Chức năng upload ảnh đang phát triển", 
            Toast.LENGTH_SHORT).show();
    }

    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile when returning from edit
        loadUserProfile();
    }
}
