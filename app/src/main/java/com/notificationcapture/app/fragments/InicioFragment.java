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

import com.notificationcapture.app.adapters.TransactionAdapter;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.enums.IngresoOEgreso;

public class InicioFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TextView emptyView;

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

        repository = RepositoryProvider.getInstance().getTransactionRepository();
        categoryRepository = RepositoryProvider.getInstance().getCategoryRepository();

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        tvIngresos = view.findViewById(R.id.tvIngresos);
        tvEgresos = view.findViewById(R.id.tvEgresos);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TransactionAdapter(new ArrayList<>(), item -> {
            repository.deleteTransaction(item.getId());
            loadNotifications();
        }, null, false); // showAddButton = false para transacciones aprobadas
        recyclerView.setAdapter(adapter);

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
        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationReceiver != null) {
            requireContext().unregisterReceiver(notificationReceiver);
        }
    }

    private void loadNotifications() {
        List<Transaction> allNotifications = repository.getAllTransactions();

        // Filtrar solo las notificaciones del mes actual
        List<Transaction> currentMonthNotifications = getCurrentMonthNotifications(allNotifications);
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

    private List<Transaction> getCurrentMonthNotifications(List<Transaction> allNotifications) {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        List<Transaction> currentMonthNotifications = new ArrayList<>();

        for (Transaction item : allNotifications) {
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

    private void updateFinancialSummary(List<Transaction> currentMonthNotifications) {
        double totalIngresos = 0;
        double totalEgresos = 0;

        for (Transaction item : currentMonthNotifications) {
            if (item.hasAmount()) {
                if (item.getType() == IngresoOEgreso.INGRESO) {
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
        return "$" + com.notificationcapture.app.utils.MoneyTextWatcher.format(amount);
    }
}