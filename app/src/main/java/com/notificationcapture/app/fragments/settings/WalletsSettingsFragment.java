package com.notificationcapture.app.fragments.settings;

// ARCHIVO NUEVO: app/src/main/java/com/notificationcapture/app/fragments/settings/WalletsSettingsFragment.java

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.fragments.SelectorBottomSheet;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.WalletRepository;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;

public class WalletsSettingsFragment extends Fragment {

    private Runnable onBackRequested;
    private WalletRepository walletRepository;
    private Spinner spinnerWallets;
    private List<Wallets> currentWallets = new ArrayList<>();

    public void setOnBackRequested(Runnable r) { this.onBackRequested = r; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_wallets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (onBackRequested != null) onBackRequested.run();
        });

        spinnerWallets = view.findViewById(R.id.spinnerWallets);
        Button btnAdd = view.findViewById(R.id.btnAddWallet);
        btnAdd.setOnClickListener(v -> showGlobalWalletSelectionDialog());

        loadWallets();

        spinnerWallets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                if (position > 0) {
                    showEditWalletDialog(currentWallets.get(position - 1));
                    spinnerWallets.setSelection(0);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadWallets() {
        currentWallets = walletRepository.getAllWallets();
        List<Wallets> displayList = new ArrayList<>();
        displayList.add(new Wallets("Seleccionar para editar/borrar...", ""));
        displayList.addAll(currentWallets);
        UniversalSpinnerAdapter<Wallets> adapter = new UniversalSpinnerAdapter<>(requireContext(), displayList);
        spinnerWallets.setAdapter(adapter);
    }

    private void showGlobalWalletSelectionDialog() {
        List<com.notificationcapture.app.models.GlobalWallet> globalWallets =
                com.notificationcapture.app.utils.ConfigManager.getInstance().getGlobalWallets();
        List<Wallets> userWallets = walletRepository.getAllWallets();

        List<String> userPackages = new ArrayList<>();
        for (Wallets w : userWallets) userPackages.add(w.getPackageName());

        SelectorBottomSheet bottomSheet = SelectorBottomSheet.newInstance(
                "Seleccioná tu billetera o banco", globalWallets, "", userPackages);

        bottomSheet.setOnOptionSelectedListener(optionName -> {
            for (com.notificationcapture.app.models.GlobalWallet gw : globalWallets) {
                if (gw.getName().equals(optionName)) {
                    boolean exists = false;
                    for (Wallets uw : userWallets) {
                        if (uw.getPackageName().equals(gw.getPrimaryPackageName())) { exists = true; break; }
                    }
                    if (!exists) {
                        walletRepository.addWallet(new Wallets(gw.getId(), gw.getName(), gw.getPrimaryPackageName()));
                        loadWallets();
                        Toast.makeText(requireContext(), "'" + optionName + "' agregada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "'" + optionName + "' ya está agregada", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        });
        bottomSheet.show(getChildFragmentManager(), "WalletSelection");
    }

    private void showEditWalletDialog(Wallets wallet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_wallet, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etWalletName);
        etName.setText(wallet.getName());

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCreate.setText(R.string.guardar);
        btnCreate.setVisibility(VISIBLE);
        btnDelete.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                wallet.setName(name);
                walletRepository.updateWallet(wallet);
                loadWallets();
                Toast.makeText(requireContext(), "Billetera actualizada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Billetera")
                .setMessage("¿Estás seguro de eliminar '" + wallet.getAppName() + "'?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    walletRepository.deleteWallet(wallet.getId());
                    loadWallets();
                    Toast.makeText(requireContext(), "Billetera eliminada", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null).show());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}
