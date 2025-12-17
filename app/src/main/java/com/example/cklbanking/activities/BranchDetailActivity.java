package com.example.cklbanking.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cklbanking.R;
import com.example.cklbanking.models.Branch;
import com.example.cklbanking.repositories.BranchRepository;
import com.example.cklbanking.utils.AnimationHelper;
import com.example.cklbanking.utils.BranchDistanceHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class BranchDetailActivity extends AppCompatActivity {
    
    private MaterialToolbar toolbar;
    private ImageView branchImage;
    private TextView branchName, branchAddress, branchPhone, branchHours, branchStatus, branchDistance;
    private ChipGroup chipGroupServices;
    private MaterialButton btnCall, btnGetDirections;
    private ExtendedFloatingActionButton fabFavorite;
    
    private Branch branch;
    private BranchRepository branchRepository;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private android.location.Location userLocation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_detail);
        
        // Get branch from intent
        branch = (Branch) getIntent().getSerializableExtra("branch");
        if (branch == null) {
            Toast.makeText(this, "Không tìm thấy thông tin chi nhánh", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get user location if available
        userLocation = (android.location.Location) getIntent().getParcelableExtra("user_location");
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        branchRepository = new BranchRepository();
        
        initViews();
        setupToolbar();
        setupListeners();
        loadBranchDetails();
        checkFavoriteStatus();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        branchImage = findViewById(R.id.branchImage);
        branchName = findViewById(R.id.branchName);
        branchAddress = findViewById(R.id.branchAddress);
        branchPhone = findViewById(R.id.branchPhone);
        branchHours = findViewById(R.id.branchHours);
        branchStatus = findViewById(R.id.branchStatus);
        branchDistance = findViewById(R.id.branchDistance);
        chipGroupServices = findViewById(R.id.chipGroupServices);
        btnCall = findViewById(R.id.btnCall);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        fabFavorite = findViewById(R.id.fabFavorite);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết chi nhánh");
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            AnimationHelper.applyBackTransition(this);
        });
    }
    
    private void setupListeners() {
        btnCall.setOnClickListener(v -> callBranch());
        btnGetDirections.setOnClickListener(v -> openNavigation());
        fabFavorite.setOnClickListener(v -> toggleFavorite());
    }
    
    private void loadBranchDetails() {
        // Branch name
        branchName.setText(branch.getName());
        
        // Address
        branchAddress.setText(branch.getAddress());
        
        // Phone
        if (branch.getPhoneNumber() != null && !branch.getPhoneNumber().isEmpty()) {
            branchPhone.setText(branch.getPhoneNumber());
            branchPhone.setVisibility(View.VISIBLE);
        } else {
            branchPhone.setVisibility(View.GONE);
        }
        
        // Opening hours
        if (branch.getOpeningHours() != null && !branch.getOpeningHours().isEmpty()) {
            branchHours.setText("Giờ mở cửa: " + branch.getOpeningHours());
            branchHours.setVisibility(View.VISIBLE);
        } else {
            branchHours.setVisibility(View.GONE);
        }
        
        // Status
        boolean isOpen = branch.isOpen();
        branchStatus.setText(isOpen ? "Đang mở cửa" : "Đã đóng cửa");
        branchStatus.setTextColor(getColor(isOpen ? R.color.success : R.color.error));
        
        // Distance
        if (userLocation != null) {
            double distance = BranchDistanceHelper.calculateDistance(userLocation, branch);
            branchDistance.setText(BranchDistanceHelper.formatDistance(distance));
            branchDistance.setVisibility(View.VISIBLE);
        } else {
            branchDistance.setVisibility(View.GONE);
        }
        
        // Services
        if (branch.getServices() != null && !branch.getServices().isEmpty()) {
            chipGroupServices.removeAllViews();
            for (String service : branch.getServices()) {
                Chip chip = new Chip(this);
                chip.setText(service);
                chip.setChipBackgroundColorResource(R.color.primary_light);
                chip.setTextColor(getColor(R.color.primary));
                chipGroupServices.addView(chip);
            }
            chipGroupServices.setVisibility(View.VISIBLE);
        } else {
            chipGroupServices.setVisibility(View.GONE);
        }
        
        // Image
        if (branch.getImageUrl() != null && !branch.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(branch.getImageUrl())
                    .placeholder(R.drawable.ic_bank)
                    .error(R.drawable.ic_bank)
                    .into(branchImage);
            branchImage.setVisibility(View.VISIBLE);
        } else {
            branchImage.setVisibility(View.GONE);
        }
    }
    
    private void checkFavoriteStatus() {
        if (userId == null) {
            fabFavorite.setVisibility(View.GONE);
            return;
        }
        
        db.collection("favorite_branches")
                .whereEqualTo("userId", userId)
                .whereEqualTo("branchId", branch.getBranchId())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isFavorite = !queryDocumentSnapshots.isEmpty();
                    branch.setFavorite(isFavorite);
                    updateFavoriteButton();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BranchDetail", "Error checking favorite", e);
                });
    }
    
    private void toggleFavorite() {
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu chi nhánh yêu thích", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean newFavoriteStatus = !branch.isFavorite();
        
        if (newFavoriteStatus) {
            // Add to favorites
            java.util.Map<String, Object> favorite = new java.util.HashMap<>();
            favorite.put("userId", userId);
            favorite.put("branchId", branch.getBranchId());
            favorite.put("branchName", branch.getName());
            favorite.put("createdAt", com.google.firebase.Timestamp.now());
            
            db.collection("favorite_branches")
                    .add(favorite)
                    .addOnSuccessListener(documentReference -> {
                        branch.setFavorite(true);
                        updateFavoriteButton();
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                        AnimationHelper.showSuccessAnimation(fabFavorite);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Remove from favorites
            db.collection("favorite_branches")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("branchId", branch.getBranchId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            queryDocumentSnapshots.getDocuments().get(0).getReference()
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        branch.setFavorite(false);
                                        updateFavoriteButton();
                                        Toast.makeText(this, "Đã xóa khỏi yêu thích", 
                                                Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Lỗi: " + e.getMessage(), 
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    
    private void updateFavoriteButton() {
        if (branch.isFavorite()) {
            fabFavorite.setText("Đã yêu thích");
            fabFavorite.setIconResource(R.drawable.ic_favorite);
            fabFavorite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.warning)));
        } else {
            fabFavorite.setText("Yêu thích");
            fabFavorite.setIconResource(R.drawable.ic_favorite);
            fabFavorite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
        }
    }
    
    private void callBranch() {
        if (branch.getPhoneNumber() == null || branch.getPhoneNumber().isEmpty()) {
            Toast.makeText(this, "Không có số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + branch.getPhoneNumber()));
        startActivity(intent);
    }
    
    private void openNavigation() {
        if (userLocation == null) {
            Toast.makeText(this, "Không thể xác định vị trí", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Open Google Maps navigation
        Uri gmmIntentUri = Uri.parse(String.format(
            "google.navigation:q=%f,%f",
            branch.getLatitude(), branch.getLongitude()
        ));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to web browser
            Uri webUri = Uri.parse(String.format(
                "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                branch.getLatitude(), branch.getLongitude()
            ));
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            startActivity(webIntent);
        }
    }
}

