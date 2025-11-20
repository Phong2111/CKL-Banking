package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.google.android.material.appbar.MaterialToolbar;

public class UtilitiesActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private LinearLayout btnElectricityBill, btnWaterBill, btnInternetBill;
    private LinearLayout btnPhoneRecharge, btnDataPackage, btnTvCard;
    private LinearLayout btnFlightTicket, btnMovieTicket, btnHotelBooking, btnEcommerce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utilities);

        initViews();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        
        // Bill payments
        btnElectricityBill = findViewById(R.id.btnElectricityBill);
        btnWaterBill = findViewById(R.id.btnWaterBill);
        btnInternetBill = findViewById(R.id.btnInternetBill);
        
        // Mobile services
        btnPhoneRecharge = findViewById(R.id.btnPhoneRecharge);
        btnDataPackage = findViewById(R.id.btnDataPackage);
        btnTvCard = findViewById(R.id.btnTvCard);
        
        // Booking services
        btnFlightTicket = findViewById(R.id.btnFlightTicket);
        btnMovieTicket = findViewById(R.id.btnMovieTicket);
        btnHotelBooking = findViewById(R.id.btnHotelBooking);
        btnEcommerce = findViewById(R.id.btnEcommerce);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tiện ích");
        }
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bill payments
        btnElectricityBill.setOnClickListener(v -> 
            showFeatureInDevelopment("Thanh toán tiền điện"));
        btnWaterBill.setOnClickListener(v -> 
            showFeatureInDevelopment("Thanh toán tiền nước"));
        btnInternetBill.setOnClickListener(v -> 
            showFeatureInDevelopment("Thanh toán tiền internet"));
        
        // Mobile services
        btnPhoneRecharge.setOnClickListener(v -> 
            showFeatureInDevelopment("Nạp tiền điện thoại"));
        btnDataPackage.setOnClickListener(v -> 
            showFeatureInDevelopment("Mua gói data"));
        btnTvCard.setOnClickListener(v -> 
            showFeatureInDevelopment("Mua thẻ truyền hình"));
        
        // Booking services
        btnFlightTicket.setOnClickListener(v -> 
            showFeatureInDevelopment("Đặt vé máy bay"));
        btnMovieTicket.setOnClickListener(v -> 
            showFeatureInDevelopment("Đặt vé xem phim"));
        btnHotelBooking.setOnClickListener(v -> 
            showFeatureInDevelopment("Đặt khách sạn"));
        btnEcommerce.setOnClickListener(v -> 
            showFeatureInDevelopment("Mua sắm online"));
    }

    private void showFeatureInDevelopment(String featureName) {
        Toast.makeText(this, featureName + " đang được phát triển", Toast.LENGTH_SHORT).show();
    }
}
