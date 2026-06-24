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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.notificationcapture.app.adapters.TransactionAdapter;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.SaldoCuenta;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.viewmodels.MainViewModel;
import com.notificationcapture.app.R;
import com.notificationcapture.app.enums.IngresoOEgreso;

public class InicioFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TextView emptyView;

    private TextView tvIngresos;
    private TextView tvEgresos;
    private TextView tvBalance;
    private TextView tvMonthTitle;
    private TextView tvDeudaCredito;
    private MainViewModel viewModel;

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

        // Observer 1: transacciones del mes actual
        viewModel.getAllApprovedTransactions().observe(getViewLifecycleOwner(), allTransactions -> {
            List<Transaction> currentMonth = getCurrentMonthTransactions(allTransactions);
            adapter.updateData(currentMonth);

            emptyView.setVisibility(currentMonth.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(currentMonth.isEmpty() ? View.GONE : View.VISIBLE);

            updateFinancialSummary(currentMonth);
        });

        // Observer 2: saldos por cuenta — LiveData separado, siempre en hilo principal.
        // Reemplaza el callback anidado con runOnUiThread que existía antes.
        viewModel.getSaldosPorCuenta().observe(getViewLifecycleOwner(), saldos -> {
            if (saldos == null || tvTotalDisponible == null) return;

            double totalLiquid = 0;
            double totalDeuda = 0;
            for (SaldoCuenta sc : saldos) {
                if ("CREDITO".equals(sc.getTipoCuenta())) {
                    totalDeuda += sc.getSaldo();
                } else {
                    totalLiquid += sc.getSaldo();
                }
            }

            double available = totalLiquid - totalDeuda;
            tvTotalDisponible.setText(formatAmount(available));
            tvTotalDisponible.setTextColor(
                    getResources().getColor(available >= 0 ? R.color.green : R.color.red));
        });
    }

    private List<Transaction> getCurrentMonthTransactions(List<Transaction> all) {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        List<Transaction> result = new ArrayList<>();
        for (Transaction item : all) {
            Calendar itemDate = Calendar.getInstance();
            itemDate.setTimeInMillis(item.getTimestamp());
            if (itemDate.get(Calendar.MONTH) == currentMonth
                    && itemDate.get(Calendar.YEAR) == currentYear) {
                result.add(item);
            }
        }
        return result;
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
        double totalIngresos = 0;
        double totalEgresos = 0;

        for (Transaction item : transactions) {
            if (item.hasAmount() && item.getPaymentMethod() != PaymentMethod.CREDITO) {
                if (item.getType() == IngresoOEgreso.INGRESO) {
                    totalIngresos += item.getAmount();
                } else {
                    totalEgresos += item.getAmount();
                }
            }
        }

        double balance = totalIngresos - totalEgresos;
        tvIngresos.setText(formatAmount(totalIngresos));
        tvEgresos.setText(formatAmount(totalEgresos));
        tvBalance.setText(formatAmount(Math.abs(balance)));
        tvBalance.setTextColor(getResources().getColor(balance >= 0 ? R.color.green : R.color.red));
    }

    private String formatAmount(double amount) {
        return "$" + com.notificationcapture.app.utils.MoneyTextWatcher.format(amount);
    }
}