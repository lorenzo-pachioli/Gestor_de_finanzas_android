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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

        // Configure visibility based on expanded state
        if (item.isExpanded()) {
            holder.getLayoutSummary().setVisibility(View.GONE);
            holder.getLayoutEdit().setVisibility(View.VISIBLE);

            // Load data in the form
            holder.getEtTitle().setText(item.getTitle());
            holder.getEtText().setText(item.getText());

            if (item.getAmount() != null) {
                // Format initially to EU style so it looks correct immediately
                java.text.DecimalFormat formatter = (java.text.DecimalFormat) java.text.DecimalFormat
                        .getInstance(java.util.Locale.GERMANY);
                formatter.applyPattern("#,###.##");
                String initialAmount = formatter.format(item.getAmount().doubleValue());
                holder.getEtAmount().setText(initialAmount);
            } else {
                holder.getEtAmount().setText("");
            }

            // Remove previous watcher if exists to avoid stacking
            if (holder.currentWatcher != null) {
                holder.getEtAmount().removeTextChangedListener(holder.currentWatcher);
            }
            holder.currentWatcher = new com.notificationcapture.app.utils.MoneyTextWatcher(holder.getEtAmount());
            holder.getEtAmount().addTextChangedListener(holder.currentWatcher);

            // Configure Type Switch
            boolean isIngreso = item.getType() == IngresoOEgreso.INGRESO;
            holder.getSwitchType().setChecked(!isIngreso, true); // checked = Egreso, unchecked = Ingreso

            // *** NEW: Initialize selected category ***
            Category currentCat = categoryRepo.getCategoryById(item.getCategoryId());
            if (currentCat == null) {
                currentCat = new Category("Otros", item.getType() != null ? item.getType() : IngresoOEgreso.EGRESO);
            }
            holder.setSelectedCategory(currentCat);

            // Configure initial UI
            updateToggleUI(holder, !isIngreso);

            // Configure initial selectors
            holder.getTvCategorySelector().setText(currentCat.getName());

            String paymentText = getPaymentMethodText(item);
            holder.getTvPaymentMethod().setText(paymentText);

            // Configure isNotification switch
            holder.getSwitchIsNotification().setChecked(item.isNotification());

            // *** MODIFIED: Pass holder instead of just TextView ***
            holder.getTvCategorySelector().setOnClickListener(v -> {
                boolean currentIsIngreso = !holder.getSwitchType().isChecked();
                showCategorySelector(context, holder, currentIsIngreso);
            });

            holder.getTvPaymentMethod().setOnClickListener(v -> {
                showPaymentMethodSelector(context, holder, item);
            });

            // *** MODIFIED: Reset category when type changes ***
            holder.getSwitchType().setOnCheckedChangeListener(isChecked -> {
                updateToggleUI(holder, isChecked);

                boolean isIngresoNow = !isChecked;
                IngresoOEgreso newType = isIngresoNow ? IngresoOEgreso.INGRESO : IngresoOEgreso.EGRESO;
                
                boolean foundEquivalent = false;
                List<Category> newTypeCategories = categoryRepo.getCategories(newType);
                Map<String, Category> categoryMap = new HashMap<>();
                for (Category c : newTypeCategories) {
                    categoryMap.put(c.getName().toLowerCase(), c);
                }
                Category found = categoryMap.get(holder.getSelectedCategory().getName().toLowerCase());
                if (found != null) {
                    holder.setSelectedCategory(found);
                    foundEquivalent = true;
                }
                
                if (!foundEquivalent) {
                    String otherId = isIngresoNow ? CategoryRepository.OTHER_INCOME_ID : CategoryRepository.OTHER_OUTCOME_ID;
                    holder.setSelectedCategory(categoryRepo.getCategoryById(otherId));
                    if (holder.getSelectedCategory() == null) {
                        holder.setSelectedCategory(new Category(otherId, "Otros", newType));
                    }
                }
                
                holder.getTvCategorySelector().setText(holder.getSelectedCategory().getName());
            });

            // *** MODIFIED: Use selectedCategory instead of creating new ***
            holder.getBtnSave().setOnClickListener(v -> {
                item.setTitle(holder.getEtTitle().getText().toString());
                item.setText(holder.getEtText().getText().toString());

                // Save type
                boolean isEgresoSelected = holder.getSwitchType().isChecked();
                item.setType(IngresoOEgreso.getTransactionType(isEgresoSelected));

                // *** USE selectedCategory ID ***
                item.setCategoryId(holder.getSelectedCategory().getId());

                item.setNotification(holder.getSwitchIsNotification().isChecked());

                if (item instanceof Debit d) {
                    item.setPaymentMethod(PaymentMethod.DEBITO);
                } else if (item instanceof Cash) {
                    item.setPaymentMethod(PaymentMethod.EFECTIVO);
                } else {
                    item.setPaymentMethod(PaymentMethod.CREDITO);
                }

                String amountStr = holder.getEtAmount().getText().toString();
                if (!amountStr.isEmpty()) {
                    try {
                        String clean = amountStr.replace(".", "").replace(",", ".");
                        item.setAmount(new BigDecimal(clean));
                    } catch (NumberFormatException e) {
                        // Ignore invalid format
                    }
                } else {
                    item.setAmount(null);
                }

                item.setExpanded(false);
                repository.updateTransaction(item);
                notifyItemChanged(holder.getAdapterPosition());
            });

            holder.getBtnCancel().setOnClickListener(v -> {
                item.setExpanded(false);
                notifyItemChanged(holder.getAdapterPosition());
            });

        } else

        {
            holder.getLayoutSummary().setVisibility(View.VISIBLE);
            holder.getLayoutEdit().setVisibility(View.GONE);

            // Load data in summary
            holder.getTvAppName().setText(resolveSourceName(item));
            Category displayCat = categoryRepo.getCategoryById(item.getCategoryId());
            if (displayCat == null) {
                displayCat = new Category("Otros", item.getType() != null ? item.getType() : IngresoOEgreso.EGRESO);
            }
            holder.getTvCategory().setText(displayCat.getName());
            holder.getTvTitle().setText(item.getTitle());
            if (item.getText() == null || item.getText().trim().isEmpty()) {
                holder.getTvText().setVisibility(View.GONE);
            } else {
                holder.getTvText().setVisibility(View.VISIBLE);
                holder.getTvText().setText(item.getText());
            }

            // Show/hide notification tag
            holder.getTvNotificacion().setVisibility(item.isNotification() ? View.VISIBLE : View.GONE);

            String formattedDate = dateFormat.format(new Date(item.getTimestamp()));
            holder.getTvTimestamp().setText(formattedDate);

            // Show amount if exists
            if (item.hasAmount()) {
                holder.getTvAmount().setVisibility(View.VISIBLE);
                holder.getTvAmount()
                        .setText(IngresoOEgreso.getTypeIndicator(item.getType()) + " " + item.getFormattedAmount());
                holder.getTvAmount().setTextColor(CatColors.getOneIntColorByType(item.getType(), 0));
            } else {
                holder.getTvAmount().setVisibility(View.GONE);
            }

            // Show/hide add button based on context
            if (showAddButton) {
                holder.getBtnAdd().setVisibility(View.VISIBLE);
                holder.getBtnAdd().setOnClickListener(v -> {
                    if (addListener != null) {
                        addListener.onAddClick(item);
                    }
                });
            } else {
                holder.getBtnAdd().setVisibility(View.GONE);
            }

            holder.getBtnDelete().setOnClickListener(v -> {
                Dialog.show(
                        "¿Estás seguro de que deseas eliminar esta transacción?",
                        DialogType.CONFIRMATION,
                        () -> {
                            if (deleteListener != null) {
                                deleteListener.onDeleteClick(item);
                            }
                        });
            });

            // Click to expand
            holder.getLayoutSummary().setOnClickListener(v -> {
                item.setExpanded(true);
                notifyItemChanged(holder.getAdapterPosition());
            });

            // Set Border Color
            Category borderCat = categoryRepo.getCategoryById(item.getCategoryId());
            int categoryColor = (borderCat != null) ? borderCat.getDisplayColor() : android.graphics.Color.LTGRAY;
            holder.getViewNotificationBorder().setBackgroundColor(categoryColor);
        }
    }

    private void updateToggleUI(ViewHolder holder, boolean isEgreso) {
        // Switch handles its own UI now
    }

    // *** MODIFIED: Receives ViewHolder to update selectedCategory ***
    private void showCategorySelector(Context context, ViewHolder holder, boolean isIngreso) {
        List<Category> options = categoryRepo
                .getCategories(isIngreso ? IngresoOEgreso.INGRESO : IngresoOEgreso.EGRESO);

        SelectorBottomSheet sheet = SelectorBottomSheet.newInstance(
                "Seleccionar Categoría",
                options,
                holder.getTvCategorySelector().getText().toString());

        sheet.setOnOptionSelectedListener(option -> {
            // *** FIND COMPLETE CATEGORY TO PRESERVE COLOR ***
            for (Category cat : options) {
                if (cat.getName().equals(option)) {
                    holder.setSelectedCategory(cat);
                    break;
                }
            }
            holder.getTvCategorySelector().setText(option);
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
            // Read current UI values to not lose unsaved changes
            String currentTitle = holder.getEtTitle().getText().toString();
            String currentText = holder.getEtText().getText().toString();
            boolean currentIsEgreso = holder.getSwitchType().isChecked();
            IngresoOEgreso currentType = currentIsEgreso ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
            boolean currentIsNotification = holder.getSwitchIsNotification().isChecked();

            String amountStr = holder.getEtAmount().getText().toString();
            BigDecimal currentAmount = null;
            if (!amountStr.isEmpty()) {
                try {
                    String clean = amountStr.replace(".", "").replace(",", ".");
                    currentAmount = new BigDecimal(clean);
                } catch (Exception e) {
                }
            }

            Transaction newItem;
            if (method == PaymentMethod.EFECTIVO) {
                newItem = new Cash(currentTitle, currentText, item.getTimestamp(), currentType,
                        holder.getSelectedCategory().getId(), currentIsNotification);
            } else if (method == PaymentMethod.DEBITO) {
                newItem = new Debit(currentTitle, currentText, item.getTimestamp(), currentType,
                        holder.getSelectedCategory().getId(), detailId, currentIsNotification);
            } else {
                // CREDIT
                String groupId = (item instanceof Credit) ? ((Credit) item).getInstallmentGroupId() : null;
                int currentInstallment = (item instanceof Credit) ? ((Credit) item).getCurrentInstallment() : 1;
                newItem = new Credit(currentTitle, currentText, item.getTimestamp(), currentType,
                        holder.getSelectedCategory().getId(), detailId, installments, currentInstallment, groupId,
                        currentIsNotification);
            }

            // Preserve ID and processed amount
            newItem.setId(item.getId());
            newItem.setAmount(currentAmount);
            newItem.setExpanded(true);

            // Replace in list
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
            return PaymentMethod.DISPLAY_CASH;
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
            return PaymentMethod.DISPLAY_CASH;
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
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TransactionDiffCallback(this.transactions, newNotifications));
        this.transactions = newNotifications;
        diffResult.dispatchUpdatesTo(this);
    }

    static class TransactionDiffCallback extends DiffUtil.Callback {
        private final List<Transaction> oldList;
        private final List<Transaction> newList;

        TransactionDiffCallback(List<Transaction> oldList, List<Transaction> newList) {
            this.oldList = oldList != null ? oldList : new ArrayList<>();
            this.newList = newList != null ? newList : new ArrayList<>();
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Transaction oldItem = oldList.get(oldItemPosition);
            Transaction newItem = newList.get(newItemPosition);
            return oldItem != null && newItem != null && oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Transaction oldItem = oldList.get(oldItemPosition);
            Transaction newItem = newList.get(newItemPosition);
            if (oldItem == null || newItem == null) return oldItem == newItem;
            if (!Objects.equals(oldItem.getTitle(), newItem.getTitle())) return false;
            if (!Objects.equals(oldItem.getText(), newItem.getText())) return false;
            if (oldItem.getTimestamp() != newItem.getTimestamp()) return false;
            if (!Objects.equals(oldItem.getAmount(), newItem.getAmount())) return false;
            if (oldItem.getType() != newItem.getType()) return false;
            if (!Objects.equals(oldItem.getCategoryId(), newItem.getCategoryId())) return false;
            if (oldItem.getPaymentMethod() != newItem.getPaymentMethod()) return false;
            if (oldItem.isNotification() != newItem.isNotification()) return false;
            return true;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View layoutSummary;
        private View layoutEdit;
        private View viewNotificationBorder;

        private TextView tvAppName;
        private TextView tvCategory;
        private TextView tvTitle;
        private TextView tvText;
        private TextView tvAmount;
        private TextView tvTimestamp;
        private TextView tvNotificacion;
        private View btnAdd;
        private View btnDelete;

        public android.text.TextWatcher currentWatcher;

        private EditText etTitle;
        private EditText etText;
        private EditText etAmount;
        private CustomTypeSwitch switchType;
        private TextView tvCategorySelector;
        private TextView tvPaymentMethod;
        private SwitchCompat switchIsNotification;
        private Button btnSave;
        private Button btnCancel;

        // *** NUEVO: Campo para mantener la categoría seleccionada ***
        private Category selectedCategory;

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

        public View getLayoutSummary() {
            return layoutSummary;
        }

        public View getLayoutEdit() {
            return layoutEdit;
        }

        public View getViewNotificationBorder() {
            return viewNotificationBorder;
        }

        public TextView getTvAppName() {
            return tvAppName;
        }

        public TextView getTvCategory() {
            return tvCategory;
        }

        public TextView getTvTitle() {
            return tvTitle;
        }

        public TextView getTvText() {
            return tvText;
        }

        public TextView getTvAmount() {
            return tvAmount;
        }

        public TextView getTvTimestamp() {
            return tvTimestamp;
        }

        public TextView getTvNotificacion() {
            return tvNotificacion;
        }

        public View getBtnAdd() {
            return btnAdd;
        }

        public View getBtnDelete() {
            return btnDelete;
        }

        public EditText getEtTitle() {
            return etTitle;
        }

        public EditText getEtText() {
            return etText;
        }

        public EditText getEtAmount() {
            return etAmount;
        }

        public CustomTypeSwitch getSwitchType() {
            return switchType;
        }

        public TextView getTvCategorySelector() {
            return tvCategorySelector;
        }

        public TextView getTvPaymentMethod() {
            return tvPaymentMethod;
        }

        public SwitchCompat getSwitchIsNotification() {
            return switchIsNotification;
        }

        public Button getBtnSave() {
            return btnSave;
        }

        public Button getBtnCancel() {
            return btnCancel;
        }

        public Category getSelectedCategory() {
            return selectedCategory;
        }

        public void setSelectedCategory(Category selectedCategory) {
            this.selectedCategory = selectedCategory;
        }
    }
}

// Se puede modularizar y simplificar para quer sea mas legible el "if
// (item.isExpanded()) { ... }"?
