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
import com.notificationcapture.app.utils.CustomTypeSwitch;
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
    private EditText etAmount;
    private CustomTypeSwitch swType;
    private Spinner spinnerCategory;
    private Button btnCreate;
    private TextInputEditText etDate;
    private TextView tvPaymentMethod;
    private TransactionRepository repository;
    private CategoryRepository categoryRepository;
    private com.notificationcapture.app.repositories.WalletRepository walletRepository;
    private com.notificationcapture.app.repositories.CreditCardRepository creditCardRepository;
    private long selectedDateTimestamp;

    private PaymentMethod selectedMethod = PaymentMethod.EFECTIVO;
    private String selectedMethodDetailId = "";
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
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();
        creditCardRepository = RepositoryProvider.getInstance().getCreditCardRepository();

        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod);
        etTitle = view.findViewById(R.id.etTitle);
        etAmount = view.findViewById(R.id.etAmount);
        etAmount.addTextChangedListener(new com.notificationcapture.app.utils.MoneyTextWatcher(etAmount));
        etAmount.setHint("0");
        etAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                // Hide Keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                etAmount.clearFocus();
                return true;
            }
            return false;
        });
        swType = view.findViewById(R.id.swType);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnCreate = view.findViewById(R.id.btnCreate);
        etDate = view.findViewById(R.id.etDate);

        // Configurar fecha por defecto (hoy)
        selectedDateTimestamp = System.currentTimeMillis();
        updateDateField(selectedDateTimestamp);

        // Configurar selector de método de pago
        tvPaymentMethod.setOnClickListener(v -> showPaymentMethodBottomSheet());

        // Configurar listener del switch
        swType.setOnCheckedChangeListener(this::updateToggleUI);

        // Estado inicial (Egreso = true/checked)
        swType.setChecked(true, false); // Inicia en Egreso sin animación
        updateToggleUI(true);

        // Configurar selector de fecha
        etDate.setOnClickListener(v -> showDatePicker());

        btnCreate.setOnClickListener(v -> createNotification());
    }

    private void updateToggleUI(boolean isEgreso) {
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

        java.util.List<Category> categories = categoryRepository.getCategories(type);
        UniversalSpinnerAdapter<Category> adapterCategories = new UniversalSpinnerAdapter<>(
                requireContext(), categories);
        spinnerCategory.setAdapter(adapterCategories);
    }

    private void createNotification() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        // CAMBIO: Usar el objeto Category completo del spinner en lugar de solo el
        // nombre
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();

        // Validación de categoría
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Debe seleccionar una categoría", Toast.LENGTH_SHORT).show();
            spinnerCategory.requestFocus();
            return;
        }

        // Validaciones
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show();
            etTitle.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "El monto es obligatorio", Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }

        double amountValue = 0;
        try {
            // Remove thousands separators (dots) and replace decimal separator (comma) with
            // dot for parsing
            String cleanAmount = amountStr.replace(".", "").replace(",", ".");
            amountValue = Double.parseDouble(cleanAmount);

            if (amountValue <= 0) {
                Toast.makeText(requireContext(), "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Formato de monto inválido", Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
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
            String itemDescription = getString(R.string.sin_descripcion);

            Transaction transaction = null;

            if (selectedMethod == PaymentMethod.CREDITO) {
                // Create Credit Transaction
                transaction = new Credit(itemTitle, itemDescription, itemTimestamp, type,
                        selectedCategory.getId(), selectedMethodDetailId, totalInstallments, i, installmentGroupId,
                        false);
            } else if (selectedMethod == PaymentMethod.DEBITO) {
                // Create Debit Transaction
                transaction = new Debit(itemTitle, itemDescription, itemTimestamp, type,
                        selectedCategory.getId(), selectedMethodDetailId, false);
            } else {
                // Create Cash Transaction
                transaction = new Cash(itemTitle, itemDescription, itemTimestamp, type,
                        selectedCategory.getId(), false);
            }

            transaction.setAmount(amountValue);
            repository.saveTransaction(transaction);

            // Add 1 month for next installment
            calendar.add(java.util.Calendar.MONTH, 1);
        }

        // Notificar actualización
        android.content.Intent intent = new android.content.Intent("com.notificationcapture.NEW_NOTIFICATION");
        requireContext().sendBroadcast(intent);

        // Limpiar formulario
        etTitle.setText("");
        etAmount.setText("");

        // Reset Payment Method
        selectedMethod = PaymentMethod.EFECTIVO;
        selectedMethodDetailId = "";
        selectedInstallments = 1;
        tvPaymentMethod.setText("Efectivo");

        spinnerCategory.setSelection(0);
        swType.setChecked(true, true); // Reset to Egreso with animation
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
        bottomSheet.setListener((method, detailId, installments) -> {
            this.selectedMethod = method;
            this.selectedMethodDetailId = detailId != null ? detailId : "";
            this.selectedInstallments = installments;

            String displayText;
            if (method == PaymentMethod.EFECTIVO) {
                displayText = "Efectivo";
            } else if (method == PaymentMethod.DEBITO) {
                Wallets w = walletRepository.getWalletById(selectedMethodDetailId);
                displayText = "Débito - " + (w != null ? w.getAppName() : "Wallet");
            } else {
                CreditCard c = creditCardRepository.getCreditCardById(selectedMethodDetailId);
                displayText = "Crédito - " + (c != null ? c.getName() : "Tarjeta")
                        + (installments > 1 ? " (" + installments + " cuotas)" : "");
            }
            tvPaymentMethod.setText(displayText);
        });
        bottomSheet.show(getParentFragmentManager(), "PaymentMethodBottomSheet");
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