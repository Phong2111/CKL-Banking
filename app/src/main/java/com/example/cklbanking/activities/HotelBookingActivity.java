package com.example.cklbanking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Account;
import com.example.cklbanking.models.Transaction;
import com.example.cklbanking.models.UtilityPayment;
import com.example.cklbanking.repositories.AccountRepository;
import com.example.cklbanking.repositories.TransactionRepository;
import com.example.cklbanking.repositories.UtilityRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HotelBookingActivity extends AppCompatActivity {

    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private Spinner spinnerAccount;
    private TextInputEditText editLocation;
    private TextInputEditText editHotelName;
    private TextInputEditText editCheckIn;
    private TextInputEditText editCheckOut;
    private TextInputEditText editRooms;
    private TextInputEditText editGuests;
    private TextInputEditText editAmount;
    private TextView textAccountBalance;
    private MaterialButton btnBook;
    private CircularProgressIndicator progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private UtilityRepository utilityRepository;

    private List<Account> accounts;
    private Account selectedAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_booking);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        utilityRepository = new UtilityRepository();

        initViews();
        setupToolbar();
        setupListeners();
        loadAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerAccount = findViewById(R.id.spinnerAccount);
        editLocation = findViewById(R.id.editLocation);
        editHotelName = findViewById(R.id.editHotelName);
        editCheckIn = findViewById(R.id.editCheckIn);
        editCheckOut = findViewById(R.id.editCheckOut);
        editRooms = findViewById(R.id.editRooms);
        editGuests = findViewById(R.id.editGuests);
        editAmount = findViewById(R.id.editAmount);
        textAccountBalance = findViewById(R.id.textAccountBalance);
        btnBook = findViewById(R.id.btnBook);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (accounts != null && position >= 0 && position < accounts.size()) {
                    selectedAccount = accounts.get(position);
                    updateAccountBalanceDisplay();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnBook.setOnClickListener(v -> processBooking());
    }

    private void loadAccounts() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        accountRepository.getAccountsForUser(userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accounts = new ArrayList<>();
                    List<String> accountNames = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Account account = document.toObject(Account.class);
                        account.setAccountId(document.getId());
                        accounts.add(account);
                        accountNames.add(account.getAccountNumber() + " - " + getAccountTypeName(account.getAccountType()));
                    }

                    if (accounts.isEmpty()) {
                        Toast.makeText(this, "Bạn chưa có tài khoản nào", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            accountNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAccount.setAdapter(adapter);

                    // Set first account as default
                    if (!accounts.isEmpty()) {
                        selectedAccount = accounts.get(0);
                        updateAccountBalanceDisplay();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh sách tài khoản: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAccountBalanceDisplay() {
        if (selectedAccount != null) {
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            textAccountBalance.setText("Số dư: " + formatter.format(selectedAccount.getBalance()) + " VNĐ");
        }
    }

    private String getAccountTypeName(String accountType) {
        switch (accountType) {
            case "checking":
                return "Tài khoản thanh toán";
            case "saving":
                return "Tài khoản tiết kiệm";
            case "mortgage":
                return "Tài khoản vay";
            default:
                return accountType;
        }
    }

    private void processBooking() {
        // Validate input
        String location = editLocation.getText().toString().trim();
        String hotelName = editHotelName.getText().toString().trim();
        String checkIn = editCheckIn.getText().toString().trim();
        String checkOut = editCheckOut.getText().toString().trim();
        String roomsStr = editRooms.getText().toString().trim();
        String guestsStr = editGuests.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();

        if (TextUtils.isEmpty(location)) {
            editLocation.setError("Vui lòng nhập địa điểm");
            return;
        }

        if (TextUtils.isEmpty(hotelName)) {
            editHotelName.setError("Vui lòng nhập tên khách sạn");
            return;
        }

        if (TextUtils.isEmpty(checkIn)) {
            editCheckIn.setError("Vui lòng nhập ngày check-in");
            return;
        }

        if (TextUtils.isEmpty(checkOut)) {
            editCheckOut.setError("Vui lòng nhập ngày check-out");
            return;
        }

        if (TextUtils.isEmpty(roomsStr)) {
            editRooms.setError("Vui lòng nhập số phòng");
            return;
        }

        int rooms;
        try {
            rooms = Integer.parseInt(roomsStr);
            if (rooms <= 0 || rooms > 10) {
                editRooms.setError("Số phòng phải từ 1 đến 10");
                return;
            }
        } catch (NumberFormatException e) {
            editRooms.setError("Số phòng không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(guestsStr)) {
            editGuests.setError("Vui lòng nhập số khách");
            return;
        }

        int guests;
        try {
            guests = Integer.parseInt(guestsStr);
            if (guests <= 0 || guests > 50) {
                editGuests.setError("Số khách phải từ 1 đến 50");
                return;
            }
        } catch (NumberFormatException e) {
            editGuests.setError("Số khách không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            editAmount.setError("Vui lòng nhập tổng tiền");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            editAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            editAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount < 200000) {
            editAmount.setError("Tổng tiền tối thiểu là 200.000 VNĐ");
            return;
        }

        if (selectedAccount == null) {
            Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check balance
        if (selectedAccount.getBalance() < amount) {
            Toast.makeText(this, "Số dư tài khoản không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        performBooking(location, hotelName, checkIn, checkOut, rooms, guests, amount);
    }

    private void performBooking(String location, String hotelName, String checkIn, String checkOut, int rooms, int guests, double amount) {
        btnBook.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();

        // Calculate new balance
        double newBalance = selectedAccount.getBalance() - amount;

        // Create booking reference
        String bookingReference = "HT" + System.currentTimeMillis();
        
        // Create booking details JSON string
        String bookingDetails = String.format(
            "{\"location\":\"%s\",\"hotelName\":\"%s\",\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"rooms\":%d,\"guests\":%d}",
            location, hotelName, checkIn, checkOut, rooms, guests
        );

        // Create utility payment
        UtilityPayment utilityPayment = new UtilityPayment();
        utilityPayment.setUserId(userId);
        utilityPayment.setFromAccountId(selectedAccount.getAccountId());
        utilityPayment.setUtilityType("hotel_booking");
        utilityPayment.setAmount(amount);
        utilityPayment.setStatus("completed");
        utilityPayment.setBookingReference(bookingReference);
        utilityPayment.setBookingDetails(bookingDetails);
        utilityPayment.setDescription(String.format("Đặt phòng tại %s (%d phòng, %d khách)", hotelName, rooms, guests));

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(selectedAccount.getAccountId());
        transaction.setToAccountId(hotelName); // Hotel as recipient
        transaction.setAmount(amount);
        transaction.setType("hotel_booking");
        transaction.setStatus("completed");

        // Use batch write for atomicity
        WriteBatch batch = db.batch();

        // Update account balance
        batch.update(db.collection("accounts").document(selectedAccount.getAccountId()), "balance", newBalance);

        // Create utility payment
        String paymentId = db.collection("utility_payments").document().getId();
        utilityPayment.setPaymentId(paymentId);
        batch.set(db.collection("utility_payments").document(paymentId), utilityPayment);

        // Create transaction
        String transactionId = db.collection("transactions").document().getId();
        transaction.setTransactionId(transactionId);
        batch.set(db.collection("transactions").document(transactionId), transaction);

        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đặt phòng thành công! Mã đặt chỗ: " + bookingReference, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnBook.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}





