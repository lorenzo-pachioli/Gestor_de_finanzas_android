package com.notificationcapture.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.SaldoCuenta;
import com.notificationcapture.app.utils.MoneyTextWatcher;
import java.util.List;
import java.math.BigDecimal;


public class SaldoCuentaAdapter extends RecyclerView.Adapter<SaldoCuentaAdapter.ViewHolder> {

    public interface OnPagarClickListener {
        void onPagarClick(SaldoCuenta saldoCuenta);
    }

    private List<SaldoCuenta> saldos;
    private OnPagarClickListener listener;

    public SaldoCuentaAdapter(List<SaldoCuenta> saldos, OnPagarClickListener listener) {
        this.saldos = saldos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saldo_cuenta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SaldoCuenta saldo = saldos.get(position);
        holder.tvNombreCuenta.setText(saldo.getNombreCuenta());
        holder.tvTipoCuenta.setText(saldo.getTipoCuenta());
        
        holder.tvSaldo.setText("$" + MoneyTextWatcher.format(saldo.getSaldo().abs()));

        if (saldo.getSaldo().compareTo(BigDecimal.ZERO) >= 0) {
            holder.tvSaldo.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
        } else {
            holder.tvSaldo.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red));
            holder.tvSaldo.setText("-$" + MoneyTextWatcher.format(saldo.getSaldo().abs()));
        }

        if ("CREDITO".equalsIgnoreCase(saldo.getTipoCuenta()) && saldo.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
            holder.btnPagar.setVisibility(View.VISIBLE);
            holder.btnPagar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPagarClick(saldo);
                }
            });
        } else {
            holder.btnPagar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return saldos != null ? saldos.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreCuenta, tvTipoCuenta, tvSaldo;
        android.widget.Button btnPagar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreCuenta = itemView.findViewById(R.id.tvNombreCuenta);
            tvTipoCuenta = itemView.findViewById(R.id.tvTipoCuenta);
            tvSaldo = itemView.findViewById(R.id.tvSaldo);
            btnPagar = itemView.findViewById(R.id.btnPagar);
        }
    }
}
