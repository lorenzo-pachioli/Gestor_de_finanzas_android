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

        // Initialize ViewModel scoped to Activity so all fragments share data
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        tvIngresos = view.findViewById(R.id.tvIngresos);
        tvEgresos = view.findViewById(R.id.tvEgresos);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        
        View cardTotalDisponible = view.findViewById(R.id.cardTotalDisponible);
        TextView tvTotalDisponible = view.findViewById(R.id.tvTotalDisponible);
        tvDeudaCredito = view.findViewById(R.id.tvDeudaCredito);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Patrimonio Neto Trigger mensual (si corresponde) o global
        View.OnClickListener patrimonioClickListener = v -> {
            MyBottomSheetDialogFragment sheet = new MyBottomSheetDialogFragment();
            Bundle args = new Bundle();
            args.putString("modo", "patrimonio_neto");
            sheet.setArguments(args);
            sheet.show(getParentFragmentManager(), "patrimonio_neto");
        };

        tvBalance.setOnClickListener(patrimonioClickListener);
        cardTotalDisponible.setOnClickListener(patrimonioClickListener);

        adapter = new TransactionAdapter(new ArrayList<>(), getChildFragmentManager(), item -> {
            viewModel.deleteTransaction(item.getId());
        }, null, false); // showAddButton = false para transacciones aprobadas
        recyclerView.setAdapter(adapter);

        // Actualizar el título del mes
        String monthName = getMonthName();
        tvMonthTitle.setText(getString(R.string.resumen_de, monthName));

        // Setup observer for unidirectional data flow
        viewModel.getAllApprovedTransactions().observe(getViewLifecycleOwner(), allNotifications -> {
            List<Transaction> currentMonthNotifications = getCurrentMonthNotifications(allNotifications);
            adapter.updateData(currentMonthNotifications);

            if (currentMonthNotifications.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            // 1. Actualizar el resumen financiero del mes (Ingresos/Egresos/Balance)
            updateFinancialSummary(currentMonthNotifications);

            // 2. Actualizar disponibilidad y deuda global usando los saldos de cada cuenta
            viewModel.getSaldosPorCuenta(result -> {
                if (result.isSuccess() && result.getData() != null) {
                    double totalLiquid = 0;
                    double totalDeuda = 0;
                    for (com.notificationcapture.app.models.SaldoCuenta sc : result.getData()) {
                        if ("CREDITO".equals(sc.getTipoCuenta())) {
                            // En el repositorio, el saldo de crédito se devuelve como positivo si hay deuda
                            totalDeuda += sc.getSaldo();
                        } else {
                            totalLiquid += sc.getSaldo();
                        }
                    }
                    
                    final double finalLiquid = totalLiquid;
                    final double finalDeuda = totalDeuda;
                    
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvTotalDisponible.setText(formatAmount(finalLiquid));
                            tvTotalDisponible.setTextColor(getResources().getColor(finalLiquid >= 0 ? R.color.green : R.color.red));
                            
                            tvDeudaCredito.setText(formatAmount(finalDeuda));
                            if (finalDeuda > 0) {
                                tvDeudaCredito.setTextColor(getResources().getColor(R.color.red));
                            } else {
                                tvDeudaCredito.setTextColor(getResources().getColor(R.color.text_secondary));
                            }
                        });
                    }
                }
            });
        });
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
                // CASH FLOW: Ignorar gastos con tarjeta en el resumen mensual de egresos
                // Solo cuentan si son ingresos o egresos de billetera (Efectivo/Débito)
                if (item.getPaymentMethod() != PaymentMethod.CREDITO) {
                    if (item.getType() == IngresoOEgreso.INGRESO) {
                        totalIngresos += item.getAmount();
                    } else {
                        totalEgresos += item.getAmount();
                    }
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