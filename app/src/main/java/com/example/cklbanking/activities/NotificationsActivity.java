package com.example.cklbanking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.adapters.NotificationAdapter;
import com.example.cklbanking.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
        
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadNotifications() {
        String userId = mAuth.getCurrentUser().getUid();
        
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notifications.clear();
                    notifications.addAll(queryDocumentSnapshots.toObjects(Notification.class));
                    adapter.notifyDataSetChanged();
                    
                    if (notifications.isEmpty()) {
                        recyclerNotifications.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerNotifications.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    recyclerNotifications.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                });
    }
}
