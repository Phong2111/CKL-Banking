package com.example.cklbanking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.BranchAdapter;
import com.example.cklbanking.models.Branch;
import com.example.cklbanking.repositories.BranchRepository;
import com.example.cklbanking.utils.AnimationHelper;
import com.example.cklbanking.utils.BranchDistanceHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BranchLocatorActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MaterialToolbar toolbar;
    private WebView mapWebView;
    private FloatingActionButton fabMyLocation;
    private MaterialButton btnGetDirections;
    private MaterialButton btnCall;
    private TextView branchName, branchAddress, branchDistance, branchStatus;
    private RecyclerView branchesRecyclerView;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private List<Branch> branches;
    private List<Branch> filteredBranches; // Filtered and sorted branches
    private Branch nearestBranch;
    private BranchRepository branchRepository;
    private BranchAdapter branchAdapter;
    private boolean mapReady = false;
    
    // Filter UI components
    private ChipGroup chipGroupDistance, chipGroupType;
    private Chip chipDistanceAll, chipDistance1km, chipDistance5km, chipDistance10km;
    private Chip chipTypeAll, chipTypeBranch, chipTypeATM;
    private Chip chipOnlyOpen;
    
    // Filter values
    private double maxDistanceMeters = Double.MAX_VALUE; // Default: no limit
    private String filterType = "all"; // "all", "branch", "atm"
    private boolean onlyOpen = false;
    
    // User ID for favorite branches
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_locator);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        branchRepository = new BranchRepository();
        branches = new ArrayList<>();
        
        // Get user ID for favorite branches
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        initViews();
        setupRecyclerView();
        setupWebView();
        loadBranchesFromFirestore();
        checkLocationPermission();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        mapWebView = findViewById(R.id.mapWebView);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnCall = findViewById(R.id.btnCall);
        branchName = findViewById(R.id.branchName);
        branchAddress = findViewById(R.id.branchAddress);
        branchDistance = findViewById(R.id.branchDistance);
        branchStatus = findViewById(R.id.branchStatus);
        branchesRecyclerView = findViewById(R.id.branchesRecyclerView);
        
        // Filter UI
        chipGroupDistance = findViewById(R.id.chipGroupDistance);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipDistanceAll = findViewById(R.id.chipDistanceAll);
        chipDistance1km = findViewById(R.id.chipDistance1km);
        chipDistance5km = findViewById(R.id.chipDistance5km);
        chipDistance10km = findViewById(R.id.chipDistance10km);
        chipTypeAll = findViewById(R.id.chipTypeAll);
        chipTypeBranch = findViewById(R.id.chipTypeBranch);
        chipTypeATM = findViewById(R.id.chipTypeATM);
        chipOnlyOpen = findViewById(R.id.chipOnlyOpen);
        
        filteredBranches = new ArrayList<>();
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tìm chi nhánh");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        fabMyLocation.setOnClickListener(v -> centerMapOnUserLocation());
        btnGetDirections.setOnClickListener(v -> openNavigation());
        btnCall.setOnClickListener(v -> callBranch());
        
        setupFilterListeners();
    }
    
    private void setupFilterListeners() {
        // Distance filter
        chipDistanceAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                maxDistanceMeters = Double.MAX_VALUE;
                applyFilters();
            }
        });
        chipDistance1km.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                maxDistanceMeters = 1000; // 1 km
                applyFilters();
            }
        });
        chipDistance5km.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                maxDistanceMeters = 5000; // 5 km
                applyFilters();
            }
        });
        chipDistance10km.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                maxDistanceMeters = 10000; // 10 km
                applyFilters();
            }
        });
        
        // Type filter
        chipTypeAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                filterType = "all";
                applyFilters();
            }
        });
        chipTypeBranch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                filterType = "branch";
                applyFilters();
            }
        });
        chipTypeATM.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                filterType = "atm";
                applyFilters();
            }
        });
        
        // Only open filter
        chipOnlyOpen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlyOpen = isChecked;
            applyFilters();
        });
    }
    
    /**
     * Áp dụng tất cả filters và sắp xếp danh sách
     */
    private void applyFilters() {
        if (branches == null || branches.isEmpty()) {
            return;
        }
        
        // Start with all branches
        filteredBranches = new ArrayList<>(branches);
        
        // Filter by distance
        if (currentLocation != null && maxDistanceMeters < Double.MAX_VALUE) {
            filteredBranches = BranchDistanceHelper.filterByDistance(
                filteredBranches, currentLocation, maxDistanceMeters);
        }
        
        // Filter by type
        if (!"all".equals(filterType)) {
            filteredBranches = BranchDistanceHelper.filterByType(filteredBranches, filterType);
        }
        
        // Filter by open status
        if (onlyOpen) {
            filteredBranches = BranchDistanceHelper.filterByOpenStatus(filteredBranches, true);
        }
        
        // Sort by distance
        if (currentLocation != null) {
            BranchDistanceHelper.sortByDistance(filteredBranches, currentLocation);
        }
        
        // Update adapter
        branchAdapter.updateBranches(filteredBranches);
        
        // Update map markers
        if (mapReady) {
            addBranchMarkers();
        }
        
        // Update nearest branch
        if (currentLocation != null && !filteredBranches.isEmpty()) {
            findNearestBranch();
        }
    }

    private void setupWebView() {
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // Enable console logging for debugging
        mapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                android.util.Log.d("WebView", consoleMessage.message() + " -- From line "
                    + consoleMessage.lineNumber() + " of "
                    + consoleMessage.sourceId());
                return true;
            }
        });
        
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject API key ngay sau khi page load
                // Đợi một chút để đảm bảo script đã load xong
                view.postDelayed(() -> {
                    String apiKey = getString(R.string.goong_api_key);
                    // Inject API key và khởi tạo map
                    String js = "window.setGoongAPIKey('" + apiKey + "');";
                    view.evaluateJavascript(js, null);
                    android.util.Log.d("BranchLocator", "API key injected: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
                }, 500);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                android.util.Log.e("WebView", "Error: " + description + " - URL: " + failingUrl);
            }
        });
        
        // Add JavaScript interface
        mapWebView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        // Load HTML file
        mapWebView.loadUrl("file:///android_asset/goong_map.html");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onMapReady() {
            runOnUiThread(() -> {
                mapReady = true;
                if (branches != null && !branches.isEmpty()) {
                    addBranchMarkers();
                }
                if (currentLocation != null) {
                    addUserMarker();
                    findNearestBranch();
                }
            });
        }
    }

    private void setupRecyclerView() {
        branchAdapter = new BranchAdapter(this, filteredBranches);
        branchAdapter.setOnBranchClickListener(branch -> {
            // Open branch detail activity
            Intent intent = new Intent(this, BranchDetailActivity.class);
            intent.putExtra("branch", branch);
            if (currentLocation != null) {
                intent.putExtra("user_location", currentLocation);
            }
            startActivity(intent);
            AnimationHelper.applyActivityTransition(this);
        });
        
        branchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        branchesRecyclerView.setAdapter(branchAdapter);
    }

    private void loadBranchesFromFirestore() {
        branchRepository.getAllBranches()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    branches.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Branch branch = document.toObject(Branch.class);
                        branch.setBranchId(document.getId());
                        branches.add(branch);
                    }
                    
                    if (branches.isEmpty()) {
                        initializeSampleBranches();
                    }
                    
                    // Load favorite status for all branches
                    loadFavoriteStatus();
                    
                    // Apply filters and sort
                    applyFilters();
                    branchAdapter.setUserLocation(currentLocation);
                    
                    if (mapReady) {
                        addBranchMarkers();
                    }
                    
                    if (currentLocation != null) {
                        findNearestBranch();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh sách chi nhánh: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    initializeSampleBranches();
                    loadFavoriteStatus();
                    applyFilters();
                    if (mapReady) {
                        addBranchMarkers();
                    }
                });
    }

    private void initializeSampleBranches() {
        // Chi nhánh Quận 1
        Branch branch1 = new Branch("Chi nhánh Quận 1", "123 Nguyễn Huệ, P.Bến Nghé, Q.1, TP.HCM", 
            10.7769, 106.7009, "02838291234", "8:00 - 17:00", "branch");
        branch1.setBranchId("branch_1");
        java.util.List<String> services1 = new ArrayList<>();
        services1.add("Gửi tiết kiệm");
        services1.add("Vay vốn");
        services1.add("Tư vấn tài chính");
        services1.add("Chuyển khoản");
        branch1.setServices(services1);
        branches.add(branch1);
        
        // Chi nhánh Quận 3
        Branch branch2 = new Branch("Chi nhánh Quận 3", "456 Lê Văn Sỹ, P.12, Q.3, TP.HCM", 
            10.7929, 106.6900, "02838291235", "8:00 - 17:00", "branch");
        branch2.setBranchId("branch_2");
        java.util.List<String> services2 = new ArrayList<>();
        services2.add("Gửi tiết kiệm");
        services2.add("Vay vốn");
        services2.add("Tư vấn tài chính");
        branch2.setServices(services2);
        branches.add(branch2);
        
        // Chi nhánh Quận 7
        Branch branch3 = new Branch("Chi nhánh Quận 7", "789 Nguyễn Thị Thập, P.Tân Phú, Q.7, TP.HCM", 
            10.7299, 106.7219, "02838291236", "8:00 - 17:00", "branch");
        branch3.setBranchId("branch_3");
        java.util.List<String> services3 = new ArrayList<>();
        services3.add("Gửi tiết kiệm");
        services3.add("Vay vốn");
        branch3.setServices(services3);
        branches.add(branch3);
        
        // ATM Quận 1
        Branch atm1 = new Branch("ATM Quận 1", "321 Đồng Khởi, Q.1, TP.HCM", 
            10.7756, 106.7019, "02838291237", "24/7", "atm");
        atm1.setBranchId("atm_1");
        java.util.List<String> atmServices1 = new ArrayList<>();
        atmServices1.add("Rút tiền");
        atmServices1.add("Chuyển khoản");
        atm1.setServices(atmServices1);
        branches.add(atm1);
        
        // ATM Quận 2
        Branch atm2 = new Branch("ATM Quận 2", "654 Nguyễn Duy Trinh, Q.2, TP.HCM", 
            10.7874, 106.7493, "02838291238", "24/7", "atm");
        atm2.setBranchId("atm_2");
        java.util.List<String> atmServices2 = new ArrayList<>();
        atmServices2.add("Rút tiền");
        atmServices2.add("Chuyển khoản");
        atm2.setServices(atmServices2);
        branches.add(atm2);
    }
    
    /**
     * Load favorite status for all branches
     */
    private void loadFavoriteStatus() {
        if (userId == null || branches == null || branches.isEmpty()) {
            return;
        }
        
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("favorite_branches")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.Set<String> favoriteBranchIds = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String branchId = document.getString("branchId");
                        if (branchId != null) {
                            favoriteBranchIds.add(branchId);
                        }
                    }
                    
                    // Update favorite status for all branches
                    for (Branch branch : branches) {
                        branch.setFavorite(favoriteBranchIds.contains(branch.getBranchId()));
                    }
                    
                    // Notify adapter
                    if (branchAdapter != null) {
                        branchAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BranchLocator", "Error loading favorite status", e);
                });
    }

    private void showBranchOnMap(Branch branch) {
        if (!mapReady) return;
        
        String js = String.format("window.moveToLocation(%f, %f, 15);", 
            branch.getLatitude(), branch.getLongitude());
        mapWebView.evaluateJavascript(js, null);
        
        if (currentLocation != null) {
            nearestBranch = branch;
            drawRouteToNearestBranch();
            double distance = BranchDistanceHelper.calculateDistance(currentLocation, branch);
            updateNearestBranchUI(distance);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí để hiển thị bản đồ", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }
    
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        // Nếu đang chạy trên emulator, set location mặc định là TP.HCM, Việt Nam
        if (isEmulator()) {
            android.util.Log.d("BranchLocator", "Running on emulator, using default location: TP.HCM, Vietnam");
            Location emulatorLocation = new Location("emulator");
            emulatorLocation.setLatitude(10.8231);  // TP.HCM
            emulatorLocation.setLongitude(106.6297);
            emulatorLocation.setAccuracy(50.0f);
            emulatorLocation.setTime(System.currentTimeMillis());
            
            currentLocation = emulatorLocation;
            
            if (mapReady) {
                addUserMarker();
                centerMapOnUserLocation();
            }
            findNearestBranch();
            if (branchAdapter != null) {
                branchAdapter.setUserLocation(emulatorLocation);
                applyFilters();
            }
            
            // Vẫn thử lấy location thật nếu có (cho trường hợp dùng Extended Controls)
            // Nhưng không block nếu không có
        }
        
        // Tạo LocationRequest để lấy vị trí real-time từ GPS
        if (locationRequest == null) {
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000); // Update mỗi 10 giây
            locationRequest.setFastestInterval(5000); // Tối thiểu 5 giây
        }
        
        // Tạo LocationCallback
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        Location location = locationResult.getLastLocation();
                        
                        // Kiểm tra xem location có hợp lệ không (không phải emulator default)
                        // Emulator thường trả về 37.4219983, -122.084 (Mountain View, CA)
                        if (location.getLatitude() == 37.4219983 && location.getLongitude() == -122.084) {
                            android.util.Log.d("BranchLocator", "Detected emulator default location, using TP.HCM instead");
                            // Nếu là emulator default location, dùng location Việt Nam
                            if (currentLocation == null || isEmulator()) {
                                Location vietnamLocation = new Location("emulator");
                                vietnamLocation.setLatitude(10.8231);
                                vietnamLocation.setLongitude(106.6297);
                                vietnamLocation.setAccuracy(50.0f);
                                vietnamLocation.setTime(System.currentTimeMillis());
                                currentLocation = vietnamLocation;
                                location = vietnamLocation;
                            } else {
                                // Giữ location hiện tại nếu đã có
                                return;
                            }
                        } else {
                            currentLocation = location;
                        }
                        
                        android.util.Log.d("BranchLocator", "Location updated: " + 
                            location.getLatitude() + ", " + location.getLongitude());
                        
                        if (mapReady) {
                            addUserMarker();
                            // Center map vào vị trí user
                            centerMapOnUserLocation();
                        }
                        findNearestBranch();
                        if (branchAdapter != null) {
                            branchAdapter.setUserLocation(location);
                            // Re-apply filters when location updates
                            applyFilters();
                        }
                    }
                }
            };
        }
        
        // Thử lấy vị trí cuối cùng trước (nhanh hơn)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Kiểm tra xem có phải emulator default location không
                        if (location.getLatitude() == 37.4219983 && location.getLongitude() == -122.084) {
                            android.util.Log.d("BranchLocator", "Detected emulator default location, using TP.HCM instead");
                            Location vietnamLocation = new Location("emulator");
                            vietnamLocation.setLatitude(10.8231);
                            vietnamLocation.setLongitude(106.6297);
                            vietnamLocation.setAccuracy(50.0f);
                            vietnamLocation.setTime(System.currentTimeMillis());
                            currentLocation = vietnamLocation;
                            location = vietnamLocation;
                        } else {
                            currentLocation = location;
                        }
                        
                        android.util.Log.d("BranchLocator", "Last location: " + 
                            location.getLatitude() + ", " + location.getLongitude());
                        
                        if (mapReady) {
                            addUserMarker();
                            centerMapOnUserLocation();
                        }
                        findNearestBranch();
                        if (branchAdapter != null) {
                            branchAdapter.setUserLocation(location);
                            applyFilters();
                        }
                    } else if (isEmulator() && currentLocation == null) {
                        // Nếu không có location và đang chạy emulator, dùng location mặc định
                        android.util.Log.d("BranchLocator", "No location available on emulator, using default: TP.HCM");
                        Location vietnamLocation = new Location("emulator");
                        vietnamLocation.setLatitude(10.8231);
                        vietnamLocation.setLongitude(106.6297);
                        vietnamLocation.setAccuracy(50.0f);
                        vietnamLocation.setTime(System.currentTimeMillis());
                        currentLocation = vietnamLocation;
                        
                        if (mapReady) {
                            addUserMarker();
                            centerMapOnUserLocation();
                        }
                        findNearestBranch();
                        if (branchAdapter != null) {
                            branchAdapter.setUserLocation(vietnamLocation);
                            applyFilters();
                        }
                    }
                });
        
        // Bắt đầu request location updates để lấy vị trí real-time
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            android.util.Log.e("BranchLocator", "Error requesting location updates", e);
        }
    }

    private void addUserMarker() {
        if (currentLocation == null || !mapReady) return;
        
        String js = String.format("window.addUserMarker(%f, %f);", 
            currentLocation.getLatitude(), currentLocation.getLongitude());
        mapWebView.evaluateJavascript(js, null);
    }

    private void addBranchMarkers() {
        if (!mapReady || filteredBranches == null) return;
        
        // Clear existing markers
        mapWebView.evaluateJavascript("window.clearMarkers();", null);
        
        // Add filtered branch markers
        for (Branch branch : filteredBranches) {
            String js = String.format(
                "window.addBranchMarker(%f, %f, '%s', '%s', '%s');",
                branch.getLatitude(), branch.getLongitude(),
                branch.getName().replace("'", "\\'"),
                branch.getAddress().replace("'", "\\'"),
                branch.getType()
            );
            mapWebView.evaluateJavascript(js, null);
        }
    }

    private void findNearestBranch() {
        if (currentLocation == null || filteredBranches == null || filteredBranches.isEmpty()) return;
        
        // Since filteredBranches is already sorted by distance, first item is nearest
        Branch nearest = filteredBranches.get(0);
        double distance = BranchDistanceHelper.calculateDistance(currentLocation, nearest);
        
        nearestBranch = nearest;
        updateNearestBranchUI(distance);
        if (mapReady) {
            drawRouteToNearestBranch();
        }
    }

    private void updateNearestBranchUI(double distanceInMeters) {
        if (nearestBranch == null) return;
        
        branchName.setText(nearestBranch.getName());
        branchAddress.setText(nearestBranch.getAddress());
        branchDistance.setText(BranchDistanceHelper.formatDistance(distanceInMeters));
        
        branchStatus.setText(nearestBranch.isOpen() ? "Đang mở cửa" : "Đã đóng cửa");
        branchStatus.setTextColor(getColor(nearestBranch.isOpen() ? R.color.success : R.color.error));
    }

    private void drawRouteToNearestBranch() {
        if (!mapReady || currentLocation == null || nearestBranch == null) return;
        
        String js = String.format(
            "window.drawRoute(%f, %f, %f, %f);",
            currentLocation.getLatitude(), currentLocation.getLongitude(),
            nearestBranch.getLatitude(), nearestBranch.getLongitude()
        );
        mapWebView.evaluateJavascript(js, null);
    }

    private void centerMapOnUserLocation() {
        if (currentLocation == null) {
            getCurrentLocation();
            return;
        }
        
        if (!mapReady) return;
        
        // Center map vào vị trí user với zoom phù hợp
        String js = String.format("window.moveToLocation(%f, %f, 15);", 
            currentLocation.getLatitude(), currentLocation.getLongitude());
        mapWebView.evaluateJavascript(js, null);
        
        android.util.Log.d("BranchLocator", "Centering map on user location: " + 
            currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Dừng location updates khi activity pause để tiết kiệm pin
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Tiếp tục location updates khi activity resume
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
        
        // Reload favorite status when returning from BranchDetailActivity
        if (branches != null && !branches.isEmpty()) {
            loadFavoriteStatus();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng location updates khi activity destroy
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void openNavigation() {
        if (nearestBranch == null || currentLocation == null) {
            Toast.makeText(this, "Không tìm thấy chi nhánh hoặc vị trí", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Hiển thị route đi bộ trên map hiện tại thay vì mở app bên ngoài
        // Sử dụng Goong Directions API để lấy route thực tế
        getWalkingRoute(currentLocation.getLatitude(), currentLocation.getLongitude(),
            nearestBranch.getLatitude(), nearestBranch.getLongitude());
    }
    
    private void getWalkingRoute(double originLat, double originLng, 
                                 double destLat, double destLng) {
        // Tính khoảng cách trực tiếp để kiểm tra
        float[] distanceResults = new float[1];
        Location.distanceBetween(originLat, originLng, destLat, destLng, distanceResults);
        double directDistance = distanceResults[0]; // meters
        
        android.util.Log.d("BranchLocator", String.format(
            "Calculating route from (%.6f, %.6f) to (%.6f, %.6f), direct distance: %.2f km",
            originLat, originLng, destLat, destLng, directDistance / 1000));
        
        // Nếu khoảng cách quá xa (> 50km), có thể Goong Directions API không hỗ trợ
        // Hoặc không hợp lý để đi bộ, vẽ đường thẳng thay vì tính route
        if (directDistance > 50000) { // 50km
            runOnUiThread(() -> {
                Toast.makeText(this, 
                    String.format("Khoảng cách quá xa (%.2f km). Hiển thị đường thẳng.", directDistance / 1000),
                    Toast.LENGTH_LONG).show();
                drawRouteToNearestBranch();
            });
            return;
        }
        
        // Sử dụng Goong Directions API để lấy route đi bộ
        String apiKey = getString(R.string.goong_api_key);
        String url = String.format(
            "https://rsapi.goong.io/Direction?origin=%f,%f&destination=%f,%f&vehicle=walk&api_key=%s",
            originLat, originLng, destLat, destLng, apiKey
        );
        
        android.util.Log.d("BranchLocator", "Requesting route from Goong API: " + url.replace(apiKey, "API_KEY"));
        
        // Hiển thị loading
        runOnUiThread(() -> {
            Toast.makeText(this, "Đang tính toán đường đi...", Toast.LENGTH_SHORT).show();
        });
        
        new Thread(() -> {
            try {
                java.net.URL apiUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000); // 10 seconds
                conn.setReadTimeout(10000); // 10 seconds
                
                int responseCode = conn.getResponseCode();
                android.util.Log.d("BranchLocator", "Goong API response code: " + responseCode);
                
                String responseBody;
                if (responseCode == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    responseBody = response.toString();
                } else {
                    // Đọc error response
                    java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    responseBody = response.toString();
                    android.util.Log.e("BranchLocator", "Goong API error response: " + responseBody);
                }
                
                if (responseCode == 200) {
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    // Kiểm tra xem có routes không
                    if (!jsonResponse.has("routes") || jsonResponse.isNull("routes")) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "API không trả về đường đi", Toast.LENGTH_SHORT).show();
                            drawRouteToNearestBranch();
                        });
                        return;
                    }
                    
                    JSONObject routesObj = jsonResponse.getJSONObject("routes");
                    if (!routesObj.has("features") || routesObj.isNull("features")) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Không tìm thấy đường đi", Toast.LENGTH_SHORT).show();
                            drawRouteToNearestBranch();
                        });
                        return;
                    }
                    
                    JSONArray routes = routesObj.getJSONArray("features");
                    
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject geometry = route.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        
                        // Lấy thông tin route
                        JSONObject properties = route.getJSONObject("properties");
                        double distance = properties.getDouble("distance"); // meters
                        double duration = properties.getDouble("duration"); // seconds
                        
                        android.util.Log.d("BranchLocator", String.format(
                            "Route found: distance=%.2f km, duration=%.0f seconds, points=%d",
                            distance / 1000, duration, coordinates.length()));
                        
                        // Chuyển đổi coordinates từ [lng, lat] sang [lat, lng] cho Leaflet
                        StringBuilder coordsStr = new StringBuilder("[");
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            double lng = coord.getDouble(0);
                            double lat = coord.getDouble(1);
                            if (i > 0) coordsStr.append(",");
                            coordsStr.append("[").append(lat).append(",").append(lng).append("]");
                        }
                        coordsStr.append("]");
                        
                        // Vẽ route trên map
                        runOnUiThread(() -> {
                            String js = String.format(
                                "window.drawWalkingRoute(%s, %f, %f);",
                                coordsStr.toString(), distance, duration
                            );
                            mapWebView.evaluateJavascript(js, null);
                            
                            // Hiển thị thông tin route
                            String distanceText = distance < 1000 ? 
                                String.format("%.0f m", distance) : 
                                String.format("%.2f km", distance / 1000);
                            int minutes = (int) (duration / 60);
                            String timeText = minutes < 60 ? 
                                String.format("%d phút", minutes) : 
                                String.format("%d giờ %d phút", minutes / 60, minutes % 60);
                            
                            Toast.makeText(this, 
                                String.format("Khoảng cách: %s | Thời gian: %s", distanceText, timeText),
                                Toast.LENGTH_LONG).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Không tìm thấy đường đi", Toast.LENGTH_SHORT).show();
                            drawRouteToNearestBranch();
                        });
                    }
                } else {
                    // Parse error response nếu có
                    String errorMsg = "Lỗi khi tính toán đường đi";
                    try {
                        JSONObject errorResponse = new JSONObject(responseBody);
                        if (errorResponse.has("error")) {
                            errorMsg = errorResponse.getString("error");
                        } else if (errorResponse.has("message")) {
                            errorMsg = errorResponse.getString("message");
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    
                    final String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> {
                        android.util.Log.e("BranchLocator", "Goong API error: " + finalErrorMsg);
                        Toast.makeText(this, finalErrorMsg + ". Hiển thị đường thẳng.", Toast.LENGTH_LONG).show();
                        drawRouteToNearestBranch();
                    });
                }
            } catch (java.net.SocketTimeoutException e) {
                runOnUiThread(() -> {
                    android.util.Log.e("BranchLocator", "Timeout getting route", e);
                    Toast.makeText(this, "Timeout khi tính toán đường đi. Hiển thị đường thẳng.", Toast.LENGTH_LONG).show();
                    drawRouteToNearestBranch();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    android.util.Log.e("BranchLocator", "Error getting route", e);
                    Toast.makeText(this, "Lỗi: " + e.getMessage() + ". Hiển thị đường thẳng.", Toast.LENGTH_LONG).show();
                    drawRouteToNearestBranch();
                });
            }
        }).start();
    }

    private void callBranch() {
        if (nearestBranch == null || nearestBranch.getPhoneNumber() == null) {
            Toast.makeText(this, "Không có số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + nearestBranch.getPhoneNumber()));
        startActivity(intent);
    }
}
