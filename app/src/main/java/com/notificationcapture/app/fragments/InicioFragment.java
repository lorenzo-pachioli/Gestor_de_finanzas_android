package com.notificationcapture.app.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.notificationcapture.app.adapters.NotificationAdapter;
import com.notificationcapture.app.NotificationItem;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.enums.TransactionType;

public class InicioFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private TextView emptyView;
    private Button btnEnableAccess;
    private TextView tvIngresos;
    private TextView tvEgresos;
    private TextView tvBalance;
    private TextView tvMonthTitle;
    private TransactionRepository repository;
    private CategoryRepository categoryRepository;
    private BroadcastReceiver notificationReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new TransactionRepository(requireContext());
        categoryRepository = new CategoryRepository(requireContext());

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        btnEnableAccess = view.findViewById(R.id.btnEnableAccess);
        tvIngresos = view.findViewById(R.id.tvIngresos);
        tvEgresos = view.findViewById(R.id.tvEgresos);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Prepare colors
        List<Category> allCategories = categoryRepository.getAllCategories();
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : allCategories) {
            colorMap.put(c.getName(), c.getColor());
        }

        adapter = new NotificationAdapter(new ArrayList<>(), colorMap, item -> {
            repository.deleteTransaction(item.getId());
            loadNotifications();
        });
        recyclerView.setAdapter(adapter);

        btnEnableAccess.setOnClickListener(v -> showPermissionDialog());

        checkNotificationPermission();
        loadNotifications();

        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadNotifications();
            }
        };

        IntentFilter filter = new IntentFilter("com.notificationcapture.NEW_NOTIFICATION");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkNotificationPermission();
        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationReceiver != null) {
            requireContext().unregisterReceiver(notificationReceiver);
        }
    }

    private void checkNotificationPermission() {
        if (!isNotificationServiceEnabled()) {
            btnEnableAccess.setVisibility(View.VISIBLE);
        } else {
            btnEnableAccess.setVisibility(View.GONE);
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = requireContext().getPackageName();
        final String flat = Settings.Secure.getString(requireContext().getContentResolver(),
                "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (String name : names) {
                if (name.contains(pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permiso Requerido")
                .setMessage(
                        "Esta aplicación necesita acceso a las notificaciones para poder capturarlas y mostrarlas.\n\n"
                                +
                                "Por favor, habilita el acceso en la siguiente pantalla buscando esta aplicación y activando el permiso.")
                .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadNotifications() {
        List<NotificationItem> allNotifications = repository.getAllTransactions();

        // Filtrar solo las notificaciones del mes actual
        List<NotificationItem> currentMonthNotifications = getCurrentMonthNotifications(allNotifications);
        adapter.updateData(currentMonthNotifications);

        if (currentMonthNotifications.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Actualizar el título del mes
        String monthName = getMonthName();
        tvMonthTitle.setText(getString(R.string.resumen_de, monthName));

        // Actualizar el resumen financiero
        updateFinancialSummary(currentMonthNotifications);
    }

    private List<NotificationItem> getCurrentMonthNotifications(List<NotificationItem> allNotifications) {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        List<NotificationItem> currentMonthNotifications = new ArrayList<>();

        for (NotificationItem item : allNotifications) {
            Calendar itemDate = Calendar.getInstance();
            itemDate.setTimeInMillis(item.getTimestamp());

            // Verificar si es del mes y año actual
            if (itemDate.get(Calendar.MONTH) == currentMonth &&
                    itemDate.get(Calendar.YEAR) == currentYear) {
                currentMonthNotifications.add(item);
            }
        }

        return currentMonthNotifications;
    }

    private String getMonthName() {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);

        int[] monthResIds = {
                R.string.enero, R.string.febrero, R.string.marzo,
                R.string.abril, R.string.mayo, R.string.junio,
                R.string.julio, R.string.agosto, R.string.septiembre,
                R.string.octubre, R.string.noviembre, R.string.diciembre
        };

        return getString(monthResIds[currentMonth]);
    }

    private void updateFinancialSummary(List<NotificationItem> currentMonthNotifications) {
        double totalIngresos = 0;
        double totalEgresos = 0;

        for (NotificationItem item : currentMonthNotifications) {
            if (item.hasAmount()) {
                if (item.getType() == TransactionType.INGRESO) {
                    totalIngresos += item.getAmount();
                } else {
                    totalEgresos += item.getAmount();
                }
            }
        }

        double balance = totalIngresos - totalEgresos;

        // Formatear y mostrar
        tvIngresos.setText(formatAmount(totalIngresos));
        tvEgresos.setText(formatAmount(totalEgresos));
        tvBalance.setText(formatAmount(Math.abs(balance)));

        // Cambiar color del balance según sea positivo o negativo
        if (balance >= 0) {
            tvBalance.setTextColor(getResources().getColor(R.color.green));
        } else {
            tvBalance.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private String formatAmount(double amount) {
        return String.format("$%.2f", amount)
                .replace(",", "#")
                .replace(".", ",")
                .replace("#", ".");
    }
}