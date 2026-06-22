package com.notificationcapture.app.fragments;

// ARCHIVO NUEVO: app/src/main/java/com/notificationcapture/app/fragments/TransferenciaFragment.java

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.utils.MoneyTextWatcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransferenciaFragment extends Fragment {

    private EditText etAmount;
    private Spinner spinnerOrigen;
    private Spinner spinnerDestino;
    private TextInputEditText etDate;
    private TransactionRepository repository;
    private WalletRepository walletRepository;

    // Lista unificada: wallets del usuario + efectivo
    private List<AccountItem> accounts = new ArrayList<>();
    private long selectedDateTimestamp;

    // Modelo interno simple para unificar wallets y efectivo
    private static class AccountItem {
        final String id;   // null = efectivo
        final String name;
        AccountItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transferencia, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = RepositoryProvider.getInstance().getTransactionRepository();
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();

        etAmount = view.findViewById(R.id.etTransferAmount);
        spinnerOrigen = view.findViewById(R.id.spinnerOrigen);
        spinnerDestino = view.findViewById(R.id.spinnerDestino);
        etDate = view.findViewById(R.id.etTransferDate);

        etAmount.addTextChangedListener(new MoneyTextWatcher(etAmount));

        // Cargar cuentas disponibles
        accounts.clear();
        accounts.add(new AccountItem(null, "Efectivo"));
        for (Wallets w : walletRepository.getAllWallets()) {
            accounts.add(new AccountItem(w.getId(), w.getAppName()));
        }

        ArrayAdapter<AccountItem> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, accounts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrigen.setAdapter(adapter);
        spinnerDestino.setAdapter(adapter);

        // Selección inicial diferente para evitar mismo origen/destino
        if (accounts.size() > 1) spinnerDestino.setSelection(1);

        // Fecha por defecto = hoy
        selectedDateTimestamp = System.currentTimeMillis();
        updateDateField(selectedDateTimestamp);
        etDate.setOnClickListener(v -> showDatePicker());

        view.findViewById(R.id.btnRegistrarTransferencia).setOnClickListener(v -> registrarTransferencia());
    }

    private void registrarTransferencia() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "El monto es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(amountStr.replace(".", "").replace(",", "."));
            if (monto <= 0) {
                Toast.makeText(requireContext(), "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Formato de monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        int origenPos = spinnerOrigen.getSelectedItemPosition();
        int destinoPos = spinnerDestino.getSelectedItemPosition();

        if (origenPos == destinoPos) {
            Toast.makeText(requireContext(), "El origen y destino no pueden ser iguales", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountItem origen = accounts.get(origenPos);
        AccountItem destino = accounts.get(destinoPos);

        // Para efectivo usamos el ID especial "EFECTIVO", para wallets usamos su ID
        String origenId = origen.id != null ? origen.id : "EFECTIVO";
        String destinoId = destino.id != null ? destino.id : "EFECTIVO";

        repository.saveTransfer(origenId, destinoId, monto, selectedDateTimestamp, result -> {
            if (result.isSuccess()) {
                Toast.makeText(requireContext(),
                        "✅ Transferencia de $" + MoneyTextWatcher.format(monto) + " registrada",
                        Toast.LENGTH_SHORT).show();

                // Limpiar formulario
                etAmount.setText("");
                spinnerOrigen.setSelection(0);
                if (accounts.size() > 1) spinnerDestino.setSelection(1);
                selectedDateTimestamp = System.currentTimeMillis();
                updateDateField(selectedDateTimestamp);
            } else {
                Toast.makeText(requireContext(), "❌ Error al registrar la transferencia", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(selectedDateTimestamp)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(selection);
            Calendar localCal = Calendar.getInstance();
            localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
            localCal.set(Calendar.MILLISECOND, 0);
            selectedDateTimestamp = localCal.getTimeInMillis();
            updateDateField(selectedDateTimestamp);
        });

        datePicker.show(getParentFragmentManager(), "TRANSFER_DATE_PICKER");
    }

    private void updateDateField(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new Date(timestamp)));
    }
}
