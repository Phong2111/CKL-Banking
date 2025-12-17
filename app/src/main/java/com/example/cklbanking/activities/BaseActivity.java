package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.utils.AnimationHelper;

/**
 * Base Activity với animations và common functionality
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        AnimationHelper.applyActivityTransition(this);
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        AnimationHelper.applyActivityTransition(this);
    }
    
    @Override
    public void finish() {
        super.finish();
        AnimationHelper.applyBackTransition(this);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationHelper.applyBackTransition(this);
    }
    
    /**
     * Show loading với animation
     */
    protected void showLoading(View loadingView) {
        if (loadingView != null) {
            AnimationHelper.showLoadingWithAnimation(loadingView);
        }
    }
    
    /**
     * Hide loading với animation
     */
    protected void hideLoading(View loadingView) {
        if (loadingView != null) {
            AnimationHelper.hideLoadingWithAnimation(loadingView);
        }
    }
    
    /**
     * Show success message với animation
     */
    protected void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show error message với animation
     */
    protected void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}

