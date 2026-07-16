package com.notificationcapture.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.CategorySummaryAdapter;
import com.notificationcapture.app.adapters.TransactionAdapter;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.services.CategoryService;
import com.notificationcapture.app.services.ServiceProvider;
import com.notificationcapture.app.services.TransactionService;
import com.notificationcapture.app.utils.MoneyTextWatcher;
import com.notificationcapture.app.viewmodels.CategoriasViewModel;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CategoriasFragment extends Fragment {

    private RecyclerView recyclerCategories;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;
    private TabLayout tabLayout;

    private IngresoOEgreso currentType = IngresoOEgreso.EGRESO;

    private CategorySummaryAdapter summaryAdapter;
    private TransactionRepository repository;
    private CategoriasViewModel viewModel;
    private List<Transaction> currentTransactions = new ArrayList<>();
    private CategoryService categoryService;
    private TransactionService transactionService;

    public CategoriasFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        categoryService = ServiceProvider.getInstance().getCategoryService();
        transactionService = ServiceProvider.getInstance().getTransactionService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categorias, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupAdapters();
        setupPeriodSpinner();

        viewModel = new ViewModelProvider(this).get(CategoriasViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                currentTransactions = transactions;
                updateYearsSpinnerIfNeeded();
                refreshData();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initViews(View view) {
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        tabLayout = view.findViewById(R.id.tabLayout);

        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        // Ajustar paddingBottom de forma dinámica según la barra de navegación real
        // del dispositivo. Reemplaza el valor hardcodeado del XML que funcionaba en
        // el emulador pero fallaba en dispositivos físicos con gestos o nav bar distintos.
        int bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.size_60dp);
        ViewCompat.setOnApplyWindowInsetsListener(recyclerCategories, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    bottomNavHeight + systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(recyclerCategories);
        setupTabLayout();
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition() == 0 ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
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
        List<Category> categories = categoryService.getCategoriesForType(currentType);
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : categories) {
            colorMap.put(c.getName(), c.getDisplayColor());
        }

        summaryAdapter = new CategorySummaryAdapter(new HashMap<>(), colorMap, this::onCategoryClick);
        recyclerCategories.setAdapter(summaryAdapter);

        new TransactionAdapter(new ArrayList<>(), getChildFragmentManager(), item -> {
            repository.deleteTransaction(item.getId());
            refreshData();
        }, null, false);
    }

    private void setupPeriodSpinner() {
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

        updateYearsSpinnerIfNeeded();

        AdapterView.OnItemSelectedListener periodChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerMonth.setOnItemSelectedListener(periodChangeListener);
        spinnerYear.setOnItemSelectedListener(periodChangeListener);
    }

    private void updateYearsSpinnerIfNeeded() {
        if (spinnerYear == null || getActivity() == null) return;
        List<String> years = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (Transaction item : currentTransactions) {
            cal.setTimeInMillis(item.getTimestamp());
            String year = String.valueOf(cal.get(Calendar.YEAR));
            if (!years.contains(year)) {
                years.add(year);
            }
        }

        if (years.isEmpty()) {
            years.add(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }
        Collections.sort(years, Collections.reverseOrder());

        String previousSelectedYear = (String) spinnerYear.getSelectedItem();
        UniversalSpinnerAdapter<String> yearAdapter = new UniversalSpinnerAdapter<>(requireContext(), years);
        spinnerYear.setAdapter(yearAdapter);

        String targetYear = previousSelectedYear != null ? previousSelectedYear
                : String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        int yearIndex = years.indexOf(targetYear);
        spinnerYear.setSelection(yearIndex != -1 ? yearIndex : 0);
    }

    private void loadDataForPeriod(int month, int year) {
        Map<String, BigDecimal> groupedById = transactionService.groupByCategoryId(currentTransactions, currentType);
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : groupedById.entrySet()) {
            Category cat = categoryService.resolveCategory(entry.getKey(), currentType);
            categoryTotals.put(cat.getDisplayName(), entry.getValue());
        }

        List<Category> categories = categoryService.getCategoriesForType(currentType);
        Map<String, Integer> colorMap = new HashMap<>();
        for (Category c : categories) {
            colorMap.put(c.getName(), c.getDisplayColor());
        }

        summaryAdapter.updateData(categoryTotals, colorMap);
    }

    private void onCategoryClick(String category, BigDecimal totalAmount) {
        int month = spinnerMonth.getSelectedItemPosition();
        String selectedYearStr = (String) spinnerYear.getSelectedItem();
        if (selectedYearStr == null) return;

        int year = Integer.parseInt(selectedYearStr);
        List<Transaction> filteredList = new ArrayList<>();

        for (Transaction item : currentTransactions) {
            Calendar itemCal = Calendar.getInstance();
            itemCal.setTimeInMillis(item.getTimestamp());
            if (itemCal.get(Calendar.MONTH) == month
                    && itemCal.get(Calendar.YEAR) == year
                    && item.getType() == currentType) {
                Category cat = categoryService.resolveCategory(item.getCategoryId(), currentType);
                if (cat.getDisplayName().equals(category)) {
                    filteredList.add(item);
                }
            }
        }

        String formattedAmount = "$" + MoneyTextWatcher.format(totalAmount);
        String title = category + " - Total: " + formattedAmount;

        android.widget.Toast.makeText(getContext(), "Abriendo detalle: " + category, android.widget.Toast.LENGTH_SHORT)
                .show();
        MyBottomSheetDialogFragment bottomSheet = MyBottomSheetDialogFragment.newInstance(title, filteredList);
        bottomSheet.setOnDismissListener(changed -> {
            if (changed) {
                refreshData();
            }
        });
        bottomSheet.show(getParentFragmentManager(), "CategoryDetails");
    }

    private void refreshData() {
        int selectedMonth = spinnerMonth.getSelectedItemPosition();
        String selectedYearStr = (String) spinnerYear.getSelectedItem();
        if (selectedYearStr != null) {
            loadDataForPeriod(selectedMonth, Integer.parseInt(selectedYearStr));
        }
    }
}
