package com.example.cklbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.NotificationAdapter;
import com.example.cklbanking.models.Notification;
import com.example.cklbanking.utils.ErrorHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerNotifications;
    private LinearLayout emptyState;
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Pagination
    private static final int PAGE_SIZE = 20;
    private com.google.firebase.firestore.QueryDocumentSnapshot lastDocument;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        loadNotifications();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        emptyState = findViewById(R.id.emptyState);

        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(this, notifications);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerNotifications.setLayoutManager(layoutManager);
        recyclerNotifications.setAdapter(adapter);
        
        // Add scroll listener for pagination
        recyclerNotifications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // Load more when user scrolls near the end
                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreNotifications();
                    }
                }
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadNotifications() {
        if (isLoading) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        isLoading = true;
        notifications.clear();
        lastDocument = null;
        hasMoreData = true;
        
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    notifications.addAll(queryDocumentSnapshots.toObjects(Notification.class));
                    
                    // Update last document for pagination
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    ErrorHandler.handleError(this, e, "Lỗi tải thông báo");
                    updateEmptyState();
                });
    }
    
    private void loadMoreNotifications() {
        if (isLoading || !hasMoreData || lastDocument == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        isLoading = true;
        
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastDocument)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    notifications.addAll(queryDocumentSnapshots.toObjects(Notification.class));
                    
                    // Update last document for pagination
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    ErrorHandler.handleError(this, e, "Lỗi tải thêm thông báo");
                });
    }
    
    private void updateEmptyState() {
        if (notifications.isEmpty()) {
            recyclerNotifications.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerNotifications.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
    
}
