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

import com.notificationcapture.app.adapters.NotificationAdapter;
import com.notificationcapture.app.NotificationItem;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.interfaces.OnDismissListener;

public class MyBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private TextView tvTitle;
    private RecyclerView recyclerDetails;
    private NotificationAdapter adapter;
    private boolean dataChanged = false;

    private OnDismissListener dismissListener;

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_NOTIFICATIONS = "arg_notifications";
    private static final String ARG_COLOR_MAP = "arg_color_map";

    public MyBottomSheetDialogFragment() {
    }

    public static MyBottomSheetDialogFragment newInstance(String title, List<NotificationItem> notifications,
            Map<String, Integer> colorMap) {
        MyBottomSheetDialogFragment fragment = new MyBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_NOTIFICATIONS, (Serializable) notifications);
        args.putSerializable(ARG_COLOR_MAP, (Serializable) colorMap);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Infla el layout que creaste
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTitle = view.findViewById(R.id.tvTitle);
        recyclerDetails = view.findViewById(R.id.recyclerDetails);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            List<NotificationItem> notifications = (List<NotificationItem>) getArguments()
                    .getSerializable(ARG_NOTIFICATIONS);
            Map<String, Integer> colorMap = (Map<String, Integer>) getArguments().getSerializable(ARG_COLOR_MAP);

            if (title != null)
                tvTitle.setText(title);
            if (notifications != null) {
                android.widget.Toast.makeText(getContext(), "Mostrando " + notifications.size() + " items",
                        android.widget.Toast.LENGTH_SHORT).show();
                if (colorMap == null)
                    colorMap = new HashMap<>();

                adapter = new NotificationAdapter(notifications, colorMap, item -> {
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
                });
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