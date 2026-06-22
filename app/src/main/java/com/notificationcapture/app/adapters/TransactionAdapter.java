package com.notificationcapture.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.notificationcapture.app.utils.CustomTypeSwitch;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.fragments.PaymentMethodBottomSheet;
import com.notificationcapture.app.R;
import com.notificationcapture.app.fragments.SelectorBottomSheet;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.interfaces.OnDeleteClickListener;
import com.notificationcapture.app.interfaces.OnAddClickListener;
import com.notificationcapture.app.utils.Dialog;
import com.notificationcapture.app.enums.DialogType;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private CategoryRepository categoryRepo;
    private com.notificationcapture.app.repositories.WalletRepository walletRepo;
    private com.notificationcapture.app.repositories.CreditCardRepository cardRepo;
    private TransactionRepository repository;
    private SimpleDateFormat dateFormat;
    private OnDeleteClickListener deleteListener;
    private OnAddClickListener addListener;
    private boolean showAddButton;
    private FragmentManager fragmentManager;

    public TransactionAdapter(List<Transaction> transactions, FragmentManager fragmentManager,
            OnDeleteClickListener deleteListener,
            OnAddClickListener addListener, boolean showAddButton) {
        this.transactions = transactions;
        this.fragmentManager = fragmentManager;
        this.deleteListener = deleteListener;
        this.addListener = addListener;
        this.showAddButton = showAddButton;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        categoryRepo = RepositoryProvider.getInstance().getCategoryRepository();
        walletRepo = RepositoryProvider.getInstance().getWalletRepository();
        cardRepo = RepositoryProvider.getInstance().getCreditCardRepository();
        repository = RepositoryProvider.getInstance().getTransactionRepository();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction item = transactions.get(position);
        Context context = holder.itemView.getContext();

        // Configurar visibilidad según estado de expansión
        if (item.isExpanded()) {
            holder.layoutSummary.setVisibility(View.GONE);
            holder.layoutEdit.setVisibility(View.VISIBLE);

            // Cargar datos en el formulario
            holder.etTitle.setText(item.getTitle());
            holder.etText.setText(item.getText());

            if (item.getAmount() != null) {
                // Format initially to EU style so it looks correct immediately
                java.text.DecimalFormat formatter = (java.text.DecimalFormat) java.text.DecimalFormat
                        .getInstance(java.util.Locale.GERMANY);
                formatter.applyPattern("#,###.##");
                String initialAmount = formatter.format(item.getAmount());
                holder.etAmount.setText(initialAmount);
            } else {
                holder.etAmount.setText("");
            }

            // Remove previous watcher if exists to avoid stacking
            if (holder.currentWatcher != null) {
                holder.etAmount.removeTextChangedListener(holder.currentWatcher);
            }
            holder.currentWatcher = new com.notificationcapture.app.utils.MoneyTextWatcher(holder.etAmount);
            holder.etAmount.addTextChangedListener(holder.currentWatcher);

            // Configurar Switch de Tipo
            boolean isIngreso = item.getType() == IngresoOEgreso.INGRESO;
            holder.switchType.setChecked(!isIngreso, true); // checked = Egreso, unchecked = Ingreso

            // *** NUEVO: Inicializar la categoría seleccionada ***
            Category currentCat = categoryRepo.getCategoryById(item.getCategoryId());
            if (currentCat == null) {
                currentCat = new Category("Otros", item.getType() != null ? item.getType() : IngresoOEgreso.EGRESO);
            }
            holder.selectedCategory = currentCat;

            // Configurar UI inicial
            updateToggleUI(holder, !isIngreso);

            // Configurar Selectores iniciales
            holder.tvCategorySelector.setText(currentCat.getName());

            String paymentText = getPaymentMethodText(item);
            holder.tvPaymentMethod.setText(paymentText);

            // Configurar Switch isNotification
            holder.switchIsNotification.setChecked(item.isNotification());

            // *** MODIFICADO: Pasar holder en lugar de solo el TextView ***
            holder.tvCategorySelector.setOnClickListener(v -> {
                boolean currentIsIngreso = !holder.switchType.isChecked();
                showCategorySelector(context, holder, currentIsIngreso);
            });

            holder.tvPaymentMethod.setOnClickListener(v -> {
                showPaymentMethodSelector(context, holder, item);
            });

            // *** MODIFICADO: Resetear categoría cuando cambia el tipo ***
            holder.switchType.setOnCheckedChangeListener(isChecked -> {
                updateToggleUI(holder, isChecked);

                boolean isIngresoNow = !isChecked;
                IngresoOEgreso newType = isIngresoNow ? IngresoOEgreso.INGRESO : IngresoOEgreso.EGRESO;
                
                boolean foundEquivalent = false;
                List<Category> newTypeCategories = categoryRepo.getCategories(newType);
                for (Category c : newTypeCategories) {
                    if (c.getName().equalsIgnoreCase(holder.selectedCategory.getName())) {
                        holder.selectedCategory = c;
                        foundEquivalent = true;
                        break;
                    }
                }
                
                if (!foundEquivalent) {
                    holder.selectedCategory = categoryRepo.getCategoryById("other");
                    if (holder.selectedCategory == null) {
                        holder.selectedCategory = new Category("other", "Otros", newType);
                    }
                }
                
                holder.tvCategorySelector.setText(holder.selectedCategory.getName());
            });

            // *** MODIFICADO: Usar selectedCategory en lugar de crear nueva ***
            holder.btnSave.setOnClickListener(v -> {
                item.setTitle(holder.etTitle.getText().toString());
                item.setText(holder.etText.getText().toString());

                // Guardar tipo
                boolean isEgresoSelected = holder.switchType.isChecked();
                item.setType(IngresoOEgreso.getTransactionType(isEgresoSelected));

                // *** USAR selectedCategory ID ***
                item.setCategoryId(holder.selectedCategory.getId());

                item.setNotification(holder.switchIsNotification.isChecked());

                if (item instanceof Debit d) {
                    item.setPaymentMethod(PaymentMethod.DEBITO);
                } else if (item instanceof Cash) {
                    item.setPaymentMethod(PaymentMethod.EFECTIVO);
                } else {
                    item.setPaymentMethod(PaymentMethod.CREDITO);
                }

                String amountStr = holder.etAmount.getText().toString();
                if (!amountStr.isEmpty()) {
                    try {
                        String clean = amountStr.replace(".", "").replace(",", ".");
                        item.setAmount(Double.parseDouble(clean));
                    } catch (NumberFormatException e) {
                        // Ignorar formato inválido
                    }
                } else {
                    item.setAmount(null);
                }

                item.setExpanded(false);
                repository.updateTransaction(item);
                notifyItemChanged(holder.getAdapterPosition());
            });

            holder.btnCancel.setOnClickListener(v -> {
                item.setExpanded(false);
                notifyItemChanged(holder.getAdapterPosition());
            });

        } else

        {
            holder.layoutSummary.setVisibility(View.VISIBLE);
            holder.layoutEdit.setVisibility(View.GONE);

            // Cargar datos en el resumen
            holder.tvAppName.setText(resolveSourceName(item));
            Category displayCat = categoryRepo.getCategoryById(item.getCategoryId());
            if (displayCat == null) {
                displayCat = new Category("Otros", item.getType() != null ? item.getType() : IngresoOEgreso.EGRESO);
            }
            holder.tvCategory.setText(displayCat.getName());
            holder.tvTitle.setText(item.getTitle());
            if (item.getText() == null || item.getText().trim().isEmpty()) {
                holder.tvText.setVisibility(View.GONE);
            } else {
                holder.tvText.setVisibility(View.VISIBLE);
                holder.tvText.setText(item.getText());
            }

            // Mostrar/Ocultar tag de notificación
            holder.tvNotificacion.setVisibility(item.isNotification() ? View.VISIBLE : View.GONE);

            String formattedDate = dateFormat.format(new Date(item.getTimestamp()));
            holder.tvTimestamp.setText(formattedDate);

            // Mostrar el monto si existe
            if (item.hasAmount()) {
                holder.tvAmount.setVisibility(View.VISIBLE);
                holder.tvAmount
                        .setText(IngresoOEgreso.getTypeIndicator(item.getType()) + " " + item.getFormattedAmount());
                holder.tvAmount.setTextColor(CatColors.getOneIntColorByType(item.getType(), 0));
            } else {
                holder.tvAmount.setVisibility(View.GONE);
            }

            // Show/hide add button based on context
            if (showAddButton) {
                holder.btnAdd.setVisibility(View.VISIBLE);
                holder.btnAdd.setOnClickListener(v -> {
                    if (addListener != null) {
                        addListener.onAddClick(item);
                    }
                });
            } else {
                holder.btnAdd.setVisibility(View.GONE);
            }

            holder.btnDelete.setOnClickListener(v -> {
                Dialog.show(
                        "¿Estás seguro de que deseas eliminar esta transacción?",
                        DialogType.CONFIRMATION,
                        () -> {
                            if (deleteListener != null) {
                                deleteListener.onDeleteClick(item);
                            }
                        });
            });

            // Click para expandir
            holder.layoutSummary.setOnClickListener(v -> {
                item.setExpanded(true);
                notifyItemChanged(holder.getAdapterPosition());
            });

            // Set Border Color
            Category borderCat = categoryRepo.getCategoryById(item.getCategoryId());
            int categoryColor = (borderCat != null) ? borderCat.getDisplayColor() : android.graphics.Color.LTGRAY;
            holder.viewNotificationBorder.setBackgroundColor(categoryColor);
        }
    }

    private void updateToggleUI(ViewHolder holder, boolean isEgreso) {
        // Switch handles its own UI now
    }

    // *** MODIFICADO: Recibe ViewHolder para actualizar selectedCategory ***
    private void showCategorySelector(Context context, ViewHolder holder, boolean isIngreso) {
        List<Category> options = categoryRepo
                .getCategories(isIngreso ? IngresoOEgreso.INGRESO : IngresoOEgreso.EGRESO);

        SelectorBottomSheet sheet = SelectorBottomSheet.newInstance(
                "Seleccionar Categoría",
                options,
                holder.tvCategorySelector.getText().toString());

        sheet.setOnOptionSelectedListener(option -> {
            // *** BUSCAR LA CATEGORÍA COMPLETA PARA PRESERVAR EL COLOR ***
            for (Category cat : options) {
                if (cat.getName().equals(option)) {
                    holder.selectedCategory = cat;
                    break;
                }
            }
            holder.tvCategorySelector.setText(option);
        });

        if (fragmentManager != null) {
            sheet.show(fragmentManager, "CategorySelector");
        }
    }

    private void showPaymentMethodSelector(Context context, ViewHolder holder, Transaction item) {
        PaymentMethodBottomSheet bottomSheet = new PaymentMethodBottomSheet();

        if (item instanceof Credit c) {
            bottomSheet.setRestrictedMode(true);
            bottomSheet.setInitialInstallments(c.getInstallments());
        }

        bottomSheet.setListener((method, detailId, installments) -> {
            // Leer valores actuales de la UI para no perder cambios no guardados
            String currentTitle = holder.etTitle.getText().toString();
            String currentText = holder.etText.getText().toString();
            boolean currentIsEgreso = holder.switchType.isChecked();
            IngresoOEgreso currentType = currentIsEgreso ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
            boolean currentIsNotification = holder.switchIsNotification.isChecked();

            String amountStr = holder.etAmount.getText().toString();
            Double currentAmount = null;
            if (!amountStr.isEmpty()) {
                try {
                    String clean = amountStr.replace(".", "").replace(",", ".");
                    currentAmount = Double.parseDouble(clean);
                } catch (Exception e) {
                }
            }

            Transaction newItem;
            if (method == PaymentMethod.EFECTIVO) {
                newItem = new Cash(currentTitle, currentText, item.getTimestamp(), currentType,
                        holder.selectedCategory.getId(), currentIsNotification);
            } else if (method == PaymentMethod.DEBITO) {
                newItem = new Debit(currentTitle, currentText, item.getTimestamp(), currentType,
                        holder.selectedCategory.getId(), detailId, currentIsNotification);
            } else {
                // CREDITO
                String groupId = (item instanceof Credit) ? ((Credit) item).getInstallmentGroupId() : null;
                int currentInstallment = (item instanceof Credit) ? ((Credit) item).getCurrentInstallment() : 1;
                newItem = new Credit(currentTitle, currentText, item.getTimestamp(), currentType,
                        holder.selectedCategory.getId(), detailId, installments, currentInstallment, groupId,
                        currentIsNotification);
            }

            // Preservar ID y monto procesado
            newItem.setId(item.getId());
            newItem.setAmount(currentAmount);
            newItem.setExpanded(true);

            // Reemplazar en la lista
            int index = transactions.indexOf(item);
            if (index != -1) {
                transactions.set(index, newItem);
                notifyItemChanged(index);
            }
        });

        if (fragmentManager != null) {
            bottomSheet.show(fragmentManager, "PaymentMethodSelector");
        }
    }

    private String getPaymentMethodText(Transaction item) {
        String detail = resolveSourceName(item);
        if (item instanceof Credit c) {
            int installments = c.getInstallments();
            return "Crédito - " + (detail != null ? detail : "")
                    + (installments > 1 ? " (" + installments + " cuotas)" : "");
        } else if (item instanceof Debit) {
            return "Débito - " + (detail != null ? detail : "");
        } else {
            return "Efectivo";
        }
    }

    private String resolveSourceName(Transaction item) {
        if (item instanceof Debit d) {
            com.notificationcapture.app.models.Wallets w = walletRepo.getWalletById(d.getWalletId());
            if (w != null)
                return w.getAppName();
            return d.getSourceName(); // Fallback to migration object name
        } else if (item instanceof Credit c) {
            com.notificationcapture.app.models.CreditCard card = cardRepo.getCreditCardById(c.getCreditCardId());
            if (card != null)
                return card.getName();
            return c.getSourceName(); // Fallback to migration object name
        } else if (item instanceof Cash) {
            return "Efectivo";
        }
        
        // Final safety fallback using the enum name if classes don't match for some reason
        if (item.getPaymentMethod() != null) {
            String name = item.getPaymentMethod().name();
            // Capitalize first letter
            return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
        
        return "Desconocido";
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateData(List<Transaction> newNotifications) {
        this.transactions = newNotifications;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutSummary;
        View layoutEdit;
        View viewNotificationBorder;

        TextView tvAppName;
        TextView tvCategory;
        TextView tvTitle;
        TextView tvText;
        TextView tvAmount;
        TextView tvTimestamp;
        TextView tvNotificacion;
        View btnAdd;
        View btnDelete;

        public android.text.TextWatcher currentWatcher;

        EditText etTitle;
        EditText etText;
        EditText etAmount;
        CustomTypeSwitch switchType;
        TextView tvCategorySelector;
        TextView tvPaymentMethod;
        SwitchCompat switchIsNotification;
        Button btnSave;
        Button btnCancel;

        // *** NUEVO: Campo para mantener la categoría seleccionada ***
        Category selectedCategory;

        ViewHolder(View itemView) {
            super(itemView);
            layoutSummary = itemView.findViewById(R.id.layoutSummary);
            layoutEdit = itemView.findViewById(R.id.layoutEdit);
            viewNotificationBorder = itemView.findViewById(R.id.viewNotificationBorder);

            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvText = itemView.findViewById(R.id.tvText);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvNotificacion = itemView.findViewById(R.id.tvNotificacion);
            btnAdd = itemView.findViewById(R.id.btnAdd);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            etTitle = itemView.findViewById(R.id.etTitle);
            etText = itemView.findViewById(R.id.etText);
            etAmount = itemView.findViewById(R.id.etAmount);
            switchType = itemView.findViewById(R.id.switchType);
            tvCategorySelector = itemView.findViewById(R.id.tvCategorySelector);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            switchIsNotification = itemView.findViewById(R.id.switchIsNotification);
            btnSave = itemView.findViewById(R.id.btnSave);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}

// Se puede modularizar y simplificar para quer sea mas legible el "if
// (item.isExpanded()) { ... }"?
