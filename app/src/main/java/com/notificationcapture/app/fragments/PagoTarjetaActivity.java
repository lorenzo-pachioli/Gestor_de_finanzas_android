package com.notificationcapture.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.notificationcapture.app.R;
import com.notificationcapture.app.database.AppDatabase;
import com.notificationcapture.app.database.CreditCardPaymentEntity;
import com.notificationcapture.app.database.TransactionEntity;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.ResumenDeudaTarjeta;
import com.notificationcapture.app.models.SaldoCuenta;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.utils.AppLogger;
import com.notificationcapture.app.viewmodels.ResumenDeudaViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class PagoTarjetaActivity extends AppCompatActivity {

    public static final String EXTRA_CREDIT_CARD_ID = "creditCardId";
    public static final String EXTRA_CARD_NAME = "cardName";
    public static final String EXTRA_START_TIMESTAMP = "startTimestamp";
    public static final String EXTRA_END_TIMESTAMP = "endTimestamp";

    private ResumenDeudaTarjeta resumen;
    private String creditCardId;
    private String cardName;
    private ResumenDeudaViewModel viewModel;

    // Views
    private TextView tvGastosDelMes, tvArrastre, tvPagosRegistrados;
    private TextView tvTotalAPagar;
    private View layoutArrastre, layoutPagosRegistrados;
    private TextInputEditText etMontoPago;
    private Spinner spinnerCuentas;
    private MaterialButton btnConfirmar;
    private List<Wallets> availableWallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pago_tarjeta);

        creditCardId = getIntent().getStringExtra(EXTRA_CREDIT_CARD_ID);
        cardName = getIntent().getStringExtra(EXTRA_CARD_NAME);
        long start = getIntent().getLongExtra(EXTRA_START_TIMESTAMP, 0);
        long end = getIntent().getLongExtra(EXTRA_END_TIMESTAMP, 0);

        initUI();
        setupWalletsSpinner();
        
        AppDatabase db = AppDatabase.getDatabase(this);
        viewModel = new ViewModelProvider(this).get(ResumenDeudaViewModel.class);
        viewModel.init(creditCardId, cardName, start, end, db.transactionDao(), db.creditCardPaymentDao());

        viewModel.getResumen().observe(this, res -> {
            this.resumen = res;
            updateUI(res);
        });
    }

    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.pago_de_tarjeta, cardName));
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvGastosDelMes = findViewById(R.id.tv_gastos_del_mes);
        tvArrastre = findViewById(R.id.tv_arrastre);
        tvPagosRegistrados = findViewById(R.id.tv_pagos_registrados);
        tvTotalAPagar = findViewById(R.id.tv_total_a_pagar);
        layoutArrastre = findViewById(R.id.layout_arrastre);
        layoutPagosRegistrados = findViewById(R.id.layout_pagos_registrados);
        etMontoPago = findViewById(R.id.et_monto_pago);
        spinnerCuentas = findViewById(R.id.spinner_cuentas);
        btnConfirmar = findViewById(R.id.btn_confirmar);
        
        findViewById(R.id.btn_cancelar).setOnClickListener(v -> finish());
        btnConfirmar.setOnClickListener(v -> confirmPayment());
    }


    private void updateUI(ResumenDeudaTarjeta resumen) {
        if (resumen == null) return;
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.pago_de_tarjeta, resumen.getCardName()));
        }

        tvGastosDelMes.setText(formatCurrency(resumen.getGastosDelMes()));
        
        double arrastre = resumen.getArrastreAnterior();
