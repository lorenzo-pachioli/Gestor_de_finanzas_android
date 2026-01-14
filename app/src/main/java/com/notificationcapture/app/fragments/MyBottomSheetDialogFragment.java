package com.notificationcapture.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.notificationcapture.app.adapters.TransactionAdapter;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.interfaces.OnDismissListener;

public class MyBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private TextView tvTitle;
    private RecyclerView recyclerDetails;
    private TransactionAdapter adapter;
    private boolean dataChanged = false;

    private OnDismissListener dismissListener;

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_NOTIFICATIONS = "arg_notifications";

    public MyBottomSheetDialogFragment() {
    }

    public static MyBottomSheetDialogFragment newInstance(String title, List<Transaction> notifications) {
        MyBottomSheetDialogFragment fragment = new MyBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_NOTIFICATIONS, (Serializable) notifications);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior
                        .from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTitle = view.findViewById(R.id.tvTitle);
        recyclerDetails = view.findViewById(R.id.recyclerDetails);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            List<Transaction> notifications = (List<Transaction>) getArguments()
                    .getSerializable(ARG_NOTIFICATIONS);

            if (title != null)
                tvTitle.setText(title);
            if (notifications != null) {
                android.widget.Toast.makeText(getContext(), "Mostrando " + notifications.size() + " items",
                        android.widget.Toast.LENGTH_SHORT).show();

                adapter = new TransactionAdapter(notifications, getChildFragmentManager(), item -> {
                    TransactionRepository repository = RepositoryProvider.getInstance().getTransactionRepository();
                    repository.deleteTransaction(item.getId());
                    notifications.remove(item);
                    adapter.notifyDataSetChanged();
                    dataChanged = true;

                    if (notifications.isEmpty()) {
                        dismiss();
                    } else {
                        // Optional: Update title if it contains total
                        tvTitle.setText(tvTitle.getText().toString().replaceFirst("\\d+ items?",
                                notifications.size() + " items"));
                    }
                }, null, false); // showAddButton = false para transacciones aprobadas
                recyclerDetails.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerDetails.setAdapter(adapter);
                recyclerDetails.setVisibility(View.VISIBLE);
            } else {
                android.widget.Toast.makeText(getContext(), "No se recibieron datos", android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            android.widget.Toast.makeText(getContext(), "Faltan argumentos", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDismissed(dataChanged);
        }
    }

    public void setView(View dialogView) {

    }
}