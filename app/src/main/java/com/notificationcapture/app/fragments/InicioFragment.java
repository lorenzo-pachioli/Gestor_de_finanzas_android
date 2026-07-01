package com.notificationcapture.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.TransactionAdapter;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.SaldoCuenta;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.services.ServiceProvider;
import com.notificationcapture.app.services.AppExecutors;
import com.notificationcapture.app.services.TransactionService;
import com.notificationcapture.app.utils.MoneyTextWatcher;
import com.notificationcapture.app.viewmodels.MainViewModel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InicioFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TextView emptyView;

    private TextView tvIngresos;
    private TextView tvEgresos;
    private TextView tvBalance;
    private TextView tvMonthTitle;
    private MainViewModel viewModel;
    private TransactionService transactionService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        transactionService = ServiceProvider.getInstance().getTransactionService();

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        tvIngresos = view.findViewById(R.id.tvIngresos);
        tvEgresos = view.findViewById(R.id.tvEgresos);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);

        View cardTotalDisponible = view.findViewById(R.id.cardTotalAvailable);
        TextView tvTotalDisponible = view.findViewById(R.id.tvTotalAvailable);
        TextView tvViewDetails = view.findViewById(R.id.tvViewDetails);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        View.OnClickListener patrimonioClickListener = v -> {
            MyBottomSheetDialogFragment sheet = new MyBottomSheetDialogFragment();
            Bundle args = new Bundle();
            args.putString("modo", "patrimonio_neto");
            sheet.setArguments(args);
            sheet.show(getParentFragmentManager(), "patrimonio_neto");
        };

        if (tvViewDetails != null) tvViewDetails.setOnClickListener(patrimonioClickListener);
        if (cardTotalDisponible != null) cardTotalDisponible.setOnClickListener(patrimonioClickListener);

        adapter = new TransactionAdapter(new ArrayList<>(), getChildFragmentManager(), item -> {
            viewModel.deleteTransaction(item.getId());
        }, null, false);
        recyclerView.setAdapter(adapter);

        tvMonthTitle.setText(getString(R.string.resumen_de, getMonthName()));

        viewModel.getAllApprovedTransactions().observe(getViewLifecycleOwner(), allTransactions -> {
            AppExecutors executors = AppExecutors.getInstance();
            CompletableFuture
                    .supplyAsync(() -> transactionService.filterCurrentMonth(allTransactions),
                            executors.computation())
                    .thenAccept(currentMonth -> executors.mainThread().execute(() -> {
                        adapter.updateData(currentMonth);
                        emptyView.setVisibility(currentMonth.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(currentMonth.isEmpty() ? View.GONE : View.VISIBLE);
                        updateFinancialSummary(currentMonth);
                    }));
        });

        viewModel.getSaldosPorCuenta().observe(getViewLifecycleOwner(), saldos -> {
            if (saldos == null || tvTotalDisponible == null) return;

            BigDecimal totalLiquid = BigDecimal.ZERO;
            BigDecimal totalDeuda = BigDecimal.ZERO;
            for (SaldoCuenta sc : saldos) {
                if ("CREDITO".equals(sc.getTipoCuenta())) {
                    totalDeuda = totalDeuda.add(sc.getSaldo());
                } else {
                    totalLiquid = totalLiquid.add(sc.getSaldo());
                }
            }

            BigDecimal available = totalLiquid.subtract(totalDeuda);
            tvTotalDisponible.setText(formatAmount(available));
            tvTotalDisponible.setTextColor(
                    getResources().getColor(available.compareTo(BigDecimal.ZERO) >= 0 ? R.color.green : R.color.red));
        });
    }

    private String getMonthName() {
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int[] monthResIds = {
                R.string.enero, R.string.febrero, R.string.marzo,
                R.string.abril, R.string.mayo, R.string.junio,
                R.string.julio, R.string.agosto, R.string.septiembre,
                R.string.octubre, R.string.noviembre, R.string.diciembre
        };
        return getString(monthResIds[currentMonth]);
    }

    private void updateFinancialSummary(List<Transaction> transactions) {
        BigDecimal totalIngresos = transactionService.sumByType(transactions, IngresoOEgreso.INGRESO);
        BigDecimal totalEgresos = transactionService.sumByType(transactions, IngresoOEgreso.EGRESO);
        BigDecimal balance = totalIngresos.subtract(totalEgresos);
        tvIngresos.setText(formatAmount(totalIngresos));
        tvEgresos.setText(formatAmount(totalEgresos));
        tvBalance.setText(formatAmount(balance.abs()));
        tvBalance.setTextColor(getResources().getColor(balance.compareTo(BigDecimal.ZERO) >= 0 ? R.color.green : R.color.red));
    }

    private String formatAmount(BigDecimal amount) {
        return "$" + MoneyTextWatcher.format(amount);
    }
}
