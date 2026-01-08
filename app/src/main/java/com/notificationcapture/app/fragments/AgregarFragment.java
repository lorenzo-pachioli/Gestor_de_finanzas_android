package com.notificationcapture.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.CreditCard;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.Category;

public class AgregarFragment extends Fragment {

    private EditText etTitle;
    private EditText etText;
    private SwitchCompat swType;
    private TextView tvIngreso;
    private TextView tvEgreso;
    private Spinner spinnerCategory;
    private Button btnCreate;
    private TextInputEditText etDate;
    private TextView tvPaymentMethod;
    private TransactionRepository repository;
    private CategoryRepository categoryRepository;
    private long selectedDateTimestamp;

    private PaymentMethod selectedMethod = PaymentMethod.EFECTIVO;
    private String selectedMethodDetail = "";
    private int selectedInstallments = 1;

    // Removed hardcoded walletApps

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agregar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = RepositoryProvider.getInstance().getTransactionRepository();
        categoryRepository = RepositoryProvider.getInstance().getCategoryRepository();

        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod);
        etTitle = view.findViewById(R.id.etTitle);
        etText = view.findViewById(R.id.etText);
        swType = view.findViewById(R.id.swType);
        tvIngreso = view.findViewById(R.id.tvIngreso);
        tvEgreso = view.findViewById(R.id.tvEgreso);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnCreate = view.findViewById(R.id.btnCreate);
        etDate = view.findViewById(R.id.etDate);

        // Configurar fecha por defecto (hoy)
        selectedDateTimestamp = System.currentTimeMillis();
        updateDateField(selectedDateTimestamp);

        // Configurar selector de método de pago
        tvPaymentMethod.setOnClickListener(v -> showPaymentMethodBottomSheet());

        // Configurar listener del switch
        swType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateToggleUI(isChecked);
        });

        // Listeners para los textos
        tvIngreso.setOnClickListener(v -> swType.setChecked(false));
        tvEgreso.setOnClickListener(v -> swType.setChecked(true));

        // Estado inicial (Egreso = true/checked, Ingreso = false/unchecked)
        // Por defecto queremos que inicie en Egreso si así estaba antes, o lo que
        // definamos.
        // El xml tiene checked="false" (Ingreso). Si queremos Egreso por defecto:
        swType.setChecked(true); // Inicia en Egreso
        updateToggleUI(true);

        // Configurar spinner de categorías
        // updateToggleUI ya llama a configurarSpinnerCat

        // Configurar selector de fecha
        etDate.setOnClickListener(v -> showDatePicker());

        btnCreate.setOnClickListener(v -> createNotification());
    }

    private void updateToggleUI(boolean isEgreso) {
        if (isEgreso) {
            // Modo Egreso (Switch ON, Derecha)
            tvIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_unselected));
            tvEgreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.red)); // Usar color definido si
                                                                                          // existe, o hardcodeado
                                                                                          // temporalmente si no carga
        } else {
            // Modo Ingreso (Switch OFF, Izquierda)
            tvIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
            tvEgreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_unselected));
        }
        configurarSpinnerCat();
    }

    private void configurarSpinnerCat() {
        // Configurar spinner de categorías based on switch
        // isChecked = true -> Egreso
        // isChecked = false -> Ingreso
        boolean isEgreso = swType.isChecked();

        IngresoOEgreso type = isEgreso
                ? IngresoOEgreso.EGRESO
                : IngresoOEgreso.INGRESO;

        java.util.List<com.notificationcapture.app.models.Category> categories = categoryRepository.getCategories(type);
        UniversalSpinnerAdapter<Category> adapterCategories = new UniversalSpinnerAdapter<>(
                requireContext(), categories);
        spinnerCategory.setAdapter(adapterCategories);
    }

    private void createNotification() {
        String title = etTitle.getText().toString().trim();
        String text = etText.getText().toString().trim();
        com.notificationcapture.app.models.Category selectedCategoryObj = (com.notificationcapture.app.models.Category) spinnerCategory
                .getSelectedItem();
        String category = selectedCategoryObj != null ? selectedCategoryObj.getName() : "Sin Categoría";

        // Validaciones
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show();
            etTitle.requestFocus();
            return;
        }

        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "El texto es obligatorio", Toast.LENGTH_SHORT).show();
            etText.requestFocus();
            return;
        }

        // Determinar tipo de transacción
        IngresoOEgreso type = swType.isChecked()
                ? IngresoOEgreso.EGRESO
                : IngresoOEgreso.INGRESO;

        // Loop for installments
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateTimestamp);

        int totalInstallments = selectedInstallments > 0 ? selectedInstallments : 1;

        // Generate Installment Group ID if installments > 1
        String installmentGroupId = totalInstallments > 1 ? java.util.UUID.randomUUID().toString() : null;

        for (int i = 1; i <= totalInstallments; i++) {
            long itemTimestamp = calendar.getTimeInMillis();
            String itemTitle = title + (totalInstallments > 1 ? " (" + i + "/" + totalInstallments + ")" : "");

            Transaction transaction = null;

            if (selectedMethod == PaymentMethod.CREDITO) {
                // Create Credit Transaction
                CreditCard card = new CreditCard(
                        selectedMethodDetail != null ? selectedMethodDetail : "Tarjeta de Crédito", 1, 0);
                transaction = new Credit(itemTitle, text, itemTimestamp, type,
                        new Category(category, type), card, totalInstallments, i, installmentGroupId, false);
            } else if (selectedMethod == PaymentMethod.DEBITO) {
                // Create Debit Transaction
                String pkg = getPackageNameFromApp(selectedMethodDetail);
                Wallets wallet = new Wallets(selectedMethodDetail != null ? selectedMethodDetail : "Wallet", pkg);
                transaction = new Debit(itemTitle, text, itemTimestamp, type,
                        new Category(category, type), wallet, false);
            } else {
                // Create Cash Transaction
                transaction = new Cash(itemTitle, text, itemTimestamp, type,
                        new Category(category, type), false);
            }

            // Assign Amount ?? Transaction constructor extracts amount from title/text.
            // NOTE: The previous code didn't assign amount explicitly from logic, it relied
            // on constructor extraction.
            // But if user enters generic text, amount might be null.
            // Does AgregarFragment have Amount field? NO. It seems it relies on parsing
            // title/text for amount?
            // Wait, looking at AgregarFragment UI... NO visible Amount field in
            // `onViewCreated` or imports!
            // Wait, `etText` might contain "$100".
            // However, NotificationItem constructor `extractAmount(title, text)` does the
            // job.
            // Transaction constructor does `this.amount = extractAmount(title, text);`.
            // So if user puts amount in title/text, it works.

            repository.saveTransaction(transaction);

            // Add 1 month for next installment
            calendar.add(java.util.Calendar.MONTH, 1);
        }

        // Notificar actualización
        android.content.Intent intent = new android.content.Intent("com.notificationcapture.NEW_NOTIFICATION");
        requireContext().sendBroadcast(intent);

        // Limpiar formulario
        etTitle.setText("");
        etText.setText("");

        // Reset Payment Method
        selectedMethod = PaymentMethod.EFECTIVO;
        selectedMethodDetail = "";
        selectedInstallments = 1;
        tvPaymentMethod.setText("Efectivo");

        spinnerCategory.setSelection(0);
        swType.setChecked(true); // Reset to Egreso
        updateToggleUI(true);
        // Resetear fecha a hoy
        selectedDateTimestamp = System.currentTimeMillis();
        updateDateField(selectedDateTimestamp);

        // Confirmación
        String typeText = type == IngresoOEgreso.INGRESO ? "Ingreso" : "Egreso";
        Toast.makeText(requireContext(),
                "✅ " + typeText + " creado exitosamente",
                Toast.LENGTH_SHORT).show();
    }

    private void showPaymentMethodBottomSheet() {
        PaymentMethodBottomSheet bottomSheet = new PaymentMethodBottomSheet();
        bottomSheet.setListener((method, detail, installments) -> {
            this.selectedMethod = method;
            this.selectedMethodDetail = detail != null ? detail : "";
            this.selectedInstallments = installments;

            String displayText;
            if (method == PaymentMethod.EFECTIVO) {
                displayText = "Efectivo";
            } else if (method == PaymentMethod.DEBITO) {
                displayText = "Débito - " + selectedMethodDetail;
            } else {
                displayText = "Crédito - " + selectedMethodDetail
                        + (installments > 1 ? " (" + installments + " cuotas)" : "");
            }
            tvPaymentMethod.setText(displayText);
        });
        bottomSheet.show(getParentFragmentManager(), "PaymentMethodBottomSheet");
    }

    private String getPackageNameFromApp(String appName) {
        switch (appName) {
            case "Mercado Pago":
                return "com.mercadopago.wallet";
            case "Ualá":
                return "com.uala.app";
            case "Brubank":
                return "brubank.app";
            case "Naranja X":
                return "com.naranja.app";
            case "Modo":
                return "com.reba.contactless";
            case "Personal Pay":
                return "personal.pay";
            case "Bimo":
                return "bimo.app";
            case "BIND":
                return "ar.com.bind";
            case "Prex":
                return "ar.com.prex";
            case "Wilobank":
                return "ar.wilobank";
            case "Santander Río":
                return "ar.com.santander.rio";
            case "BBVA":
                return "com.bbva.nxt_argentina";
            case "Galicia":
                return "ar.com.bancogalicia";
            case "Macro":
                return "com.macro";
            case "Banco Nación":
                return "ar.com.bna";
            case "Mi Argentina":
                return "ar.gov.anses.mi";
            case "Claro Pay":
                return "com.claro.pay";
            default:
                return "com.wallet.custom";
        }
    }

    private void showDatePicker() {
        // Crear el MaterialDatePicker con la fecha seleccionada actual
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(selectedDateTimestamp)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateTimestamp = selection;
            updateDateField(selection);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDateField(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedDate = sdf.format(new Date(timestamp));
        etDate.setText(formattedDate);
    }
}