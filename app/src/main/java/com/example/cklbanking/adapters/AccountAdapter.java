package com.example.cklbanking.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.activities.AccountDetailActivity;
import com.example.cklbanking.models.Account;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private Context context;
    private List<Account> accounts;
    private OnAccountClickListener listener;

    public AccountAdapter(Context context, List<Account> accounts) {
        this.context = context;
        this.accounts = accounts;
    }

    public void setOnAccountClickListener(OnAccountClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.bind(account);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    class AccountViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        View leftBorder;
        TextView accountTypeName, accountNumber, accountBalance;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            leftBorder = itemView.findViewById(R.id.leftBorder);
            accountTypeName = itemView.findViewById(R.id.accountTypeName);
            accountNumber = itemView.findViewById(R.id.accountNumber);
            accountBalance = itemView.findViewById(R.id.accountBalance);
        }

        public void bind(Account account) {
            // Set account type name and color
            int borderColor;
            String typeName;
            
            switch (account.getAccountType()) {
                case "checking":
                    typeName = "Tài khoản thanh toán";
                    borderColor = context.getColor(R.color.checking_account);
                    break;
                case "saving":
                    typeName = "Tài khoản tiết kiệm";
                    borderColor = context.getColor(R.color.saving_account);
                    break;
                case "mortgage":
                    typeName = "Tài khoản vay thế chấp";
                    borderColor = context.getColor(R.color.mortgage_account);
                    break;
                default:
                    typeName = "Tài khoản";
                    borderColor = context.getColor(R.color.primary);
                    break;
            }
            
            accountTypeName.setText(typeName);
            leftBorder.setBackgroundColor(borderColor);
            accountNumber.setText(account.getAccountNumber());
            accountBalance.setText(formatCurrency(account.getBalance()));

            // Set click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccountClick(account);
                } else {
                    // Default behavior: open account detail
                    Intent intent = new Intent(context, AccountDetailActivity.class);
                    intent.putExtra("account_id", account.getAccountNumber());
                    intent.putExtra("account_type", account.getAccountType());
                    context.startActivity(intent);
                }
            });
        }

        private String formatCurrency(double amount) {
            return String.format("%,.0f ₫", amount);
        }
    }

    public void updateAccounts(List<Account> newAccounts) {
        this.accounts = newAccounts;
        notifyDataSetChanged();
    }
}
