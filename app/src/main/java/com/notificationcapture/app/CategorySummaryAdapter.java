package com.notificationcapture.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder> {

    private List<Map.Entry<String, Double>> categoryList;
    private Map<String, Integer> categoryColors;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category, Double totalAmount);
    }

    public CategorySummaryAdapter(Map<String, Double> categoryData, Map<String, Integer> categoryColors,
            OnCategoryClickListener listener) {
        this.categoryList = new ArrayList<>(categoryData.entrySet());
        this.categoryColors = categoryColors;
        // Sort by amount descending
        this.categoryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Double> entry = categoryList.get(position);
        String category = entry.getKey();
        Double total = entry.getValue();

        holder.tvCategoryName.setText(category);

        String formattedAmount = String.format("$%.2f", total)
                .replace(",", "#")
                .replace(".", ",")
                .replace("#", ".");
        holder.tvTotalAmount.setText(formattedAmount);

        // Apply Colors
        int color = categoryColors.getOrDefault(category, android.graphics.Color.GRAY);
        holder.viewLeftBorder.setBackgroundColor(color);
        holder.viewBackgroundTint.setBackgroundColor(color);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category, total);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateData(Map<String, Double> newCategoryData, Map<String, Integer> newColors) {
        this.categoryList = new ArrayList<>(newCategoryData.entrySet());
        if (newColors != null) {
            this.categoryColors = newColors;
        }
        // Sort by amount descending
        this.categoryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvTotalAmount;
        View viewLeftBorder;
        View viewBackgroundTint;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTotalAmount = itemView.findViewById(R.id.tvCategoryAmount);
            viewLeftBorder = itemView.findViewById(R.id.viewLeftBorder);
            viewBackgroundTint = itemView.findViewById(R.id.viewBackgroundTint);
        }
    }
}
