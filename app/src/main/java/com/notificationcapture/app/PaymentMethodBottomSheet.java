package com.notificationcapture.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.notificationcapture.app.models.CreditCard;

import java.util.List;

public class PaymentMethodBottomSheet extends BottomSheetDialogFragment {

    public interface PaymentMethodListener {
        void onPaymentMethodSelected(NotificationItem.PaymentMethod method, String detail, int installments);
    }

    private PaymentMethodListener listener;
    private NotificationRepository repository;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_payment_method_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new NotificationRepository(requireContext());

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
                    listener.onPaymentMethodSelected(NotificationItem.PaymentMethod.EFECTIVO, null, 1);
                }
                dismiss();
            });
        }
    }

    private class DebitViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerWallets;

        DebitViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerWallets = itemView.findViewById(R.id.recyclerWallets);
            recyclerWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        void bind() {
            List<String> wallets = repository.getWallets();
            // Using a simple adapter for wallets directly here
            SimpleTextAdapter adapter = new SimpleTextAdapter(wallets, walletName -> {
                if (listener != null) {
                    listener.onPaymentMethodSelected(NotificationItem.PaymentMethod.DEBITO, walletName, 1);
                }
                dismiss();
            });
            recyclerWallets.setAdapter(adapter);
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
            List<CreditCard> cards = repository.getCreditCards();

            // Need a displayable list for UniversalSpinnerAdapter or just manual
            // To keep it simple, let's use UniversalSpinnerAdapter logic or create a list

            // Assuming we reuse Universal but might need a context.
            // Better to just use a simple ArrayAdapter for now or reuse Universal if
            // possible.
            // UniversalSpinnerAdapter usage:
            UniversalSpinnerAdapter<CreditCard> adapter = new UniversalSpinnerAdapter<>(requireContext(), cards);
            spinnerCards.setAdapter(adapter);

            // Pre-fill installments
            etInstallments.setText(String.valueOf(initialInstallments));

            // Disable installments if restricted (Edit Mode for Credit)
            // Wait, logic says: "si es credito, que solo se pueda cambiar la tarjeta de
            // credito (las cantidad de cuotas no)"
            if (restrictedToCredit) {
                etInstallments.setEnabled(false);
                etInstallments.setAlpha(0.5f);
                // Maybe set a hint explaining why
            }

            btnConfirm.setOnClickListener(v -> {
                CreditCard selectedCard = (CreditCard) spinnerCards.getSelectedItem();
                if (selectedCard == null) {
                    Toast.makeText(requireContext(), "Selecciona una tarjeta", Toast.LENGTH_SHORT).show();
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

                // If restricted, we might want to preserve original installments,
                // but the bottom sheet doesn't know original.
                // The caller (NotificationAdapter) handles updating the item.
                // The BottomSheet returns the value from EditText.
                // If disabled, user can't change it, so it returns default (1) or whatever is
                // there.
                // We should probably pass existing installments to the sheet if possible to
                // pre-fill?
                // The user request is specifically that they CANNOT change it.
                // So returning 0 or -1 to indicate "no change" might be better, or just return
                // existing if we had it.
                // For now, if disabled, returning what's in box (default 1 or user needs to see
                // original).
                // Let's settle for returning the value, trusting it wasn't changed if disabled.
                // Ideally we should pre-fill it. For now, let's assume the user doesn't see the
                // original value here yet.

                if (listener != null) {
                    listener.onPaymentMethodSelected(NotificationItem.PaymentMethod.CREDITO, selectedCard.getName(),
                            cuotas);
                }
                dismiss();
            });
        }
    }

    // Simple Adapter for Wallets RecyclerView
    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(String item);
        }

        SimpleTextAdapter(List<String> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using simple list item
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = items.get(position);
            holder.textView.setText(item);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
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