//        Toast.makeText(PagoTarjetaActivity.this, "Arrastre " + arrastre, Toast.LENGTH_SHORT).show();
//        tvArrastre.setText(formatCurrency(arrastre));
//        layoutArrastre.setVisibility(View.VISIBLE);
//
//        double pagos = resumen.getPagosDelMes();
//        Toast.makeText(PagoTarjetaActivity.this, "Pagos " + pagos, Toast.LENGTH_SHORT).show();
//
//        tvPagosRegistrados.setText("-" + formatCurrency(pagos));
//        layoutPagosRegistrados.setVisibility(View.VISIBLE);

        if (Math.abs(arrastre) > 0.001) {
            tvArrastre.setText(formatCurrency(arrastre));
            layoutArrastre.setVisibility(View.VISIBLE);
        } else {
            layoutArrastre.setVisibility(View.GONE);
        }

        if (resumen.getPagosDelMes() > 0) {
            tvPagosRegistrados.setText("-" + formatCurrency(resumen.getPagosDelMes()));
            layoutPagosRegistrados.setVisibility(View.VISIBLE);
        } else {
            layoutPagosRegistrados.setVisibility(View.GONE);
        }

        double total = resumen.getDeudaTotal();
        tvTotalAPagar.setText(formatCurrency(total));
        etMontoPago.setHint(String.format("%.2f", total).replace(",", "."));
        btnConfirmar.setEnabled(total > 0.01);
    }

    private String formatCurrency(double value) {
        return String.format("$%.2f", value);
    }

    private void confirmPayment() {
        String montoStr = etMontoPago.getText().toString();
        if (montoStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.monto_invalido), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double monto = Double.parseDouble(montoStr.replace(",", "."));
            if (monto <= 0) {
                Toast.makeText(this, getString(R.string.monto_positivo), Toast.LENGTH_SHORT).show();
                return;
            }

            if (monto > resumen.getDeudaTotal() + 0.05) {
                Toast.makeText(this, getString(R.string.monto_excede_deuda), Toast.LENGTH_SHORT).show();
                return;
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirmar_pago))
                    .setMessage(getString(R.string.monto_a_pagar_label) + ": " + formatCurrency(monto))
                    .setPositiveButton(getString(R.string.confirmar), (dialog, which) -> executePayment(monto))
                    .setNegativeButton(getString(R.string.cancelar), null)
                    .show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.monto_invalido), Toast.LENGTH_SHORT).show();
        }
    }

    private void executePayment(double montoAPagar) {
        int selectedIndex = spinnerCuentas.getSelectedItemPosition();
        Wallets selectedWallet = availableWallets.get(selectedIndex);
        PaymentMethod paymentMethod = "EFECTIVO".equals(selectedWallet.getId())
                ? PaymentMethod.EFECTIVO : PaymentMethod.DEBITO;
        String walletId = "EFECTIVO".equals(selectedWallet.getId()) ? null : selectedWallet.getId();
        long now = (System.currentTimeMillis() / 1000) * 1000; // Truncar milisegundos a 000
        
        CreditCardPaymentEntity pago = new CreditCardPaymentEntity(
                resumen.getCreditCardId(),
                resumen.getMesStart(), resumen.getMesEnd(),
                resumen.getGastosDelMes() + resumen.getArrastreAnterior(),
                montoAPagar, walletId, now
        );

        TransactionEntity gastoBilletera = new TransactionEntity(
                UUID.randomUUID().toString(), paymentMethod,
                "Pago Tarjeta " + resumen.getCardName(), "Pago realizado",
                now, montoAPagar, IngresoOEgreso.EGRESO,
                CategoryRepository.PAGO_TARJETA_ID, false, TransactionEntity.STATUS_APPROVED
        );
        gastoBilletera.setWalletId(walletId);

        TransactionRepository repo = RepositoryProvider.getInstance().getTransactionRepository();
        repo.efectuarPagoTarjeta(pago, gastoBilletera, result -> {
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(PagoTarjetaActivity.this, getString(R.string.pago_exitoso), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(PagoTarjetaActivity.this, getString(R.string.error_pago), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupWalletsSpinner() {
        WalletRepository walletRepository = RepositoryProvider.getInstance().getWalletRepository();
        availableWallets = new ArrayList<>(walletRepository.getAllWallets());
        Wallets efectivo = new Wallets("EFECTIVO", getString(R.string.efectivo), "com.notificationcapture.app.efectivo");
        availableWallets.add(0, efectivo);
        List<String> names = new ArrayList<>();
        for (Wallets w : availableWallets) names.add(w.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCuentas.setAdapter(adapter);
    }
}
