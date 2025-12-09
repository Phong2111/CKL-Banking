package com.example.cklbanking.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private ShapeableImageView editProfileAvatar;
    private MaterialButton btnChangeAvatar, btnSaveProfile;
    private TextInputEditText editFullName, editEmail, editPhone;
    private TextInputEditText editDateOfBirth, editIdNumber, editAddress;
    private CircularProgressIndicator progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;
    private Calendar calendar;
    private Uri selectedImageUri;

    // Image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        calendar = Calendar.getInstance();

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
        editProfileAvatar = findViewById(R.id.editProfileAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        editFullName = findViewById(R.id.editFullName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editDateOfBirth = findViewById(R.id.editDateOfBirth);
        editIdNumber = findViewById(R.id.editIdNumber);
        editAddress = findViewById(R.id.editAddress);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
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
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        editProfileAvatar.setImageURI(selectedImageUri);
                    }
                }
            }
        );
    }

    private void setupListeners() {
        btnChangeAvatar.setOnClickListener(v -> selectImage());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        // Date picker for date of birth
        editDateOfBirth.setOnClickListener(v -> showDatePicker());
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
                        populateFields();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void populateFields() {
        if (currentUser == null) return;

        editFullName.setText(currentUser.getFullName());
        editEmail.setText(currentUser.getEmail());
        editPhone.setText(currentUser.getPhone());
        editDateOfBirth.setText("");
        editIdNumber.setText("");
        editAddress.setText("");

        // TODO: Load avatar
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                editDateOfBirth.setText(sdf.format(calendar.getTime()));
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }

    private void saveProfile() {
        // Validate input
        String fullName = editFullName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String dateOfBirth = editDateOfBirth.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            editFullName.setError("Họ tên không được để trống");
            editFullName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            editPhone.setError("Số điện thoại không được để trống");
            editPhone.requestFocus();
            return;
        }

        showLoading(true);

        // Update user data (only fields in User model)
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Cập nhật thành công!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Upload avatar if selected
                    if (selectedImageUri != null) {
                        uploadProfileImage();
                    } else {
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfileImage() {
        // TODO: Upload to Firebase Storage
        Toast.makeText(this, "Đang upload ảnh...", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!show);
    }
}
