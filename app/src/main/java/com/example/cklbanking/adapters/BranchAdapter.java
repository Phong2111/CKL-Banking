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
        ImageView branchIcon;
        TextView branchName, branchAddress, branchDistance, branchType;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            branchIcon = itemView.findViewById(R.id.branchIcon);
            branchName = itemView.findViewById(R.id.branchName);
            branchAddress = itemView.findViewById(R.id.branchAddress);
            branchDistance = itemView.findViewById(R.id.branchDistance);
            branchType = itemView.findViewById(R.id.branchType);
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

            // Calculate and display distance
            if (userLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        branch.getLatitude(), branch.getLongitude(),
                        results
                );
                
                double distanceInMeters = results[0];
                String distanceText;
                if (distanceInMeters < 1000) {
                    distanceText = String.format("%.0f m", distanceInMeters);
                } else {
                    distanceText = String.format("%.2f km", distanceInMeters / 1000);
                }
                branchDistance.setText(distanceText);
            } else {
                branchDistance.setText("--");
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




