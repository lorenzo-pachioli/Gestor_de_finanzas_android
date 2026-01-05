package com.notificationcapture.app.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.notificationcapture.app.adapters.NotificationAdapter;
import com.notificationcapture.app.NotificationItem;
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
    private NotificationAdapter adapter;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Prepare colors
        List<Category> allCategories = categoryRepository.getAllCategories();
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : allCategories) {
            colorMap.put(c.getName(), c.getDisplayColor());
        }

        adapter = new NotificationAdapter(new ArrayList<>(), colorMap, (item) -> {
            // Callback para eliminar notificación
            repository.deleteTransactionNotFiltered(item.getId());
            loadNotifications();
        });

        recyclerView.setAdapter(adapter);
        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        List<NotificationItem> notifications = repository.getAllTransactionNotFiltered();
        adapter.updateData(notifications);

        if (notifications.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}