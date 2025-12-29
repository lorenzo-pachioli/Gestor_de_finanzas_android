package com.notificationcapture.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;

public class SelectorBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_OPTIONS = "options";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_COLORS = "colors"; // NEW

    private OnOptionSelectedListener listener;
    private List<String> options;
    private String selectedOption;
    private java.util.Map<String, Integer> colorMap; // NEW

    public interface OnOptionSelectedListener {
        void onOptionSelected(String option);
    }

    public static SelectorBottomSheet newInstance(String title, List<String> options, String selectedOption,
            java.util.Map<String, Integer> colorMap) {
        SelectorBottomSheet fragment = new SelectorBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putStringArray(ARG_OPTIONS, options.toArray(new String[0]));
        args.putString(ARG_SELECTED, selectedOption);
        if (colorMap != null) {
            // Convert Map to Serializable? HashMap is Serializable.
            args.putSerializable(ARG_COLORS, (java.io.Serializable) colorMap);
        }
        fragment.setArguments(args);
        return fragment;
    }

    // Overload for backward compatibility / keeping it simple for wallets
    public static SelectorBottomSheet newInstance(String title, List<String> options, String selectedOption) {
        return newInstance(title, options, selectedOption, null);
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        RecyclerView recyclerOptions = view.findViewById(R.id.recyclerDetails);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            String[] optionsArray = getArguments().getStringArray(ARG_OPTIONS);
            selectedOption = getArguments().getString(ARG_SELECTED);
            // Safe cast if possible, or just check type
            try {
                colorMap = (java.util.Map<String, Integer>) getArguments().getSerializable(ARG_COLORS);
            } catch (Exception e) {
                colorMap = null;
            }

            if (title != null) {
                tvTitle.setText(title);
            }

            if (optionsArray != null) {
                options = java.util.Arrays.asList(optionsArray);
                OptionAdapter adapter = new OptionAdapter(options, selectedOption, colorMap, option -> {
                    if (listener != null) {
                        listener.onOptionSelected(option);
                    }
                    dismiss();
                });

                recyclerOptions.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerOptions.setAdapter(adapter);
            }
        }
    }

    // Adapter interno para las opciones
    private static class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.ViewHolder> {
        private final List<String> options;
        private final String selectedOption;
        private final java.util.Map<String, Integer> colorMap;
        private final OnOptionClickListener clickListener;

        private static final int TYPE_SIMPLE = 0;
        private static final int TYPE_CARD = 1;

        interface OnOptionClickListener {
            void onClick(String option);
        }

        OptionAdapter(List<String> options, String selectedOption, java.util.Map<String, Integer> colorMap,
                OnOptionClickListener clickListener) {
            this.options = options;
            this.selectedOption = selectedOption;
            this.colorMap = colorMap;
            this.clickListener = clickListener;
        }

        @Override
        public int getItemViewType(int position) {
            return colorMap != null ? TYPE_CARD : TYPE_SIMPLE;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId = (viewType == TYPE_CARD) ? R.layout.item_category_card : R.layout.item_selector_option;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(layoutId, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String option = options.get(position);
            android.content.Context context = holder.itemView.getContext();

            if (colorMap != null) {
                // CARD LAYOUT (Categories)
                if (holder.tvCategoryName != null) {
                    holder.tvCategoryName.setText(option);

                    if (option.equals(selectedOption)) {
                        holder.tvCategoryName.setTypeface(null, android.graphics.Typeface.BOLD);
                        holder.tvCategoryName.setTextColor(androidx.core.content.ContextCompat.getColor(context,
                                R.color.text_primary));
                    } else {
                        holder.tvCategoryName.setTypeface(null, android.graphics.Typeface.NORMAL);
                        holder.tvCategoryName.setTextColor(androidx.core.content.ContextCompat.getColor(context,
                                R.color.text_secondary));
                    }
                }

                if (holder.amountContainer != null) {
                    holder.amountContainer.setVisibility(View.GONE);
                }

                int color = androidx.core.content.ContextCompat.getColor(context, R.color.accent_main); // Default
                if (colorMap.containsKey(option)) {
                    Integer c = colorMap.get(option);
                    if (c != null)
                        color = c;
                }

                if (holder.viewLeftBorder != null)
                    holder.viewLeftBorder.setBackgroundColor(color);
                if (holder.viewBackgroundTint != null)
                    holder.viewBackgroundTint.setBackgroundColor(color);

            } else {
                // SIMPLE LAYOUT (Wallets)
                if (holder.tvOptionName != null) {
                    holder.tvOptionName.setText(option);

                    if (option.equals(selectedOption)) {
                        holder.tvOptionName.setTextColor(androidx.core.content.ContextCompat.getColor(context,
                                R.color.accent_main));
                        holder.tvOptionName.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        holder.tvOptionName.setTextColor(androidx.core.content.ContextCompat.getColor(context,
                                R.color.text_primary));
                        holder.tvOptionName.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
            }

            holder.itemView.setOnClickListener(v -> clickListener.onClick(option));
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            // Simple Layout Views
            TextView tvOptionName;

            // Card Layout Views
            TextView tvCategoryName;
            View viewLeftBorder;
            View viewBackgroundTint;
            View amountContainer;

            ViewHolder(View itemView) {
                super(itemView);
                tvOptionName = itemView.findViewById(R.id.tvOptionName);

                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                viewLeftBorder = itemView.findViewById(R.id.viewLeftBorder);
                viewBackgroundTint = itemView.findViewById(R.id.viewBackgroundTint);
                amountContainer = itemView.findViewById(R.id.amountContainer);
            }
        }
    }
}
