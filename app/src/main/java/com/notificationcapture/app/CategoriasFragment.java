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
import com.google.android.material.tabs.TabLayout;

import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.enums.TransactionType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoriasFragment extends Fragment {

    private RecyclerView recyclerCategories;
    // private RecyclerView recyclerDetails;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;
    private TabLayout tabLayout;

    private TransactionType currentType = TransactionType.EGRESO;

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
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        tabLayout = view.findViewById(R.id.tabLayout);

        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        setupTabLayout();
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentType = TransactionType.EGRESO;
                } else {
                    currentType = TransactionType.INGRESO;
                }
                refreshData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupAdapters() {
        // Fetch categories to get colors
        List<Category> categories = repository.getCategories(currentType);
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
        // --- Setup Month Spinner ---
        List<String> monthNames = new ArrayList<>();
        SimpleDateFormat monthSdf = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
        Calendar tempCal = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            tempCal.set(Calendar.MONTH, i);
            String monthName = monthSdf.format(tempCal.getTime());
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
            monthNames.add(monthName);
        }

        UniversalSpinnerAdapter<String> monthAdapter = new UniversalSpinnerAdapter<>(requireContext(), monthNames);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        // --- Setup Year Spinner ---
        List<String> years = new ArrayList<>();
        List<NotificationItem> allNotifications = repository.getAllNotifications();
        Calendar cal = Calendar.getInstance();

        for (NotificationItem item : allNotifications) {
            cal.setTimeInMillis(item.getTimestamp());
            String year = String.valueOf(cal.get(Calendar.YEAR));
            if (!years.contains(year)) {
                years.add(year);
            }
        }

        if (years.isEmpty()) {
            years.add("2025");
        }
        Collections.sort(years, Collections.reverseOrder());

        UniversalSpinnerAdapter<String> yearAdapter = new UniversalSpinnerAdapter<>(requireContext(), years);
        spinnerYear.setAdapter(yearAdapter);

        // Select current year if present
        String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        int yearIndex = years.indexOf(currentYear);
        if (yearIndex != -1) {
            spinnerYear.setSelection(yearIndex);
        }

        // --- Listeners ---
        AdapterView.OnItemSelectedListener periodChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedMonth = spinnerMonth.getSelectedItemPosition();
                String selectedYearStr = (String) spinnerYear.getSelectedItem();
                if (selectedYearStr != null) {
                    loadDataForPeriod(selectedMonth, Integer.parseInt(selectedYearStr));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerMonth.setOnItemSelectedListener(periodChangeListener);
        spinnerYear.setOnItemSelectedListener(periodChangeListener);
    }

    private void loadDataForPeriod(int month, int year) {
        if (repository == null)
            repository = new NotificationRepository(requireContext());
        List<NotificationItem> allNotifications = repository.getAllNotifications();

        Map<String, Double> categoryTotals = new HashMap<>();

        for (NotificationItem item : allNotifications) {
            // Filter by date
            Calendar itemCal = Calendar.getInstance();
            itemCal.setTimeInMillis(item.getTimestamp());

            if (itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year) {
                // Filter by type
                if (item.getType() == currentType && item.hasAmount()) {
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
        List<Category> categories = repository.getCategories(currentType);
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : categories) {
            colorMap.put(c.getName(), c.getColor());
        }

        summaryAdapter.updateData(categoryTotals, colorMap);
    }

    private void onCategoryClick(String category, Double totalAmount) {
        // Filter transactions for this category and current period
        int month = spinnerMonth.getSelectedItemPosition();
        String selectedYearStr = (String) spinnerYear.getSelectedItem();
        if (selectedYearStr == null)
            return;

        int year = Integer.parseInt(selectedYearStr);

        List<NotificationItem> allNotifications = repository.getAllNotifications();
        List<NotificationItem> filteredList = new ArrayList<>();

        for (NotificationItem item : allNotifications) {
            Calendar itemCal = Calendar.getInstance();
            itemCal.setTimeInMillis(item.getTimestamp());

            if (itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year) {
                if (item.getType() == currentType) {
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
        List<Category> categories = repository.getCategories(currentType);
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
        bottomSheet.setOnDismissListener(changed -> {
            if (changed) {
                refreshData();
            }
        });
        bottomSheet.show(getParentFragmentManager(), "CategoryDetails");
    }

    private void hideDetails() {
        // Legacy views tvDetailsHeader and tvSelectedCategory were removed from layout
    }

    private void refreshData() {
        int selectedMonth = spinnerMonth.getSelectedItemPosition();
        String selectedYearStr = (String) spinnerYear.getSelectedItem();
        if (selectedYearStr != null) {
            loadDataForPeriod(selectedMonth, Integer.parseInt(selectedYearStr));
        }
    }

    private void showTestDialog() {
        MyBottomSheetDialogFragment bottomSheet = MyBottomSheetDialogFragment.newInstance("Test", new ArrayList<>(),
                new HashMap<>());

        bottomSheet.show(getParentFragmentManager(), "EtiquetaUnica");
    }
}