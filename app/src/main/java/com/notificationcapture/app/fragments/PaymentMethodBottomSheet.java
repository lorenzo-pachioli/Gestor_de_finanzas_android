package com.notificationcapture.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.notificationcapture.app.repositories.CreditCardRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.CreditCard;

import java.util.List;

import com.notificationcapture.app.interfaces.PaymentMethodListener;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.WalletRepository;

public class PaymentMethodBottomSheet extends BottomSheetDialogFragment {

    private PaymentMethodListener listener;
    private WalletRepository walletRepository;
    private CreditCardRepository creditCardRepository;

    public void setListener(PaymentMethodListener listener) {
        this.listener = listener;
    }

    private boolean restrictedToCredit = false;
    private boolean restrictedToDebitCash = false;
    private int initialInstallments = 1;

    public void setRestrictedMode(boolean restrictedToCredit) {
        this.restrictedToCredit = restrictedToCredit;
        this.restrictedToDebitCash = !restrictedToCredit;
    }

    public void setInitialInstallments(int installments) {
        this.initialInstallments = installments;
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
        return inflater.inflate(R.layout.layout_payment_method_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();
        creditCardRepository = RepositoryProvider.getInstance().getCreditCardRepository();

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        PaymentMethodAdapter adapter = new PaymentMethodAdapter();
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Need to adjust position logic based on restrictions
            if (restrictedToCredit) {
                tab.setText("Crédito");
            } else if (restrictedToDebitCash) {
                switch (position) {
                    case 0:
                        tab.setText("Efectivo");
                        break;
                    case 1:
                        tab.setText("Débito");
                        break;
                }
            } else {
                switch (position) {
                    case 0:
                        tab.setText("Efectivo");
                        break;
                    case 1:
                        tab.setText("Débito");
                        break;
                    case 2:
                        tab.setText("Crédito");
                        break;
                }
            }
        }).attach();
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

    private class PaymentMethodAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            if (restrictedToCredit) {
                return new CreditViewHolder(inflater.inflate(R.layout.layout_payment_credit, parent, false));
            } else if (restrictedToDebitCash) {
                if (viewType == 0)
                    return new CashViewHolder(inflater.inflate(R.layout.layout_payment_cash, parent, false));
                else
                    return new DebitViewHolder(inflater.inflate(R.layout.layout_payment_debit, parent, false));
            } else {
                if (viewType == 0) {
                    return new CashViewHolder(inflater.inflate(R.layout.layout_payment_cash, parent, false));
                } else if (viewType == 1) {
                    return new DebitViewHolder(inflater.inflate(R.layout.layout_payment_debit, parent, false));
                } else {
                    return new CreditViewHolder(inflater.inflate(R.layout.layout_payment_credit, parent, false));
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DebitViewHolder) {
                ((DebitViewHolder) holder).bind();
            } else if (holder instanceof CreditViewHolder) {
                ((CreditViewHolder) holder).bind();
            } else if (holder instanceof CashViewHolder) {
                ((CashViewHolder) holder).bind();
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if (restrictedToCredit)
                return 1;
            if (restrictedToDebitCash)
                return 2;
            return 3;
        }
    }

    private class CashViewHolder extends RecyclerView.ViewHolder {
        Button btnConfirm;

        CashViewHolder(@NonNull View itemView) {
            super(itemView);
            btnConfirm = itemView.findViewById(R.id.btnConfirmCash);

            // Re-find in case ID behaves weirdly in ViewPager, but should be fine
        }

        void bind() {
            btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPaymentMethodSelected(PaymentMethod.EFECTIVO, null, 1);
                }
                dismiss();
            });
        }
    }

    private class DebitViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerWallets;
        Button btnConfirm;
        com.notificationcapture.app.models.Wallets selectedWallet;

        DebitViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerWallets = itemView.findViewById(R.id.recyclerWallets);
            btnConfirm = itemView.findViewById(R.id.btnConfirmDebit);
            recyclerWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        void bind() {
            List<com.notificationcapture.app.models.Wallets> wallets = walletRepository.getAllWallets();
            SimpleWalletAdapter adapter = new SimpleWalletAdapter(wallets, wallet -> {
                selectedWallet = wallet;
                // notify adapter to refresh colors? or just rely on the adapter's internal
                // state
            });
            recyclerWallets.setAdapter(adapter);

            btnConfirm.setOnClickListener(v -> {
                if (selectedWallet == null) {
                    Toast.makeText(requireContext(), "Por favor, selecciona una billetera", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (listener != null) {
                    listener.onPaymentMethodSelected(PaymentMethod.DEBITO, selectedWallet.getId(), 1);
                }
                dismiss();
            });
        }
    }

    private class CreditViewHolder extends RecyclerView.ViewHolder {
        Spinner spinnerCards;
        EditText etInstallments;
        Button btnConfirm;

        CreditViewHolder(@NonNull View itemView) {
            super(itemView);
            spinnerCards = itemView.findViewById(R.id.spinnerCards);
            etInstallments = itemView.findViewById(R.id.etInstallments);
            btnConfirm = itemView.findViewById(R.id.btnConfirmCredit);
        }

        void bind() {
            List<CreditCard> cards = creditCardRepository.getCreditCards();
            boolean noCards = cards.isEmpty();

            if (noCards) {
                // Add a placeholder card if none exist
                CreditCard placeholder = new CreditCard("No hay tarjetas", 0, 0);
                placeholder.setId("PLACEHOLDER");
                cards.add(placeholder);
            }

            UniversalSpinnerAdapter<CreditCard> adapter = new UniversalSpinnerAdapter<>(requireContext(), cards);
            spinnerCards.setAdapter(adapter);

            // Pre-fill installments
            etInstallments.setText(String.valueOf(initialInstallments));

            if (restrictedToCredit) {
                etInstallments.setEnabled(false);
                etInstallments.setAlpha(0.5f);
            }

            btnConfirm.setOnClickListener(v -> {
                CreditCard selectedCard = (CreditCard) spinnerCards.getSelectedItem();
                if (selectedCard == null || "PLACEHOLDER".equals(selectedCard.getId())) {
                    Toast.makeText(requireContext(), "No hay tarjetas disponibles", Toast.LENGTH_SHORT).show();
                    return;
                }

                String cuotasStr = etInstallments.getText().toString().trim();
                int cuotas = 1;
                try {
                    cuotas = Integer.parseInt(cuotasStr);
                    if (cuotas < 1)
                        cuotas = 1;
                } catch (NumberFormatException e) {
                }

                if (listener != null) {
                    listener.onPaymentMethodSelected(PaymentMethod.CREDITO, selectedCard.getId(),
                            cuotas);
                }
                dismiss();
            });
        }
    }

    // Simple Adapter for Wallets RecyclerView
    private static class SimpleWalletAdapter extends RecyclerView.Adapter<SimpleWalletAdapter.ViewHolder> {
        private final List<com.notificationcapture.app.models.Wallets> items;
        private final OnItemClickListener listener;
        private int selectedPosition = -1;

        interface OnItemClickListener {
            void onItemClick(com.notificationcapture.app.models.Wallets item);
        }

        SimpleWalletAdapter(List<com.notificationcapture.app.models.Wallets> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            com.notificationcapture.app.models.Wallets item = items.get(position);
            holder.textView.setText(item.getAppName());

            Context context = holder.itemView.getContext();
            if (position == selectedPosition) {
                holder.textView
                        .setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.accent_main));
                holder.textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.textView
                        .setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary));
                holder.textView.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            holder.itemView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
