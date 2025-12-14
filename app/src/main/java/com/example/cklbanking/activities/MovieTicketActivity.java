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

public class MovieTicketActivity extends AppCompatActivity {

    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private Spinner spinnerAccount;
    private TextInputEditText editCinema;
    private TextInputEditText editMovie;
    private TextInputEditText editShowtime;
    private TextInputEditText editSeats;
    private TextInputEditText editTickets;
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
        setContentView(R.layout.activity_movie_ticket);

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
        editCinema = findViewById(R.id.editCinema);
        editMovie = findViewById(R.id.editMovie);
        editShowtime = findViewById(R.id.editShowtime);
        editSeats = findViewById(R.id.editSeats);
        editTickets = findViewById(R.id.editTickets);
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
        String cinema = editCinema.getText().toString().trim();
        String movie = editMovie.getText().toString().trim();
        String showtime = editShowtime.getText().toString().trim();
        String seats = editSeats.getText().toString().trim();
        String ticketsStr = editTickets.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();

        if (TextUtils.isEmpty(cinema)) {
            editCinema.setError("Vui lòng nhập tên rạp");
            return;
        }

        if (TextUtils.isEmpty(movie)) {
            editMovie.setError("Vui lòng nhập tên phim");
            return;
        }

        if (TextUtils.isEmpty(showtime)) {
            editShowtime.setError("Vui lòng nhập suất chiếu");
            return;
        }

        if (TextUtils.isEmpty(seats)) {
            editSeats.setError("Vui lòng nhập số ghế");
            return;
        }

        if (TextUtils.isEmpty(ticketsStr)) {
            editTickets.setError("Vui lòng nhập số lượng vé");
            return;
        }

        int tickets;
        try {
            tickets = Integer.parseInt(ticketsStr);
            if (tickets <= 0 || tickets > 20) {
                editTickets.setError("Số lượng vé phải từ 1 đến 20");
                return;
            }
        } catch (NumberFormatException e) {
            editTickets.setError("Số lượng vé không hợp lệ");
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

        if (amount < 50000) {
            editAmount.setError("Tổng tiền tối thiểu là 50.000 VNĐ");
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

        performBooking(cinema, movie, showtime, seats, tickets, amount);
    }

    private void performBooking(String cinema, String movie, String showtime, String seats, int tickets, double amount) {
        btnBook.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();

        // Calculate new balance
        double newBalance = selectedAccount.getBalance() - amount;

        // Create booking reference
        String bookingReference = "MV" + System.currentTimeMillis();
        
        // Create booking details JSON string
        String bookingDetails = String.format(
            "{\"cinema\":\"%s\",\"movie\":\"%s\",\"showtime\":\"%s\",\"seats\":\"%s\",\"tickets\":%d}",
            cinema, movie, showtime, seats, tickets
        );

        // Create utility payment
        UtilityPayment utilityPayment = new UtilityPayment();
        utilityPayment.setUserId(userId);
        utilityPayment.setFromAccountId(selectedAccount.getAccountId());
        utilityPayment.setUtilityType("movie_ticket");
        utilityPayment.setAmount(amount);
        utilityPayment.setStatus("completed");
        utilityPayment.setBookingReference(bookingReference);
        utilityPayment.setBookingDetails(bookingDetails);
        utilityPayment.setDescription(String.format("Đặt vé xem phim %s tại %s (%d vé)", movie, cinema, tickets));

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(selectedAccount.getAccountId());
        transaction.setToAccountId(cinema); // Cinema as recipient
        transaction.setAmount(amount);
        transaction.setType("movie_ticket");
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
                    Toast.makeText(this, "Đặt vé thành công! Mã đặt chỗ: " + bookingReference, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnBook.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

