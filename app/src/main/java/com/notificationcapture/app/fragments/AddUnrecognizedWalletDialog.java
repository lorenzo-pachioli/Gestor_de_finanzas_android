package com.notificationcapture.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.notificationcapture.app.R;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.WalletRepository;

public class AddUnrecognizedWalletDialog extends BottomSheetDialogFragment {

    public interface Listener {
        void onAddAndApprove(String walletId);
        void onApproveOnly();
        void onIgnore();
    }

    private static final String ARG_PACKAGE = "arg_package";
    private static final String ARG_TRANSACTION_ID = "arg_transaction_id";

    private Listener listener;

    public static AddUnrecognizedWalletDialog newInstance(String packageName, String transactionId) {
        AddUnrecognizedWalletDialog dialog = new AddUnrecognizedWalletDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE, packageName);
        args.putString(ARG_TRANSACTION_ID, transactionId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_unrecognized_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String packageName = getArguments() != null ? getArguments().getString(ARG_PACKAGE, "") : "";

        TextView tvPackage = view.findViewById(R.id.tvPackageName);
        EditText etName = view.findViewById(R.id.etWalletName);
        Button btnAdd = view.findViewById(R.id.btnAddAndApprove);
        Button btnOnly = view.findViewById(R.id.btnApproveOnly);
        Button btnIgnore = view.findViewById(R.id.btnIgnore);

        tvPackage.setText(packageName);
        etName.setText(WalletRepository.suggestNameFromPackage(packageName));

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Requerido");
                return;
            }

            Wallets newWallet = new Wallets(name, packageName);
            RepositoryProvider.getInstance().getWalletRepository().addWallet(newWallet);
            if (listener != null) {
                listener.onAddAndApprove(newWallet.getId());
            }
            dismiss();
        });

        btnOnly.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApproveOnly();
            }
            dismiss();
        });

        btnIgnore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIgnore();
            }
            dismiss();
        });
    }
}
