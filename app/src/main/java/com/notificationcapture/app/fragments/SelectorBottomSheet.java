package com.notificationcapture.app.fragments;

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
import java.util.Map;

import com.notificationcapture.app.R;
import com.notificationcapture.app.interfaces.OnOptionSelectedListener;
import com.notificationcapture.app.interfaces.SpinnerDisplayable;

public class SelectorBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_OPTIONS = "options";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_HIGHLIGHTED = "highlighted"; // NEW
    private static final String ARG_COLORS = "colors"; // NEW

    private OnOptionSelectedListener listener;

    public static <T extends java.io.Serializable & SpinnerDisplayable> SelectorBottomSheet newInstance(
            String title, List<T> options, String selectedOption) {
        return newInstance(title, options, selectedOption, null);
    }

    public static <T extends java.io.Serializable & SpinnerDisplayable> SelectorBottomSheet newInstance(
            String title, List<T> options, String selectedOption, List<String> highlightedIds) {
        SelectorBottomSheet fragment = new SelectorBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_OPTIONS, (java.io.Serializable) options);
        args.putString(ARG_SELECTED, selectedOption);
        args.putStringArrayList(ARG_HIGHLIGHTED, highlightedIds != null ? new java.util.ArrayList<>(highlightedIds) : null);
        fragment.setArguments(args);
        return fragment;
    }

    // public static SelectorBottomSheet newInstance(String title, List<String>
    // options, String selectedOption) {
    // // Fallback or simple version for non-SpinnerDisplayable lists (like Strings)
    // // We can just keep it for backward compatibility if needed, or refactor all
    // // calls.
    // SelectorBottomSheet fragment = new SelectorBottomSheet();
    // Bundle args = new Bundle();
    // args.putString(ARG_TITLE, title);
    // args.putStringArray(ARG_OPTIONS, options.toArray(new String[0]));
    // args.putString(ARG_SELECTED, selectedOption);
    // fragment.setArguments(args);
    // return fragment;
    // }

    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.FullScreenBottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.getLayoutParams().width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior
                        .from(bottomSheet);
                behavior.setMaxWidth(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        RecyclerView recyclerOptions = view.findViewById(R.id.recyclerDetails);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            Object optionsObj = getArguments().getSerializable(ARG_OPTIONS);
            String selectedOption = getArguments().getString(ARG_SELECTED);

            if (title != null) {
                tvTitle.setText(title);
            }

            if (optionsObj != null) {
                List<?> options;
                List<String> highlightedIds = getArguments().getStringArrayList(ARG_HIGHLIGHTED);

                if (optionsObj instanceof List) {
                    options = (List<?>) optionsObj;
                } else if (optionsObj instanceof String[]) {
                    options = java.util.Arrays.asList((String[]) optionsObj);
                } else {
                    options = new java.util.ArrayList<>();
                }

                OptionAdapter adapter = new OptionAdapter(options, selectedOption, highlightedIds, item -> {
                    if (listener != null) {
                        // For SpinnerDisplayable items, we might want to pass the object or ID
                        // But existing listener uses a String. We'll handle this in the adapter call.
                        if (item instanceof com.notificationcapture.app.interfaces.SpinnerDisplayable) {
                             listener.onOptionSelected(((com.notificationcapture.app.interfaces.SpinnerDisplayable) item).getDisplayName());
                        } else {
                             listener.onOptionSelected(item.toString());
                        }
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
        interface OnOptionClickListener {
            void onClick(Object item);
        }

        private final List<?> options;
        private final String selectedOption;
        private final List<String> highlightedIds;
        private final OnOptionClickListener clickListener;

        private static final int TYPE_SIMPLE = 0;
        private static final int TYPE_CARD = 1;

        OptionAdapter(List<?> options, String selectedOption, List<String> highlightedIds, OnOptionClickListener clickListener) {
            this.options = options;
            this.selectedOption = selectedOption;
            this.highlightedIds = highlightedIds;
            this.clickListener = clickListener;
        }

        @Override
        public int getItemViewType(int position) {
            Object option = options.get(position);
            // Wallets and GlobalWallet always use the simple layout
            if (option instanceof com.notificationcapture.app.models.Wallets
                    || option instanceof com.notificationcapture.app.models.GlobalWallet) {
                return TYPE_SIMPLE;
            }
            return (option instanceof com.notificationcapture.app.interfaces.SpinnerDisplayable) ? TYPE_CARD
                    : TYPE_SIMPLE;
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
            Object item = options.get(position);
            android.content.Context context = holder.itemView.getContext();
            String optionName;
            Integer color = null;

            if (item instanceof com.notificationcapture.app.interfaces.SpinnerDisplayable) {
                com.notificationcapture.app.interfaces.SpinnerDisplayable sd = (com.notificationcapture.app.interfaces.SpinnerDisplayable) item;
                optionName = sd.getDisplayName();
                color = sd.getDisplayColor();
            } else {
                optionName = item.toString();
            }

            // Set color for highlighting (selection or global list status)
            boolean isHighlighted = false;
            if (highlightedIds != null) {
                if (item instanceof com.notificationcapture.app.models.GlobalWallet) {
                    com.notificationcapture.app.models.GlobalWallet gw = (com.notificationcapture.app.models.GlobalWallet) item;
                    // Resaltado si el package primario está en highlightedIds
                    isHighlighted = highlightedIds.contains(gw.getPrimaryPackageName());
                } else if (item instanceof com.notificationcapture.app.models.Wallets) {
                    com.notificationcapture.app.models.Wallets wallet = (com.notificationcapture.app.models.Wallets) item;
                    isHighlighted = highlightedIds.contains(wallet.getPackageName());
                }
            }

            if (getItemViewType(position) == TYPE_CARD) {
                // CARD LAYOUT (Categories / Wallets)
                if (holder.tvCategoryName != null) {
                    holder.tvCategoryName.setText(optionName);

                    if (optionName.equals(selectedOption)) {
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

                if (color == null || color == 0) {
                    color = androidx.core.content.ContextCompat.getColor(context, R.color.accent_main); // Default for categories
                }

                if (holder.viewLeftBorder != null)
                    holder.viewLeftBorder.setBackgroundColor(color);
                if (holder.viewBackgroundTint != null)
                    holder.viewBackgroundTint.setBackgroundColor(color);

            } else {
                // SIMPLE LAYOUT
                if (holder.tvOptionName != null) {
                    holder.tvOptionName.setText(optionName);

                    if (optionName.equals(selectedOption) || isHighlighted) {
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

            holder.itemView.setOnClickListener(v -> clickListener.onClick(item));
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
