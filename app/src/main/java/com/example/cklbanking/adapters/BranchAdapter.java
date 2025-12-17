package com.example.cklbanking.adapters;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cklbanking.R;
import com.example.cklbanking.models.Branch;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

    private Context context;
    private List<Branch> branches;
    private Location userLocation;
    private OnBranchClickListener listener;

    public interface OnBranchClickListener {
        void onBranchClick(Branch branch);
    }

    public BranchAdapter(Context context, List<Branch> branches) {
        this.context = context;
        this.branches = branches;
    }

    public void setOnBranchClickListener(OnBranchClickListener listener) {
        this.listener = listener;
    }

    public void setUserLocation(Location location) {
        this.userLocation = location;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_branch, parent, false);
        return new BranchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        Branch branch = branches.get(position);
        holder.bind(branch);
    }

    @Override
    public int getItemCount() {
        return branches != null ? branches.size() : 0;
    }

    class BranchViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView branchIcon, iconFavorite;
        TextView branchName, branchAddress, branchDistance, branchType, branchStatus;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            branchIcon = itemView.findViewById(R.id.branchIcon);
            branchName = itemView.findViewById(R.id.branchName);
            branchAddress = itemView.findViewById(R.id.branchAddress);
            branchDistance = itemView.findViewById(R.id.branchDistance);
            branchType = itemView.findViewById(R.id.branchType);
            branchStatus = itemView.findViewById(R.id.branchStatus);
            iconFavorite = itemView.findViewById(R.id.iconFavorite);
        }

        public void bind(Branch branch) {
            branchName.setText(branch.getName());
            branchAddress.setText(branch.getAddress());
            
            // Set branch type
            if (branch.getType() != null) {
                if (branch.getType().equals("atm")) {
                    branchType.setText("ATM");
                    branchIcon.setImageResource(R.drawable.ic_bank); // Có thể thay bằng icon ATM
                } else {
                    branchType.setText("Chi nhánh");
                    branchIcon.setImageResource(R.drawable.ic_bank);
                }
            }

            // Calculate and display distance using BranchDistanceHelper
            if (userLocation != null) {
                double distanceInMeters = com.example.cklbanking.utils.BranchDistanceHelper.calculateDistance(
                    userLocation, branch);
                String distanceText = com.example.cklbanking.utils.BranchDistanceHelper.formatDistance(distanceInMeters);
                branchDistance.setText(distanceText);
                branchDistance.setVisibility(View.VISIBLE);
            } else {
                branchDistance.setText("--");
                branchDistance.setVisibility(View.VISIBLE);
            }
            
            // Set branch status (open/closed)
            boolean isOpen = branch.isOpen();
            branchStatus.setText(isOpen ? "Đang mở" : "Đã đóng");
            branchStatus.setTextColor(isOpen ? 
                context.getColor(R.color.success) : 
                context.getColor(R.color.error));
            
            // Show favorite icon if branch is favorite
            if (branch.isFavorite()) {
                iconFavorite.setVisibility(View.VISIBLE);
            } else {
                iconFavorite.setVisibility(View.GONE);
            }

            // Set click listener
            if (listener != null && cardView != null) {
                cardView.setOnClickListener(v -> listener.onBranchClick(branch));
            }
        }
    }

    public void updateBranches(List<Branch> newBranches) {
        this.branches = newBranches;
        notifyDataSetChanged();
    }
}











