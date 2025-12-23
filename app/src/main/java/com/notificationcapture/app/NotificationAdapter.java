package com.notificationcapture.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> notifications;
    private final java.util.Map<String, Integer> categoryColors;
    private SimpleDateFormat dateFormat;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(NotificationItem item);
    }

    public NotificationAdapter(List<NotificationItem> notifications, java.util.Map<String, Integer> categoryColors,
            OnDeleteClickListener deleteListener) {
        this.notifications = notifications;
        this.categoryColors = categoryColors;
        this.deleteListener = deleteListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
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
            boolean isIngreso = item.getType() == NotificationItem.TransactionType.INGRESO;
            holder.switchType.setChecked(isIngreso);

            // Configurar Spinner de Categoría inicial
            setupCategorySpinner(context, holder.spinnerCategory, isIngreso, item.getCategory());

            // Listener para el cambio de tipo
            holder.switchType.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Actualizar opciones del spinner al cambiar el tipo
                setupCategorySpinner(context, holder.spinnerCategory, isChecked, null);
            });

            // Configurar botones de acción
            holder.btnSave.setOnClickListener(v -> {
                // Guardar cambios
                item.setTitle(holder.etTitle.getText().toString());
                item.setText(holder.etText.getText().toString());

                // Guardar tipo
                boolean isIngresoSelected = holder.switchType.isChecked();
                item.setType(isIngresoSelected ? NotificationItem.TransactionType.INGRESO
                        : NotificationItem.TransactionType.EGRESO);

                // Guardar categoría selecccionada
                if (holder.spinnerCategory.getSelectedItem() != null) {
                    item.setCategory(holder.spinnerCategory.getSelectedItem().toString());
                }

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
                NotificationRepository repository = new NotificationRepository(context);
                repository.updateNotification(item);

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
            holder.tvCategory.setText(item.getCategory());
            holder.tvTitle.setText(item.getTitle());
            holder.tvText.setText(item.getText());

            String formattedDate = dateFormat.format(new Date(item.getTimestamp()));
            holder.tvTimestamp.setText(formattedDate);

            // Mostrar el monto si existe
            if (item.hasAmount()) {
                holder.tvAmount.setVisibility(View.VISIBLE);

                // Agregar indicador de tipo
                String typeIndicator = item.getType() == NotificationItem.TransactionType.INGRESO
                        ? "+"
                        : "-";
                holder.tvAmount.setText(typeIndicator + " " + item.getFormattedAmount());

                // Color según tipo
                int color = item.getType() == NotificationItem.TransactionType.INGRESO
                        ? 0xFF4CAF50 // Verde para ingresos
                        : 0xFFF44336; // Rojo para egresos
                holder.tvAmount.setTextColor(color);
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

    private void setupCategorySpinner(Context context, Spinner spinner, boolean isIngreso, String currentCategory) {
        String[] categories = isIngreso ? NotificationItem.INCOME_CATEGORIES : NotificationItem.OUTCOME_CATEGORIES;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                R.layout.spinner_item,
                categories);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Intentar seleccionar la categoría actual si existe en la lista
        if (currentCategory != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(currentCategory)) {
                    spinner.setSelection(i);
                    break;
                }
            }
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
        Switch switchType;
        Spinner spinnerCategory;
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
            spinnerCategory = itemView.findViewById(R.id.spinnerCategory);
            btnSave = itemView.findViewById(R.id.btnSave);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}