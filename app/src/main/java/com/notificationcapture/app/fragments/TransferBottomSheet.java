package com.notificationcapture.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.notificationcapture.app.R;
import com.notificationcapture.app.interfaces.TransferListener;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.utils.MoneyTextWatcher;

import java.util.ArrayList;
import java.util.List;

public class TransferBottomSheet extends BottomSheetDialogFragment {

    private TransferListener listener;
    private WalletRepository walletRepository;

    private EditText etAmount;
    private Spinner spinnerOrigen;
    private Spinner spinnerDestino;
    private Button btnMover;

    private List<AccountItem> allAccounts = new ArrayList<>();
    private ArrayAdapter<AccountItem> origenAdapter;
    private ArrayAdapter<AccountItem> destinoAdapter;

    private boolean isInitializing = true;
    private boolean isUpdating = false;

    private static class AccountItem {
        final String id;
        final String name;
        AccountItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        @Override public String toString() {
            return name;
        }
    }

    public void setListener(TransferListener listener) {
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
        return inflater.inflate(R.layout.layout_transfer_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();

        etAmount = view.findViewById(R.id.etAmount);
        spinnerOrigen = view.findViewById(R.id.spinnerOrigen);
        spinnerDestino = view.findViewById(R.id.spinnerDestino);
        btnMover = view.findViewById(R.id.btnMover);

        etAmount.addTextChangedListener(new MoneyTextWatcher(etAmount));

        origenAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new ArrayList<AccountItem>());
        origenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinoAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new ArrayList<AccountItem>());
        destinoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        loadAccounts();
        configureSpinners();

        btnMover.setOnClickListener(v -> onMoverClick());
    }

    private void loadAccounts() {
        allAccounts.clear();
        allAccounts.add(new AccountItem(null, requireContext().getString(R.string.efectivo)));
        for (Wallets w : walletRepository.getAllWallets()) {
            allAccounts.add(new AccountItem(w.getId(), w.getAppName()));
        }
    }

    private void configureSpinners() {
        isInitializing = true;

        for (AccountItem item : allAccounts) {
            origenAdapter.add(item);
        }

        if (allAccounts.size() > 1) {
            spinnerDestino.setSelection(1);
        }

        spinnerOrigen.setAdapter(origenAdapter);
        spinnerDestino.setAdapter(destinoAdapter);

        spinnerOrigen.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isInitializing || isUpdating) return;
                isUpdating = true;
                updateDestinationOptions();
                isUpdating = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDestino.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isInitializing || isUpdating) return;
                isUpdating = true;
                updateOriginOptions();
                isUpdating = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        isInitializing = false;
    }

    private void updateDestinationOptions() {
        AccountItem selectedOrigen = (AccountItem) spinnerOrigen.getSelectedItem();
        if (selectedOrigen == null) return;

        AccountItem currentDestino = (AccountItem) spinnerDestino.getSelectedItem();
        destinoAdapter.clear();

        int newPos = -1;
        int index = 0;
        for (AccountItem item : allAccounts) {
            if (!isSameAccount(item, selectedOrigen)) {
                destinoAdapter.add(item);
                if (currentDestino != null && isSameAccount(item, currentDestino)) {
                    newPos = index;
                }
                index++;
            }
        }

        if (newPos >= 0 && newPos < destinoAdapter.getCount()) {
            spinnerDestino.setSelection(newPos);
        } else if (destinoAdapter.getCount() > 0) {
            spinnerDestino.setSelection(0);
        }
    }

    private void updateOriginOptions() {
        AccountItem selectedDestino = (AccountItem) spinnerDestino.getSelectedItem();
        if (selectedDestino == null) return;

        AccountItem currentOrigen = (AccountItem) spinnerOrigen.getSelectedItem();
        origenAdapter.clear();

        int newPos = -1;
        int index = 0;
        for (AccountItem item : allAccounts) {
            if (!isSameAccount(item, selectedDestino)) {
                origenAdapter.add(item);
                if (currentOrigen != null && isSameAccount(item, currentOrigen)) {
                    newPos = index;
                }
                index++;
            }
        }

        if (newPos >= 0 && newPos < origenAdapter.getCount()) {
            spinnerOrigen.setSelection(newPos);
        } else if (origenAdapter.getCount() > 0) {
            spinnerOrigen.setSelection(0);
        }
    }

    private boolean isSameAccount(AccountItem a, AccountItem b) {
        if (a == null || b == null) return a == b;
        return a.id == null ? b.id == null : a.id.equals(b.id);
    }

    private void onMoverClick() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), R.string.monto_positivo, Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(amountStr.replace(".", "").replace(",", "."));
            if (monto <= 0) {
                Toast.makeText(requireContext(), R.string.monto_positivo, Toast.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), R.string.monto_invalido, Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }

        AccountItem origen = (AccountItem) spinnerOrigen.getSelectedItem();
        AccountItem destino = (AccountItem) spinnerDestino.getSelectedItem();

        if (origen == null || destino == null) {
            Toast.makeText(requireContext(), R.string.same_account_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSameAccount(origen, destino)) {
            Toast.makeText(requireContext(), R.string.same_account_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String origenId = origen.id != null ? origen.id : "EFECTIVO";
        String destinoId = destino.id != null ? destino.id : "EFECTIVO";

        if (listener != null) {
            listener.onTransferConfirmed(origenId, destinoId, monto);
        }

        etAmount.setText("");
        dismiss();
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
}
