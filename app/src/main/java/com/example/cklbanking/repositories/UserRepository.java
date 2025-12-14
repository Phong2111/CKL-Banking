package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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

    // Dùng cho Officer: Cập nhật toàn bộ thông tin user
    public Task<Void> updateUser(String userId, User user) {
        return userCollection.document(userId).set(user);
    }

    // Dùng cho Officer: Lấy tất cả customers
    public Query getAllCustomers() {
        return userCollection.whereEqualTo("role", "customer");
    }

    // Dùng cho Officer: Tìm kiếm customer theo email
    public Query searchCustomerByEmail(String email) {
        return userCollection.whereEqualTo("role", "customer")
                .whereEqualTo("email", email);
    }

    // Dùng cho Officer: Tìm kiếm customer theo số điện thoại
    public Query searchCustomerByPhone(String phone) {
        return userCollection.whereEqualTo("role", "customer")
                .whereEqualTo("phone", phone);
    }

    // Dùng cho Officer: Tìm kiếm customer theo tên (fullName)
    public Query searchCustomerByName(String name) {
        return userCollection.whereEqualTo("role", "customer")
                .whereGreaterThanOrEqualTo("fullName", name)
                .whereLessThanOrEqualTo("fullName", name + "\uf8ff");
    }
}
