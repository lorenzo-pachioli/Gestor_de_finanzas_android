package com.notificationcapture.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.notificationcapture.app.R;
import com.notificationcapture.app.interfaces.OnCategoryClickListener;

public class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder> {

    private List<Map.Entry<String, Double>> categoryList;
    private Map<String, Integer> categoryColors;
    private OnCategoryClickListener listener;

    private int totalPeriodAmount;

    public CategorySummaryAdapter(Map<String, Double> categoryData, Map<String, Integer> categoryColors,
            OnCategoryClickListener listener) {
        this.categoryList = new ArrayList<>(categoryData.entrySet());
        this.categoryColors = categoryColors;
        calculateTotal();
        // Sort by amount descending
        this.categoryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        this.listener = listener;
    }

    private void calculateTotal() {
        totalPeriodAmount = 0;
        for (Map.Entry<String, Double> entry : categoryList) {
            totalPeriodAmount += entry.getValue();
        }
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

        String formattedAmount = "$" + com.notificationcapture.app.utils.MoneyTextWatcher.format(total);
        holder.tvTotalAmount.setText(formattedAmount);

        // Percentage Calculation
        if (totalPeriodAmount > 0) {
            int percentage = (int) Math.round((total / totalPeriodAmount) * 100);
            holder.tvCategoryPercentage.setText(percentage + "%");
            holder.tvCategoryPercentage.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategoryPercentage.setVisibility(View.GONE);
        }

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
        calculateTotal();
        // Sort by amount descending
        this.categoryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvTotalAmount;
        TextView tvCategoryPercentage;
        View viewLeftBorder;
        View viewBackgroundTint;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTotalAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvCategoryPercentage = itemView.findViewById(R.id.tvCategoryPercentage);
            viewLeftBorder = itemView.findViewById(R.id.viewLeftBorder);
            viewBackgroundTint = itemView.findViewById(R.id.viewBackgroundTint);
        }
    }
}
