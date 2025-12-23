package com.notificationcapture.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class UniversalSpinnerAdapter<T> extends ArrayAdapter<T> {

    public UniversalSpinnerAdapter(@NonNull Context context, @NonNull List<T> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_category_card, parent, false);
        }

        T item = getItem(position);
        TextView tvName = convertView.findViewById(R.id.tvCategoryName);
        View viewLeftBorder = convertView.findViewById(R.id.viewLeftBorder);
        View viewBackgroundTint = convertView.findViewById(R.id.viewBackgroundTint);
        TextView tvAmount = convertView.findViewById(R.id.tvCategoryAmount);

        // Standard hide amount for all spinners
        tvAmount.setVisibility(View.GONE);

        if (item != null) {
            String displayName = "";
            Integer displayColor = null;

            if (item instanceof SpinnerDisplayable) {
                SpinnerDisplayable sd = (SpinnerDisplayable) item;
                displayName = sd.getDisplayName();
                displayColor = sd.getDisplayColor();
            } else {
                displayName = item.toString();
            }

            tvName.setText(displayName);

            // Visibility logic
            if (displayColor != null && displayColor != 0 && !displayName.contains("...")) {
                viewLeftBorder.setVisibility(View.VISIBLE);
                viewBackgroundTint.setVisibility(View.VISIBLE);
                viewLeftBorder.setBackgroundColor(displayColor);
                viewBackgroundTint.setBackgroundColor(displayColor);
            } else {
                viewLeftBorder.setVisibility(View.GONE);
                viewBackgroundTint.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}
