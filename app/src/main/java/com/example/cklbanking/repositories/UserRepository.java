package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.cklbanking.models.User; // Import model User

public class UserRepository {

    private static final String COLLECTION_NAME = "users";
    private final CollectionReference userCollection;

    public UserRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.userCollection = db.collection(COLLECTION_NAME);
    }

    // Dùng khi Đăng ký
    public Task<Void> createUser(User user) {
        // Dùng user.getUid() làm Document ID
        return userCollection.document(user.getUserId()).set(user);
    }

    // Dùng khi Đăng nhập để lấy thông tin (đặc biệt là 'role')
    public Task<DocumentSnapshot> getUser(String userId) {
        return userCollection.document(userId).get();
    }

    // Dùng cho các chức năng Cập nhật thông tin
    // (Bạn cũng có thể tạo các hàm cụ thể hơn)
    public Task<Void> updateUserField(String userId, String field, Object value) {
        return userCollection.document(userId).update(field, value);
    }
}
