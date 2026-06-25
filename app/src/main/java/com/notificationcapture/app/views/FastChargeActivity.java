package com.notificationcapture.app.views;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import java.math.BigDecimal;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.utils.CustomTypeSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastChargeActivity extends AppCompatActivity {

    private EditText etTitle, etAmount, etInstallments;
    private MaterialButtonToggleGroup togglePaymentMethod;
    private CustomTypeSwitch swType;
    private LinearLayout layoutInstallments;
    private LinearLayout layoutWalletSelector;
    private Spinner spinnerWalletsFastCharge;
    private TextView tvSuccessMessage;
    private List<Wallets> availableWallets = new ArrayList<>();

    // Executor compartido para toda la Activity — no se crea uno nuevo por cada guardado.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!RepositoryProvider.isInitialized()) {
            RepositoryProvider.initialize(getApplicationContext());
        }

        setContentView(R.layout.activity_fast_charge);

        etTitle = findViewById(R.id.etFastChargeTitle);
        etAmount = findViewById(R.id.etFastChargeAmount);
        etInstallments = findViewById(R.id.etInstallments);
        swType = findViewById(R.id.swFastChargeType);
        togglePaymentMethod = findViewById(R.id.togglePaymentMethod);
        layoutInstallments = findViewById(R.id.layoutInstallments);
        layoutWalletSelector = findViewById(R.id.layoutWalletSelector);
        spinnerWalletsFastCharge = findViewById(R.id.spinnerWalletsFastCharge);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);

        Button btnCancel = findViewById(R.id.btnCancelFastCharge);
        Button btnSave = findViewById(R.id.btnSaveFastCharge);

        setupWalletsSpinner();

        togglePaymentMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                layoutInstallments.setVisibility(checkedId == R.id.btnMethodCredit ? View.VISIBLE : View.GONE);
                layoutWalletSelector.setVisibility(checkedId == R.id.btnMethodDebit ? View.VISIBLE : View.GONE);
            }
        });

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTransaction());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setupWalletsSpinner() {
        WalletRepository walletRepository = RepositoryProvider.getInstance().getWalletRepository();
        availableWallets = new ArrayList<>(walletRepository.getAllWallets());
        List<String> names = new ArrayList<>();
        for (Wallets w : availableWallets) {
            names.add(w.getAppName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWalletsFastCharge.setAdapter(adapter);
    }

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Por favor completa los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        IngresoOEgreso type = swType.isChecked() ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
        int checkedMethodId = togglePaymentMethod.getCheckedButtonId();
        long now = System.currentTimeMillis();
        String categoryId = (type == IngresoOEgreso.INGRESO) ? "other_income" : "other_outcome";

        Transaction transaction;
        if (checkedMethodId == R.id.btnMethodDebit) {
            String walletId = null;
            if (!availableWallets.isEmpty()) {
                int idx = spinnerWalletsFastCharge.getSelectedItemPosition();
                if (idx >= 0 && idx < availableWallets.size()) {
                    walletId = availableWallets.get(idx).getId();
                }
            }
            transaction = new Debit(title, "Carga rápida", now, type, categoryId, walletId, false);
        } else if (checkedMethodId == R.id.btnMethodCredit) {
            int installments = 1;
            try {
                installments = Integer.parseInt(etInstallments.getText().toString());
            } catch (Exception ignored) {}
            transaction = new Credit(title, "Carga rápida", now, type, categoryId, null,
                    installments, installments, null, false);
        } else {
            transaction = new Cash(title, "Carga rápida", now, type, categoryId, false);
        }

        transaction.setAmount(amount);

        // Usar el executor compartido de la Activity en lugar de crear uno desechable.
        final Transaction finalTransaction = transaction;
        executor.execute(() -> {
            RepositoryProvider.getInstance().getTransactionRepository().saveTransaction(finalTransaction);
            mainHandler.post(() -> {
                tvSuccessMessage.setVisibility(View.VISIBLE);
                mainHandler.postDelayed(this::finish, 1500);
            });
        });
    }
}