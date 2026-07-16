package com.notificationcapture.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.notificationcapture.app.R;
import com.notificationcapture.app.constants.CategoryConstants;
import com.notificationcapture.app.constants.NotificationConstants;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.CreditCard;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.CreditCardRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.utils.CustomTypeSwitch;
import com.notificationcapture.app.utils.MoneyTextWatcher;
import com.notificationcapture.app.services.ServiceProvider;

public class AgregarFragment extends Fragment {

    private EditText etTitle;
    private EditText etAmount;
    private CustomTypeSwitch swType;
    private Spinner spinnerCategory;
    private Button btnCreate;
    private TextInputEditText etDate;
    private TextView tvPaymentMethod;
    private TransactionRepository repository;
    private WalletRepository walletRepository;
    private CreditCardRepository creditCardRepository;
    private long selectedDateTimestamp;

    private PaymentMethod selectedMethod = PaymentMethod.EFECTIVO;
    private String selectedMethodDetailId = "";
    private int selectedInstallments = 1;

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
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();
        creditCardRepository = RepositoryProvider.getInstance().getCreditCardRepository();

        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod);
        etTitle = view.findViewById(R.id.etTitle);
        etAmount = view.findViewById(R.id.etAmount);
        etAmount.addTextChangedListener(new MoneyTextWatcher(etAmount));
        etAmount.setHint("0");
        etAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm =
                        (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                etAmount.clearFocus();
                return true;
            }
            return false;
        });
        swType = view.findViewById(R.id.swType);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnCreate = view.findViewById(R.id.btnCreate);
        etDate = view.findViewById(R.id.etDate);

        TextView tvTransferLink = view.findViewById(R.id.tvTransferLink);
        if (tvTransferLink != null) {
            tvTransferLink.setOnClickListener(v -> {
                TransferBottomSheet bottomSheet = new TransferBottomSheet();
                bottomSheet.setListener((origenId, destinoId, monto) -> {
                    long timestamp = System.currentTimeMillis();
                    repository.saveTransfer(origenId, destinoId, monto, timestamp, result -> {
                        if (result.isSuccess()) {
                            Toast.makeText(requireContext(),
                                    "✅ " + getString(R.string.transfer_success) + " ($" + MoneyTextWatcher.format(monto) + ")",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "❌ Error al registrar la transferencia", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                bottomSheet.show(getParentFragmentManager(), "TransferBottomSheet");
            });
        }

        selectedDateTimestamp = System.currentTimeMillis();
        updateDateField(selectedDateTimestamp);

        tvPaymentMethod.setOnClickListener(v -> showPaymentMethodBottomSheet());
        swType.setOnCheckedChangeListener(this::updateToggleUI);
        swType.setChecked(true, false);
        updateToggleUI(true);
        etDate.setOnClickListener(v -> showDatePicker());
        btnCreate.setOnClickListener(v -> createNotification());

        // Aplicar paddingBottom dinámico según la barra de navegación real del dispositivo.
        // Antes era un setPadding(..., 0) fijo que ignoraba el nav bar en dispositivos físicos.
        int bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.size_60dp);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    bottomNavHeight + systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    private void updateToggleUI(boolean isEgreso) {
        configurarSpinnerCat();
    }

    private void configurarSpinnerCat() {
        boolean isEgreso = swType.isChecked();
        IngresoOEgreso type = isEgreso ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
        List<Category> categories = ServiceProvider.getInstance()
                .getCategoryService()
                .getCategoriesForType(type);
        UniversalSpinnerAdapter<Category> adapterCategories = new UniversalSpinnerAdapter<>(requireContext(), categories);
        
        String currentSelectionName = "";
        if (spinnerCategory.getSelectedItem() != null) {
            currentSelectionName = ((Category)spinnerCategory.getSelectedItem()).getName();
        }
        
        spinnerCategory.setAdapter(adapterCategories);
        
        boolean found = false;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getName().equalsIgnoreCase(currentSelectionName)) {
                spinnerCategory.setSelection(i);
                found = true;
                break;
            }
        }
        
        if (!found) {
            for (int i = 0; i < categories.size(); i++) {
                if (CategoryConstants.OTHER_INCOME_ID.equals(categories.get(i).getId())
                        || CategoryConstants.OTHER_OUTCOME_ID.equals(categories.get(i).getId())
                        || "Otros".equalsIgnoreCase(categories.get(i).getName())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }
    }

    private void createNotification() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Debe seleccionar una categoría", Toast.LENGTH_SHORT).show();
            spinnerCategory.requestFocus();
            return;
        }
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

        BigDecimal amountValue = MoneyTextWatcher.parse(amountStr);
        if (amountValue == null) {
            Toast.makeText(requireContext(), "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }

        IngresoOEgreso type = swType.isChecked() ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateTimestamp);
        int totalInstallments = selectedInstallments > 0 ? selectedInstallments : 1;
        String installmentGroupId = totalInstallments > 1 ? UUID.randomUUID().toString() : null;

        for (int i = 1; i <= totalInstallments; i++) {
            long itemTimestamp = calendar.getTimeInMillis();
            String itemTitle = title + (totalInstallments > 1 ? " (" + i + "/" + totalInstallments + ")" : "");
            Transaction transaction;

            if (selectedMethod == PaymentMethod.CREDITO) {
                transaction = new Credit(itemTitle, "", itemTimestamp, type,
                        selectedCategory.getId(), selectedMethodDetailId, totalInstallments, i,
                        installmentGroupId, false);
            } else if (selectedMethod == PaymentMethod.DEBITO) {
                transaction = new Debit(itemTitle, "", itemTimestamp, type,
                        selectedCategory.getId(), selectedMethodDetailId, false);
            } else {
                transaction = new Cash(itemTitle, "", itemTimestamp, type,
                        selectedCategory.getId(), false);
            }
            transaction.setAmount(amountValue);
            repository.saveTransaction(transaction);
            calendar.add(Calendar.MONTH, 1);
        }

        Intent intent = new Intent(NotificationConstants.ACTION_NEW_NOTIFICATION);
        requireContext().sendBroadcast(intent);

        etTitle.setText("");
        etAmount.setText("");
        selectedMethod = PaymentMethod.EFECTIVO;
        selectedMethodDetailId = "";
        selectedInstallments = 1;
        tvPaymentMethod.setText("Efectivo");
        spinnerCategory.setSelection(0);
        swType.setChecked(true, true);
        updateToggleUI(true);
        selectedDateTimestamp = System.currentTimeMillis();
        updateDateField(selectedDateTimestamp);

        String typeText = type == IngresoOEgreso.INGRESO ? "Ingreso" : "Egreso";
        Toast.makeText(requireContext(), "✅ " + typeText + " creado exitosamente", Toast.LENGTH_SHORT).show();
    }

    private void showPaymentMethodBottomSheet() {
        PaymentMethodBottomSheet bottomSheet = new PaymentMethodBottomSheet();
        bottomSheet.setListener((method, detailId, installments) -> {
            this.selectedMethod = method;
            this.selectedMethodDetailId = detailId != null ? detailId : "";
            this.selectedInstallments = installments;
            String displayText;
            if (method == PaymentMethod.EFECTIVO) {
                displayText = PaymentMethod.DISPLAY_CASH;
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

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDateField(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new Date(timestamp)));
    }
}
