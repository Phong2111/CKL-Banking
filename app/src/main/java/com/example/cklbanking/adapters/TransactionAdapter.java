package com.example.cklbanking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Transaction;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactions;
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView transactionIcon;
        TextView transactionTitle, transactionDescription, transactionDate;
        TextView transactionAmount, transactionStatus;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            transactionIcon = itemView.findViewById(R.id.transactionIcon);
            transactionTitle = itemView.findViewById(R.id.transactionTitle);
            transactionDescription = itemView.findViewById(R.id.transactionDescription);
            transactionDate = itemView.findViewById(R.id.transactionDate);
            transactionAmount = itemView.findViewById(R.id.transactionAmount);
            transactionStatus = itemView.findViewById(R.id.transactionStatus);
        }

        public void bind(Transaction transaction) {
            // Set transaction title based on type
            switch (transaction.getType()) {
                case "transfer":
                    transactionTitle.setText("Chuyển tiền");
                    transactionIcon.setImageResource(R.drawable.ic_launcher_foreground);
                    break;
                case "deposit":
                    transactionTitle.setText("Nạp tiền");
                    transactionIcon.setImageResource(R.drawable.ic_launcher_foreground);
                    break;
                case "withdraw":
                    transactionTitle.setText("Rút tiền");
                    transactionIcon.setImageResource(R.drawable.ic_launcher_foreground);
                    break;
                case "payment":
                    transactionTitle.setText("Thanh toán");
                    transactionIcon.setImageResource(R.drawable.ic_launcher_foreground);
                    break;
                default:
                    transactionTitle.setText("Giao dịch");
                    break;
            }

            // Set description - using basic info from original model
            if (transaction.getToAccountId() != null) {
                transactionDescription.setText("Đến TK: " + transaction.getToAccountId());
            } else {
                transactionDescription.setText("Không có mô tả");
            }

            // Set date
            if (transaction.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                transactionDate.setText(sdf.format(transaction.getTimestamp()));
            }

            // Set amount with color
            String amountText;
            int amountColor;
            
            if (transaction.getType().equals("deposit")) {
                amountText = "+" + formatCurrency(transaction.getAmount());
                amountColor = context.getColor(R.color.success);
            } else {
                amountText = "-" + formatCurrency(transaction.getAmount());
                amountColor = context.getColor(R.color.error);
            }
            
            transactionAmount.setText(amountText);
            transactionAmount.setTextColor(amountColor);

            // Set status
            String statusText;
            int statusColor;
            
            switch (transaction.getStatus()) {
                case "completed":
                    statusText = "Thành công";
                    statusColor = context.getColor(R.color.success);
                    break;
                case "pending":
                    statusText = "Đang xử lý";
                    statusColor = context.getColor(R.color.warning);
                    break;
                case "failed":
                    statusText = "Thất bại";
                    statusColor = context.getColor(R.color.error);
                    break;
                case "cancelled":
                    statusText = "Đã hủy";
                    statusColor = context.getColor(R.color.text_hint);
                    break;
                default:
                    statusText = "Không rõ";
                    statusColor = context.getColor(R.color.text_secondary);
                    break;
            }
            
            transactionStatus.setText(statusText);
            transactionStatus.setTextColor(statusColor);

            // Set click listener
            if (listener != null && cardView != null) {
                cardView.setOnClickListener(v -> listener.onTransactionClick(transaction));
            }
        }

        private String formatCurrency(double amount) {
            return String.format("%,.0f ₫", amount);
        }
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }
}
