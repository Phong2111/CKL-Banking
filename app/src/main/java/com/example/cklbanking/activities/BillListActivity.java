package com.example.cklbanking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Bill;
import com.example.cklbanking.repositories.BillRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity hiển thị danh sách hóa đơn chưa thanh toán
 */
public class BillListActivity extends AppCompatActivity {
    
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewBills;
    private TextView textEmptyState;
    
    private BillRepository billRepository;
    private BillAdapter billAdapter;
    private List<Bill> bills;
    private String filterType; // "electricity" or "water" from Intent
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_list);
        
        billRepository = new BillRepository();
        bills = new ArrayList<>();
        filterType = getIntent().getStringExtra("filter_type"); // Get filter from Intent
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        loadBills();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewBills = findViewById(R.id.recyclerViewBills);
        textEmptyState = findViewById(R.id.textEmptyState);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Hóa đơn chưa thanh toán");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        billAdapter = new BillAdapter(bills, bill -> {
            // Navigate to payment screen with bill info
            Intent intent = new Intent(BillListActivity.this, BillPaymentActivity.class);
            intent.putExtra("bill_id", bill.getBillId());
            intent.putExtra("customer_code", bill.getCustomerCode());
            intent.putExtra("amount", bill.getAmount());
            intent.putExtra("bill_type", bill.getBillType());
            intent.putExtra("provider", bill.getProvider());
            intent.putExtra("period", bill.getPeriod());
            startActivity(intent);
        });
        
        recyclerViewBills.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBills.setAdapter(billAdapter);
    }
    
    private void loadBills() {
        // Load all unpaid bills (for demo purposes)
        // In production, filter by customerCode from user profile
        com.google.firebase.firestore.Query query = billRepository.getAllUnpaidBills();
        
        // Apply filter if specified
        if (filterType != null && !filterType.isEmpty()) {
            query = query.whereEqualTo("billType", filterType);
        }
        
        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bills.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Bill bill = document.toObject(Bill.class);
                        bill.setBillId(document.getId());
                        bills.add(bill);
                    }
                    
                    billAdapter.notifyDataSetChanged();
                    
                    if (bills.isEmpty()) {
                        textEmptyState.setVisibility(View.VISIBLE);
                        recyclerViewBills.setVisibility(View.GONE);
                    } else {
                        textEmptyState.setVisibility(View.GONE);
                        recyclerViewBills.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh sách hóa đơn: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Adapter cho RecyclerView hiển thị danh sách hóa đơn
     */
    private static class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {
        private List<Bill> bills;
        private OnBillClickListener listener;
        
        interface OnBillClickListener {
            void onBillClick(Bill bill);
        }
        
        public BillAdapter(List<Bill> bills, OnBillClickListener listener) {
            this.bills = bills;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bill, parent, false);
            return new BillViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
            Bill bill = bills.get(position);
            holder.bind(bill);
            holder.itemView.setOnClickListener(v -> listener.onBillClick(bill));
        }
        
        @Override
        public int getItemCount() {
            return bills.size();
        }
        
        static class BillViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardBill;
            private TextView textBillType;
            private TextView textProvider;
            private TextView textCustomerCode;
            private TextView textPeriod;
            private TextView textAmount;
            private TextView textDueDate;
            private TextView textStatus;
            
            public BillViewHolder(@NonNull View itemView) {
                super(itemView);
                cardBill = itemView.findViewById(R.id.cardBill);
                textBillType = itemView.findViewById(R.id.textBillType);
                textProvider = itemView.findViewById(R.id.textProvider);
                textCustomerCode = itemView.findViewById(R.id.textCustomerCode);
                textPeriod = itemView.findViewById(R.id.textPeriod);
                textAmount = itemView.findViewById(R.id.textAmount);
                textDueDate = itemView.findViewById(R.id.textDueDate);
                textStatus = itemView.findViewById(R.id.textStatus);
            }
            
            public void bind(Bill bill) {
                textBillType.setText(bill.getBillTypeDisplayName());
                textProvider.setText(bill.getProvider());
                textCustomerCode.setText("Mã KH: " + bill.getCustomerCode());
                textPeriod.setText("Kỳ: " + bill.getPeriod());
                
                NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
                textAmount.setText(formatter.format(bill.getAmount()) + " VNĐ");
                
                if (bill.getDueDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dueDateStr = sdf.format(bill.getDueDate());
                    textDueDate.setText("Hạn: " + dueDateStr);
                    
                    // Highlight overdue bills
                    if (bill.isOverdue()) {
                        textDueDate.setTextColor(itemView.getContext().getColor(R.color.error));
                        textStatus.setText("QUÁ HẠN");
                        textStatus.setTextColor(itemView.getContext().getColor(R.color.error));
                    } else {
                        textDueDate.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                        textStatus.setText("Chưa thanh toán");
                        textStatus.setTextColor(itemView.getContext().getColor(R.color.warning));
                    }
                } else {
                    textDueDate.setText("Hạn: Chưa xác định");
                    textStatus.setText("Chưa thanh toán");
                }
            }
        }
    }
}

