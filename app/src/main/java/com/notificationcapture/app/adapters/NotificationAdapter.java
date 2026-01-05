package com.notificationcapture.app.adapters;

import static com.notificationcapture.app.repositories.WalletRepository.getPackageNameFromApp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.notificationcapture.app.NotificationItem;
import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.fragments.PaymentMethodBottomSheet;
import com.notificationcapture.app.R;
import com.notificationcapture.app.fragments.SelectorBottomSheet;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.enums.TransactionType;
import com.notificationcapture.app.interfaces.OnDeleteClickListener;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> notifications;
    private final java.util.Map<String, Integer> categoryColors;
    private CategoryRepository categoryRepo;
    private TransactionRepository repository;
    private SimpleDateFormat dateFormat;
    private OnDeleteClickListener deleteListener;

    public NotificationAdapter(List<NotificationItem> notifications, java.util.Map<String, Integer> categoryColors,
            OnDeleteClickListener deleteListener) {
        this.notifications = notifications;
        this.categoryColors = categoryColors;
        this.deleteListener = deleteListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        categoryRepo = RepositoryProvider.getInstance().getCategoryRepository();
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
        NotificationItem item = notifications.get(position);
        Context context = holder.itemView.getContext();

        // Configurar visibilidad según estado de expansión
        if (item.isExpanded()) {
            holder.layoutSummary.setVisibility(View.GONE);
            holder.layoutEdit.setVisibility(View.VISIBLE);

            // Cargar datos en el formulario
            holder.etTitle.setText(item.getTitle());
            holder.etText.setText(item.getText());

            if (item.getAmount() != null) {
                holder.etAmount.setText(String.valueOf(item.getAmount()));
            } else {
                holder.etAmount.setText("");
            }

            // Configurar Switch de Tipo
            boolean isIngreso = item.getType() == TransactionType.INGRESO;
            holder.switchType.setChecked(!isIngreso); // checked = Egreso, unchecked = Ingreso

            // Configurar UI inicial
            updateToggleUI(holder, !isIngreso);

            // Configurar Selectores iniciales
            String name = item.getCategory().getName();
            holder.tvCategorySelector.setText( name != null ? name : "Sin categoría");

            String paymentText = getPaymentMethodText(item);
            holder.tvPaymentMethod.setText(paymentText);

            // Listeners para abrir BottomSheets
            holder.tvCategorySelector.setOnClickListener(v -> {
                // Chequear estado actual del switch
                boolean currentIsIngreso = !holder.switchType.isChecked();
                showCategorySelector(context, holder.tvCategorySelector, currentIsIngreso);
            });

            holder.tvPaymentMethod.setOnClickListener(v -> {
                showPaymentMethodSelector(context, holder.tvPaymentMethod, item);
            });

            // Listener para el cambio de tipo (solo actualiza UI del toggle, la categoría
            // se mantiene hasta que el usuario la cambie o valida al guardar)
            // Opcional: resetear categoría si cambia tipo
            holder.switchType.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateToggleUI(holder, isChecked);
                // Si cambia el tipo, podrías querer resetear la categoría o avisar. Por ahora
                // mantenemos la actual o actualizamos si abren el selector.
                // Al abrir el selector de nuevo, leerá el isChecked actual.
            });

            // Listeners para los textos
            holder.tvIngreso.setOnClickListener(v -> holder.switchType.setChecked(false));
            holder.tvEgreso.setOnClickListener(v -> holder.switchType.setChecked(true));

            // Configurar botones de acción
            holder.btnSave.setOnClickListener(v -> {
                // Guardar cambios
                item.setTitle(holder.etTitle.getText().toString());
                item.setText(holder.etText.getText().toString());

                // Guardar tipo
                // checked = true -> Egreso, false -> Ingreso
                boolean isEgresoSelected = holder.switchType.isChecked();
                item.setType(isEgresoSelected ? TransactionType.EGRESO
                        : TransactionType.INGRESO);

                // Guardar categoría selecccionada
                item.setCategory(new Category(holder.tvCategorySelector.getText().toString(), TransactionType.getTransactionType(holder.switchType.isChecked())));

                // Guardar billetera / Metodo de Pago
                // Los cambios ya se guardaron en el Bottom<Sheet en el objeto ¨ítem¨
                // solo debemos persistir el iitem

                // Ensure packageName is set if we have detail from payment method
                if (item.getPaymentMethod() == PaymentMethod.DEBITO) {
                    String pkg = getPackageNameFromApp(item.getPaymentMethodDetail());
                    if (pkg != null)
                        item.setPackageName(pkg);
                } else if (item.getPaymentMethod() == PaymentMethod.EFECTIVO) {
                    item.setPackageName("com.cash.payment");
                }
                // For Credit, strictly speaking it doesn't map to an app package usually,
                // but we might want to store something or leave current.

                String amountStr = holder.etAmount.getText().toString();
                if (!amountStr.isEmpty()) {
                    try {
                        item.setAmount(Double.parseDouble(amountStr));
                    } catch (NumberFormatException e) {
                        // Ignorar formato inválido o manejar error
                    }
                } else {
                    item.setAmount(null); // O 0.0
                }

                item.setExpanded(false);

                // Persistir cambios
                repository.updateTransaction(item);

                notifyItemChanged(holder.getAdapterPosition());
            });

            holder.btnCancel.setOnClickListener(v -> {
                item.setExpanded(false);
                notifyItemChanged(holder.getAdapterPosition());
            });

        } else {
            holder.layoutSummary.setVisibility(View.VISIBLE);
            holder.layoutEdit.setVisibility(View.GONE);

            // Cargar datos en el resumen
            holder.tvAppName.setText(item.getAppName());
            holder.tvCategory.setText(item.getCategory().getName());
            holder.tvTitle.setText(item.getTitle());
            holder.tvText.setText(item.getText());

            String formattedDate = dateFormat.format(new Date(item.getTimestamp()));
            holder.tvTimestamp.setText(formattedDate);

            // Mostrar el monto si existe
            if (item.hasAmount()) {
                holder.tvAmount.setVisibility(View.VISIBLE);
                holder.tvAmount.setText(TransactionType.getTypeIndicator(item.getType()) + " " + item.getFormattedAmount());
                holder.tvAmount.setTextColor(CatColors.getOneIntColorByType(item.getType(), 0));
            } else {
                holder.tvAmount.setVisibility(View.GONE);
            }

            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(item);
                }
            });

            // Click para expandir
            holder.layoutSummary.setOnClickListener(v -> {
                item.setExpanded(true);
                notifyItemChanged(holder.getAdapterPosition());
            });

            // Set Border Color
            int categoryColor = android.graphics.Color.LTGRAY; // Default
            if (categoryColors != null && item.getCategory() != null) {
                Integer color = categoryColors.get(item.getCategory());
                if (color != null) {
                    categoryColor = color;
                }
            }
            holder.viewNotificationBorder.setBackgroundColor(categoryColor);
        }
    }

    private void updateToggleUI(ViewHolder holder, boolean isEgreso) {
        Context context = holder.itemView.getContext();
        if (isEgreso) {
            // Modo Egreso (Switch ON, Derecha)
            holder.tvIngreso.setTextColor(ContextCompat.getColor(context, R.color.grey_unselected));
            holder.tvEgreso.setTextColor(ContextCompat.getColor(context, R.color.red));
        } else {
            // Modo Ingreso (Switch OFF, Izquierda)
            holder.tvIngreso.setTextColor(ContextCompat.getColor(context, R.color.green));
            holder.tvEgreso.setTextColor(ContextCompat.getColor(context, R.color.grey_unselected));
        }
    }

    private void showCategorySelector(Context context, TextView targetView, boolean isIngreso) {
        //String[] categories = isIngreso ? NotificationItem.INCOME_CATEGORIES : NotificationItem.OUTCOME_CATEGORIES;
        List<String> options =  categoryRepo.getCategoryNames(isIngreso ? TransactionType.INGRESO : TransactionType.EGRESO);
        //java.util.List<String> options = java.util.Arrays.asList(categories);

        SelectorBottomSheet sheet = SelectorBottomSheet.newInstance(
                "Seleccionar Categoría",
                options,
                targetView.getText().toString(),
                categoryColors);

        sheet.setOnOptionSelectedListener(option -> {
            targetView.setText(option);
        });

        if (context instanceof androidx.fragment.app.FragmentActivity) {
            sheet.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(),
                    "CategorySelector");
        }
    }

    private void showPaymentMethodSelector(Context context, TextView targetView, NotificationItem item) {
        if (context instanceof androidx.fragment.app.FragmentActivity) {
            PaymentMethodBottomSheet bottomSheet = new PaymentMethodBottomSheet();

            // Configure restrictions based on current payment method
            if (item.getPaymentMethod() == PaymentMethod.CREDITO) {
                bottomSheet.setRestrictedMode(true); // Restrict to CREDIT
                bottomSheet.setInitialInstallments(item.getInstallments());
            } else {
                bottomSheet.setRestrictedMode(false); // Restrict to CASH/DEBIT
                // Initial installments irrelevant for Cash/Debit or default to 1
            }

            bottomSheet.setListener((method, detail, installments) -> {
                // Update Item
                item.setPaymentMethod(method);
                item.setPaymentMethodDetail(detail);
                // Only update installments if method is CREDIT (though restricted mode ensures
                // this)
                // If it was Credit and we are in restricted mode, the installments returned
                // might be the same (since input disabled)
                if (method == PaymentMethod.CREDITO) {
                    item.setInstallments(installments);
                } else {
                    item.setInstallments(1);
                }

                // Update UI text immediately
                String displayText;
                if (method == PaymentMethod.EFECTIVO) {
                    displayText = "Efectivo";
                } else if (method == PaymentMethod.DEBITO) {
                    displayText = "Débito - " + (detail != null ? detail : "");
                } else {
                    displayText = "Crédito - " + (detail != null ? detail : "")
                            + (installments > 1 ? " (" + installments + " cuotas)" : "");
                }
                targetView.setText(displayText);
            });
            bottomSheet.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(),
                    "PaymentMethodSelector");
        }
    }

    private String getPaymentMethodText(NotificationItem item) {
        if (item.getPaymentMethod() == null) {
            // Backward compatibility or default
            return item.getAppName(); // Fallback to app name if no method set
        }

        String detail = item.getPaymentMethodDetail();
        int installments = item.getInstallments();

        if (item.getPaymentMethod() == PaymentMethod.EFECTIVO) {
            return "Efectivo";
        } else if (item.getPaymentMethod() == PaymentMethod.DEBITO) {
            return "Débito - " + (detail != null ? detail : "");
        } else {
            return "Crédito - " + (detail != null ? detail : "")
                    + (installments > 1 ? " (" + installments + " cuotas)" : "");
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateData(List<NotificationItem> newNotifications) {
        this.notifications = newNotifications;
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
        View btnDelete;

        EditText etTitle;
        EditText etText;
        EditText etAmount;
        SwitchCompat switchType;
        TextView tvIngreso;
        TextView tvEgreso;
        TextView tvCategorySelector;
        TextView tvPaymentMethod;
        Button btnSave;
        Button btnCancel;

        ViewHolder(View itemView) {
            super(itemView);
            layoutSummary = itemView.findViewById(R.id.layoutSummary);
            layoutEdit = itemView.findViewById(R.id.layoutEdit);
            viewNotificationBorder = itemView.findViewById(R.id.viewNotificationBorder);

            // Summary Views
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvText = itemView.findViewById(R.id.tvText);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Edit Views
            etTitle = itemView.findViewById(R.id.etTitle);
            etText = itemView.findViewById(R.id.etText);
            etAmount = itemView.findViewById(R.id.etAmount);
            switchType = itemView.findViewById(R.id.switchType);
            tvIngreso = itemView.findViewById(R.id.tvIngreso);
            tvEgreso = itemView.findViewById(R.id.tvEgreso);
            tvCategorySelector = itemView.findViewById(R.id.tvCategorySelector);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            btnSave = itemView.findViewById(R.id.btnSave);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}