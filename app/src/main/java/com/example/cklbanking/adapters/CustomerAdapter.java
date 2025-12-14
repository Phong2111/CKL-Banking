package com.example.cklbanking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.models.User;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {

    private List<User> customers;
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(User customer);
    }

    public CustomerAdapter(android.content.Context context, List<User> customers) {
        this.customers = customers;
    }

    public void setOnCustomerClickListener(OnCustomerClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        User customer = customers.get(position);
        holder.bind(customer);
    }

    @Override
    public int getItemCount() {
        return customers != null ? customers.size() : 0;
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {
        private TextView textCustomerName, textCustomerEmail, textCustomerPhone;

        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomerName = itemView.findViewById(R.id.textCustomerName);
            textCustomerEmail = itemView.findViewById(R.id.textCustomerEmail);
            textCustomerPhone = itemView.findViewById(R.id.textCustomerPhone);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCustomerClick(customers.get(getAdapterPosition()));
                }
            });
        }

        void bind(User customer) {
            textCustomerName.setText(customer.getFullName() != null ? customer.getFullName() : "N/A");
            textCustomerEmail.setText(customer.getEmail() != null ? customer.getEmail() : "N/A");
            textCustomerPhone.setText(customer.getPhone() != null ? customer.getPhone() : "N/A");
        }
    }
}





