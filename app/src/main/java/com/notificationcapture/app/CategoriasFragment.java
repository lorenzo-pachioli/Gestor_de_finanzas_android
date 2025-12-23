package com.notificationcapture.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.notificationcapture.app.models.Category;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoriasFragment extends Fragment {

    private RecyclerView recyclerCategories;
    // private RecyclerView recyclerDetails;
    private Spinner spinnerPeriod;
    private TextView tvDetailsHeader;
    private TextView tvSelectedCategory;

    private CategorySummaryAdapter summaryAdapter;
    private NotificationAdapter detailsAdapter;
    private NotificationRepository repository;

    // Period handling
    private List<Calendar> periodList;
    private List<String> periodNames;

    public CategoriasFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new NotificationRepository(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_categorias, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupAdapters();
        setupPeriodSpinner();
    }

    private void initViews(View view) {
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        // recyclerDetails = view.findViewById(R.id.recyclerDetails);
        spinnerPeriod = view.findViewById(R.id.spinnerPeriod);
        // tvDetailsHeader = view.findViewById(R.id.tvDetailsHeader);
        // tvSelectedCategory = view.findViewById(R.id.tvSelectedCategory);

        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        // recyclerDetails.setLayoutManager(new LinearLayoutManager(getContext()));

        // Hide details initially (Legacy views no longer initialized)
        // tvDetailsHeader.setVisibility(View.GONE);
        // tvSelectedCategory.setVisibility(View.GONE);
        // recyclerDetails.setVisibility(View.GONE);
    }

    private void setupAdapters() {
        // Fetch categories to get colors
        List<Category> categories = repository.getCategories(NotificationItem.TransactionType.EGRESO);
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : categories) {
            colorMap.put(c.getName(), c.getColor());
        }

        summaryAdapter = new CategorySummaryAdapter(new HashMap<>(), colorMap, this::onCategoryClick);
        recyclerCategories.setAdapter(summaryAdapter);

        // Ensure NotificationAdapter handles deletions if necessary, though mainly for
        // viewing here
        detailsAdapter = new NotificationAdapter(new ArrayList<>(), colorMap, item -> {
            // Optional: Implement deletion from details view if needed
            repository.deleteNotification(item.getId());
            refreshData();
        });
        // recyclerDetails.setAdapter(detailsAdapter);
    }

    private void setupPeriodSpinner() {
        periodList = new ArrayList<>();
        periodNames = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));

        // Generate last 12 months
        for (int i = 0; i < 12; i++) {
            periodList.add((Calendar) calendar.clone());
            String periodName = sdf.format(calendar.getTime());
            // Capitalize first letter
            periodName = periodName.substring(0, 1).toUpperCase() + periodName.substring(1);
            periodNames.add(periodName);
            calendar.add(Calendar.MONTH, -1);
        }

        UniversalSpinnerAdapter<String> adapter = new UniversalSpinnerAdapter<>(requireContext(), periodNames);
        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadDataForPeriod(periodList.get(position));
                // Hide details when changing period
                hideDetails();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadDataForPeriod(Calendar period) {
        if (repository == null)
            repository = new NotificationRepository(requireContext());
        List<NotificationItem> allNotifications = repository.getAllNotifications();

        int month = period.get(Calendar.MONTH);
        int year = period.get(Calendar.YEAR);

        Map<String, Double> categoryTotals = new HashMap<>();

        for (NotificationItem item : allNotifications) {
            // Filter by date
            Calendar itemCal = Calendar.getInstance();
            itemCal.setTimeInMillis(item.getTimestamp());

            if (itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year) {
                // Filter by type (Only EGRESO as "gastos")
                if (item.getType() == NotificationItem.TransactionType.EGRESO && item.hasAmount()) {
                    String category = item.getCategory();
                    if (category == null || category.isEmpty()) {
                        category = "Sin Categoría";
                    }

                    Double amount = item.getAmount();
                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                }
            }
        }

        // Update colors as well
        List<Category> categories = repository.getCategories(NotificationItem.TransactionType.EGRESO);
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : categories) {
            colorMap.put(c.getName(), c.getColor());
        }

        summaryAdapter.updateData(categoryTotals, colorMap);
    }

    private void onCategoryClick(String category, Double totalAmount) {
        // Filter transactions for this category and current period
        int selectedPosition = spinnerPeriod.getSelectedItemPosition();
        if (selectedPosition == -1)
            return;

        Calendar period = periodList.get(selectedPosition);
        int month = period.get(Calendar.MONTH);
        int year = period.get(Calendar.YEAR);

        List<NotificationItem> allNotifications = repository.getAllNotifications();
        List<NotificationItem> filteredList = new ArrayList<>();

        for (NotificationItem item : allNotifications) {
            Calendar itemCal = Calendar.getInstance();
            itemCal.setTimeInMillis(item.getTimestamp());

            if (itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year) {
                if (item.getType() == NotificationItem.TransactionType.EGRESO) {
                    String itemCategory = item.getCategory();
                    if (itemCategory == null || itemCategory.isEmpty())
                        itemCategory = "Sin Categoría";

                    if (itemCategory.equals(category)) {
                        filteredList.add(item);
                    }
                }
            }
        }

        // Fetch categories to get colors
        List<Category> categories = repository.getCategories(NotificationItem.TransactionType.EGRESO);
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : categories) {
            colorMap.put(c.getName(), c.getColor());
        }

        String formattedAmount = String.format("$%.2f", totalAmount)
                .replace(",", "#")
                .replace(".", ",")
                .replace("#", ".");
        String title = category + " - Total: " + formattedAmount;

        android.widget.Toast.makeText(getContext(), "Abriendo detalle: " + category, android.widget.Toast.LENGTH_SHORT)
                .show();
        MyBottomSheetDialogFragment bottomSheet = MyBottomSheetDialogFragment.newInstance(title, filteredList,
                colorMap);
        bottomSheet.show(getParentFragmentManager(), "CategoryDetails");
    }

    private void hideDetails() {
        if (tvDetailsHeader != null)
            tvDetailsHeader.setVisibility(View.GONE);
        if (tvSelectedCategory != null)
            tvSelectedCategory.setVisibility(View.GONE);
    }

    private void refreshData() {
        int selectedPosition = spinnerPeriod.getSelectedItemPosition();
        if (selectedPosition != -1) {
            loadDataForPeriod(periodList.get(selectedPosition));
            hideDetails();
        }
    }

    private void showTestDialog() {
        MyBottomSheetDialogFragment bottomSheet = MyBottomSheetDialogFragment.newInstance("Test", new ArrayList<>(),
                new HashMap<>());
        bottomSheet.show(getParentFragmentManager(), "EtiquetaUnica");
    }
}