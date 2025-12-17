package com.example.cklbanking.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;

/**
 * Utility class để quản lý animations trong app
 */
public class AnimationHelper {
    
    /**
     * Áp dụng transition khi navigate giữa activities
     */
    public static void applyActivityTransition(AppCompatActivity activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    /**
     * Áp dụng transition khi quay lại activity trước
     */
    public static void applyBackTransition(AppCompatActivity activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    
    /**
     * Fade in animation cho view
     */
    public static void fadeIn(View view) {
        if (view == null) return;
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_in);
        view.startAnimation(animation);
        view.setVisibility(View.VISIBLE);
    }
    
    /**
     * Fade out animation cho view
     */
    public static void fadeOut(View view) {
        if (view == null) return;
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(animation);
    }
    
    /**
     * Scale in animation cho view (dùng cho success feedback)
     */
    public static void scaleIn(View view) {
        if (view == null) return;
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.scale_in);
        view.startAnimation(animation);
        view.setVisibility(View.VISIBLE);
    }
    
    /**
     * Scale out animation cho view
     */
    public static void scaleOut(View view) {
        if (view == null) return;
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.scale_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(animation);
    }
    
    /**
     * Pulse animation cho loading indicator
     */
    public static void startPulseAnimation(View view) {
        if (view == null) return;
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.pulse);
        view.startAnimation(animation);
    }
    
    /**
     * Stop pulse animation
     */
    public static void stopPulseAnimation(View view) {
        if (view == null) return;
        view.clearAnimation();
    }
    
    /**
     * Rotate animation cho loading spinner
     */
    public static void startRotateAnimation(View view) {
        if (view == null) return;
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.rotate);
        view.startAnimation(animation);
    }
    
    /**
     * Stop rotate animation
     */
    public static void stopRotateAnimation(View view) {
        if (view == null) return;
        view.clearAnimation();
    }
    
    /**
     * Success bounce animation
     */
    public static void showSuccessAnimation(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.success_bounce);
        view.startAnimation(animation);
    }
    
    /**
     * Error shake animation
     */
    public static void showErrorAnimation(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.error_shake);
        view.startAnimation(animation);
    }
    
    /**
     * Slide in từ bottom (dùng cho bottom sheets, dialogs)
     */
    public static void slideInFromBottom(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 
            view.getHeight(), 0f);
        animator.setDuration(300);
        animator.start();
    }
    
    /**
     * Slide out to bottom
     */
    public static void slideOutToBottom(View view, Runnable onComplete) {
        if (view == null) return;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 
            0f, view.getHeight());
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        animator.start();
    }
    
    /**
     * Fade in với delay
     */
    public static void fadeInWithDelay(View view, long delay) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(delay)
            .start();
    }
    
    /**
     * Animate loading với fade và pulse
     */
    public static void showLoadingWithAnimation(View loadingView) {
        if (loadingView == null) return;
        loadingView.setAlpha(0f);
        loadingView.setVisibility(View.VISIBLE);
        loadingView.animate()
            .alpha(1f)
            .setDuration(200)
            .start();
        startPulseAnimation(loadingView);
    }
    
    /**
     * Hide loading với fade
     */
    public static void hideLoadingWithAnimation(View loadingView) {
        if (loadingView == null) return;
        loadingView.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadingView.setVisibility(View.GONE);
                    stopPulseAnimation(loadingView);
                }
            })
            .start();
    }
}

