package com.notificationcapture.app.views;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.utils.CustomTypeSwitch;

import java.util.concurrent.Executors;

public class FastChargeActivity extends AppCompatActivity {

    private EditText etTitle, etAmount, etInstallments;
    private MaterialButtonToggleGroup togglePaymentMethod;
    private CustomTypeSwitch swType;
    private LinearLayout layoutInstallments;
    private TextView tvSuccessMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar RepositoryProvider en caso de que la app haya sido matada
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
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);

        Button btnCancel = findViewById(R.id.btnCancelFastCharge);
        Button btnSave = findViewById(R.id.btnSaveFastCharge);

        // Lógica para mostrar campo de cuotas si se selecciona Crédito
        togglePaymentMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                layoutInstallments.setVisibility(checkedId == R.id.btnMethodCredit ? View.VISIBLE : View.GONE);
            }
        });

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTransaction());
    }



    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Por favor completa los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        IngresoOEgreso type = swType.isChecked() ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;

        int checkedMethodId = togglePaymentMethod.getCheckedButtonId();
        Transaction transaction;
        long now = System.currentTimeMillis();
        String categoryId = (type == IngresoOEgreso.INGRESO) ? "other_income" : "other_outcome";

        if (checkedMethodId == R.id.btnMethodDebit) {
            transaction = new Debit(title, "Carga rápida", now, type, categoryId, null, false);
        } else if (checkedMethodId == R.id.btnMethodCredit) {
            int installments = 1;
            try {
                installments = Integer.parseInt(etInstallments.getText().toString());
            } catch (Exception ignored) {}
            transaction = new Credit(title, "Carga rápida", now, type, categoryId, null, installments, installments, null, false);
        } else {
            transaction = new Cash(title, "Carga rápida", now, type, categoryId, false);
        }

        transaction.setAmount(amount);

        Executors.newSingleThreadExecutor().execute(() -> {
            RepositoryProvider.getInstance().getTransactionRepository().saveTransaction(transaction);
            runOnUiThread(() -> {
                tvSuccessMessage.setVisibility(View.VISIBLE);
                // Cerrar después de un breve delay para que el usuario vea el mensaje
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
            });
        });
    }
}
