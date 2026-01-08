package com.notificationcapture.app.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
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

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TransactionRepository repository;
    private CategoryRepository categoryRepository;
    private TransactionAdapter adapter;
    private Button btnEnableAccess;

    public HistorialFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = RepositoryProvider.getInstance().getTransactionRepository();
        categoryRepository = RepositoryProvider.getInstance().getCategoryRepository();

        emptyView = view.findViewById(R.id.emptyViewHistorial);
        recyclerView = view.findViewById(R.id.recyclerViewHistorial);
        btnEnableAccess = view.findViewById(R.id.btnEnableAccess);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Prepare colors
        List<Category> allCategories = categoryRepository.getAllCategories();
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : allCategories) {
            colorMap.put(c.getName(), c.getDisplayColor());
        }

        adapter = new TransactionAdapter(new ArrayList<>(), colorMap, (item) -> {
            // Callback para eliminar notificación
            repository.deleteTransaction(item.getId());
            loadNotifications();
        });

        checkNotificationPermission();

        recyclerView.setAdapter(adapter);
        btnEnableAccess.setOnClickListener(v -> showPermissionDialog());
        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkNotificationPermission();
        loadNotifications();
    }

    private void loadNotifications() {
        List<Transaction> notifications = repository.getAllTransactionNotFiltered();
        adapter.updateData(notifications);

        if (notifications.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
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
}