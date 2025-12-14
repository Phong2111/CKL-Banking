package com.example.cklbanking.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.cklbanking.models.Branch;

public class BranchRepository {

    private static final String COLLECTION_NAME = "branches";
    private final CollectionReference branchCollection;

    public BranchRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.branchCollection = db.collection(COLLECTION_NAME);
    }

    // Lấy tất cả branches
    public Query getAllBranches() {
        return branchCollection.orderBy("name");
    }

    // Lấy branches theo loại (branch hoặc atm)
    public Query getBranchesByType(String type) {
        return branchCollection
                .whereEqualTo("type", type)
                .orderBy("name");
    }

    // Tạo branch mới (dành cho admin)
    public Task<Void> createBranch(Branch branch) {
        String newBranchId = branchCollection.document().getId();
        branch.setBranchId(newBranchId);
        return branchCollection.document(newBranchId).set(branch);
    }

    // Cập nhật branch
    public Task<Void> updateBranch(String branchId, Branch branch) {
        return branchCollection.document(branchId).set(branch);
    }

    // Xóa branch (dành cho admin)
    public Task<Void> deleteBranch(String branchId) {
        return branchCollection.document(branchId).delete();
    }
}




