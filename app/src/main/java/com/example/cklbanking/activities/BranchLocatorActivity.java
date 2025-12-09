package com.example.cklbanking.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cklbanking.R;
import com.google.android.material.appbar.MaterialToolbar;

public class BranchLocatorActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_locator);

        initViews();
        checkLocationPermission();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi nhánh & ATM");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted
            initializeMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                initializeMap();
            } else {
                // Permission denied
                Toast.makeText(this, "Cần cấp quyền vị trí để hiển thị bản đồ", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeMap() {
        // TODO: Initialize Google Maps
        // For now, show a placeholder message
        Toast.makeText(this, "Bản đồ đang được phát triển. Vui lòng cấu hình Google Maps API Key.", 
            Toast.LENGTH_LONG).show();
        
        // Note: To implement Google Maps:
        // 1. Get a Google Maps API key from Google Cloud Console
        // 2. Add the key to AndroidManifest.xml
        // 3. Uncomment the map fragment in activity_branch_locator.xml
        // 4. Implement OnMapReadyCallback and add markers for branches
    }
}
