package com.notificationcapture.app.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.notificationcapture.app.utils.CustomTypeSwitch;
import com.notificationcapture.app.utils.CustomLanguageSwitch;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.notificationcapture.app.utils.SecurityPreferencesManager;
import com.notificationcapture.app.viewmodels.SettingsViewModel;
import static android.view.View.VISIBLE;

import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.CreditCardRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.models.CreditCard;
import com.notificationcapture.app.repositories.WalletRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerfilFragment extends Fragment {

    private WalletRepository walletRepository;
    private CreditCardRepository creditCardRepository;
    private CategoryRepository categoryRepository;
    private Spinner spinnerCategories;
    private Spinner spinnerWallets;
    private Spinner spinnerCreditCards;
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> walletAdapter;
    private CustomTypeSwitch swCatType;
    private SwitchCompat swDarkMode;
    private CustomLanguageSwitch swLanguage;

    // Listas para mantener referencia a los objetos actuales
    private List<Category> currentCategories;
    private List<com.notificationcapture.app.models.Wallets> currentWallets;
    private List<CreditCard> currentCreditCards;

    private int selectedColor; // Set dynamically

    public PerfilFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        walletRepository = RepositoryProvider.getInstance().getWalletRepository();
        categoryRepository = RepositoryProvider.getInstance().getCategoryRepository();
        creditCardRepository = RepositoryProvider.getInstance().getCreditCardRepository();

        // Initialize UI Views
        spinnerCategories = view.findViewById(R.id.spinnerCategories);
        spinnerWallets = view.findViewById(R.id.spinnerWallets);
        swCatType = view.findViewById(R.id.swCatType);
        swDarkMode = view.findViewById(R.id.switchDarkMode);
        swLanguage = view.findViewById(R.id.swLanguage);
        Button btnAddCategory = view.findViewById(R.id.btnAddCategory);
        Button btnAddWallet = view.findViewById(R.id.btnAddWallet);
        Button btnAddCreditCard = view.findViewById(R.id.btnAddCreditCard);

        spinnerCreditCards = view.findViewById(R.id.spinnerCreditCards);

        // Setup Listeners
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        btnAddWallet.setOnClickListener(v -> showGlobalWalletSelectionDialog());
        btnAddCreditCard.setOnClickListener(v -> showAddCreditCardDialog());

        SecurityPreferencesManager prefsManager = new SecurityPreferencesManager(requireContext());
        SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        // Setup Dark Mode Switch
        int nightMode = prefsManager.getNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            swDarkMode.setChecked(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        } else {
            swDarkMode.setChecked(nightMode == AppCompatDelegate.MODE_NIGHT_YES);
        }

        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            settingsViewModel.setNightMode(mode);
            prefsManager.saveNightMode(mode);
        });

        // Setup Language Switch - Load saved language
        String savedLanguage = prefsManager.getLanguage("es"); // Default to Spanish
        swLanguage.setChecked(savedLanguage.equals("en"), false);

        swCatType.setOnCheckedChangeListener(isChecked -> updateCategoryTypeUI(isChecked));

        // Setup Language Switch
        swLanguage.setOnCheckedChangeListener(isChecked -> {
            String newLanguage = isChecked ? "en" : "es";
            String currentLanguage = prefsManager.getLanguage("es");

            if (!newLanguage.equals(currentLanguage)) {
                // Save new language preference securely
                prefsManager.saveLanguage(newLanguage);
                // Report to activity for localized context propagation
                settingsViewModel.setLanguage(newLanguage);

                requireActivity().recreate();
            }

            updateLanguageUI(isChecked);
        });

        updateLanguageUI(savedLanguage.equals("en"));

        // Setup Spinner Listeners
        setupSpinnerListeners();

        // Initial Load
        updateCategoryTypeUI(swCatType.isChecked());
        updateLanguageUI(swLanguage.isChecked());
        loadCategories();
        loadWallets();
        loadCreditCards();
    }

    private void updateLanguageUI(boolean isEnglish) {
        // Switch handles its own UI
    }


    private void setupSpinnerListeners() {
        spinnerCategories.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // 0 is placeholder
                    Category selectedCat = currentCategories.get(position - 1);
                    showEditCategoryDialog(selectedCat);
                    // Reset selection
                    spinnerCategories.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spinnerWallets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // 0 is placeholder
                    com.notificationcapture.app.models.Wallets selectedWallet = currentWallets.get(position - 1);
                    showEditWalletDialog(selectedWallet);
                    // Reset selection
                    spinnerWallets.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spinnerCreditCards.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // 0 is placeholder
                    CreditCard selectedCard = currentCreditCards.get(position - 1);
                    showEditCreditCardDialog(selectedCard);
                    // Reset selection
                    spinnerCreditCards.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void updateCategoryTypeUI(boolean isEgreso) {
        loadCategories();
    }

    private void loadCategories() {
        IngresoOEgreso type = swCatType.isChecked()
                ? IngresoOEgreso.EGRESO
                : IngresoOEgreso.INGRESO;
        currentCategories = categoryRepository.getCategories(type);

        List<Category> displayList = new ArrayList<>();
        // Placeholder
        displayList.add(new Category("Seleccionar para editar/borrar...", type));
        displayList.addAll(currentCategories);

        UniversalSpinnerAdapter<Category> adapter = new UniversalSpinnerAdapter<>(requireContext(), displayList);
        spinnerCategories.setAdapter(adapter);
    }

    private void loadWallets() {
        currentWallets = walletRepository.getAllWallets();

        List<Wallets> displayList = new ArrayList<>();
        displayList.add(new Wallets("Seleccionar para editar/borrar...", "")); // Placeholder
        displayList.addAll(currentWallets);

        UniversalSpinnerAdapter<com.notificationcapture.app.models.Wallets> adapter = new UniversalSpinnerAdapter<>(
                requireContext(), displayList);
        spinnerWallets.setAdapter(adapter);
    }

    private void loadCreditCards() {
        currentCreditCards = creditCardRepository.getCreditCards();

        List<CreditCard> displayList = new ArrayList<>();
        displayList.add(new CreditCard("Seleccionar para editar/borrar...", 0, 0)); // Placeholder or dummy
        displayList.addAll(currentCreditCards);

        UniversalSpinnerAdapter<CreditCard> adapter = new UniversalSpinnerAdapter<>(requireContext(), displayList);
        spinnerCreditCards.setAdapter(adapter);
    }

    // --- Dialogs ---

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        LinearLayout containerColors = dialogView.findViewById(R.id.containerColors);

        // Populate Colors
        IngresoOEgreso type = swCatType.isChecked()
                ? IngresoOEgreso.EGRESO
                : IngresoOEgreso.INGRESO;

        int[] colors = CatColors.getColorsByType(type); // (type == IngresoOEgreso.INGRESO) ? CatColors.INGRESOS_COLORS
                                                        // : CatColors.EGRESOS_COLORS;
        selectedColor = colors[0]; // Default to first

        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreate.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                IngresoOEgreso transactionTypeype = swCatType.isChecked()
                        ? IngresoOEgreso.EGRESO
                        : IngresoOEgreso.INGRESO;
                Category newCat = new Category(name, selectedColor, transactionTypeype);
                categoryRepository.addCategory(newCat);
                loadCategories();
                Toast.makeText(requireContext(), "Categoría agregada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Hacer el fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void showGlobalWalletSelectionDialog() {
        List<com.notificationcapture.app.models.GlobalWallet> globalWallets = com.notificationcapture.app.utils.ConfigManager.getInstance().getGlobalWallets();
        List<com.notificationcapture.app.models.Wallets> userWallets = walletRepository.getAllWallets();

        // Construir conjunto de packages del usuario para highlight
        List<String> userWalletPackages = new ArrayList<>();
        for (com.notificationcapture.app.models.Wallets w : userWallets) {
            userWalletPackages.add(w.getPackageName());
        }

        // Convertir GlobalWallet a lista Serializable para el BottomSheet
        // El BottomSheet usa SpinnerDisplayable — GlobalWallet ya lo implementa
        List<com.notificationcapture.app.models.GlobalWallet> globalList = new ArrayList<>(globalWallets);

        // highlighted: packageName primario de cada global wallet que ya tiene el usuario
        List<String> highlightedPrimaries = new ArrayList<>();
        for (com.notificationcapture.app.models.GlobalWallet gw : globalWallets) {
            for (String userPkg : userWalletPackages) {
                if (gw.matchesPackage(userPkg)) {
                    highlightedPrimaries.add(gw.getPrimaryPackageName());
                    break;
                }
            }
        }

        SelectorBottomSheet bottomSheet = SelectorBottomSheet.newInstance(
                "Seleccionar Billetera",
                globalList,
                "",
                highlightedPrimaries
        );

        bottomSheet.setOnOptionSelectedListener(optionName -> {
            for (com.notificationcapture.app.models.GlobalWallet globalWallet : globalWallets) {
                if (globalWallet.getName().equals(optionName)) {
                    // Verificar si ya existe (matchea cualquier package alternativo)
                    boolean exists = false;
                    for (com.notificationcapture.app.models.Wallets uw : userWallets) {
                        if (globalWallet.matchesPackage(uw.getPackageName())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        walletRepository.addWallet(globalWallet.toUserWallet());
                        loadWallets();
                        Toast.makeText(requireContext(), "Billetera '" + optionName + "' agregada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "La billetera '" + optionName + "' ya está agregada", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        });

        bottomSheet.show(getChildFragmentManager(), "WalletSelection");
    }

    private void showAddWalletDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_wallet, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etWalletName);

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreate.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                Wallets newWallet = new Wallets(name, ("com." + name + ".app")); // definir si se habilita crear wallets
                                                                                 // y como
                walletRepository.addWallet(newWallet);
                loadWallets();
                Toast.makeText(requireContext(), "Billetera agregada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Hacer el fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void showAddCreditCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_credit_card, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etCardName);
        EditText etClosingDate = dialogView.findViewById(R.id.etClosingDate);
        EditText etLast4 = dialogView.findViewById(R.id.etLast4);
        LinearLayout containerColors = dialogView.findViewById(R.id.containerCardColors);

        // Populate Colors (use same logic as categories or a separate palette?)
        int[] colors = CatColors.getIntEgresosColors(); // Use outcome colors for cards
        selectedColor = colors[0];
        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreateCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelCard);

        btnCreate.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String closingDateStr = etClosingDate.getText().toString().trim();
            String last4 = etLast4.getText().toString().trim();
            
            int closingDate = 1;
            try {
                closingDate = Integer.parseInt(closingDateStr);
                if (closingDate < 1 || closingDate > 31)
                    closingDate = 1;
            } catch (NumberFormatException e) {
                // Ignore
            }

            if (!name.isEmpty()) {
                CreditCard newCard = new CreditCard(name, closingDate, selectedColor, last4.isEmpty() ? "0000" : last4);
                creditCardRepository.addCreditCard(newCard);
                loadCreditCards();
                Toast.makeText(requireContext(), "Tarjeta agregada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // --- Edit Dialogs ---

    private void showEditCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        LinearLayout containerColors = dialogView.findViewById(R.id.containerColors);

        etName.setText(category.getName());
        selectedColor = category.getDisplayColor();

        // Populate Colors
        // int[] colors = (category.getType() == IngresoOEgreso.INGRESO) ?
        // CatColors.INGRESOS_COLORS
        // CatColors.EGRESOS_COLORS;
        populateColorPicker(containerColors, CatColors.getColorsByType(category.getType()));

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        btnCreate.setText(R.string.guardar);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreate.setVisibility(VISIBLE);
        btnDelete.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                Category newCat = new Category(category.getId(), name, selectedColor, category.getType());
                categoryRepository.updateCategory(newCat);
                loadCategories();
                Toast.makeText(requireContext(), "Categoría actualizada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar Categoría")
                    .setMessage("¿Estás seguro de eliminar '" + category.getName() + "'?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        categoryRepository.deleteCategory(category.getId());
                        loadCategories();
                        Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Hacer el fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
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
                wallet.setPackageName(wallet.getPackageName());
                walletRepository.updateWallet(wallet);
                loadWallets();
                Toast.makeText(requireContext(), "Billetera actualizada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setView(LayoutInflater.from(requireContext()).inflate(R.layout.delete_confirmation_layout, null))
                    .setTitle("Eliminar Billetera")
                    .setMessage("¿Estás seguro de eliminar '" + wallet.getAppName() + "'?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        walletRepository.deleteWallet(wallet.getId());
                        loadWallets();
                        Toast.makeText(requireContext(), "Billetera eliminada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Hacer el fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void showEditCreditCardDialog(CreditCard card) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_credit_card, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etCardName);
        EditText etClosingDate = dialogView.findViewById(R.id.etClosingDate);
        EditText etLast4 = dialogView.findViewById(R.id.etLast4);
        LinearLayout containerColors = dialogView.findViewById(R.id.containerCardColors);

        etName.setText(card.getName());
        etClosingDate.setText(String.valueOf(card.getClosingDate()));
        etLast4.setText(card.getLast4());
        selectedColor = card.getColor();

        int[] colors = CatColors.getIntEgresosColors();
        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreateCard);
        Button btnDelete = dialogView.findViewById(R.id.btnDeleteCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelCard);

        btnCreate.setText("Actualizar"); // Reuse Create button (or change text/logic)

        btnCreate.setVisibility(VISIBLE);
        btnDelete.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String closingDateStr = etClosingDate.getText().toString().trim();
            String last4 = etLast4.getText().toString().trim();
            
            int closingDate = 1;
            try {
                closingDate = Integer.parseInt(closingDateStr);
                if (closingDate < 1 || closingDate > 31)
                    closingDate = 1;
            } catch (NumberFormatException e) {
            }

            if (!name.isEmpty()) {
                card.setName(name);
                card.setClosingDate(closingDate);
                card.setColor(selectedColor);
                card.setLast4(last4.isEmpty() ? "0000" : last4);
                creditCardRepository.updateCreditCard(card);
                loadCreditCards();
                Toast.makeText(requireContext(), "Tarjeta actualizada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar Tarjeta")
                    .setMessage("¿Estás seguro de eliminar '" + card.getName() + "'?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        creditCardRepository.deleteCreditCard(card.getId());
                        loadCreditCards();
                        Toast.makeText(requireContext(), "Tarjeta eliminada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void populateColorPicker(LinearLayout container, int[] colors) {
        List<View> colorViews = new ArrayList<>();
        container.removeAllViews();

        for (int color : colors) {
            View colorDot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            colorDot.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            if (color == selectedColor) {
                drawable.setStroke(dpToPx(3), Color.BLACK);
            }
            colorDot.setBackground(drawable);

            colorDot.setOnClickListener(v -> {
                selectedColor = color;
                for (View cv : colorViews) {
                    GradientDrawable bg = (GradientDrawable) cv.getBackground();
                    bg.setStroke(0, Color.TRANSPARENT);
                }
                drawable.setStroke(dpToPx(3), Color.BLACK);
            });

            container.addView(colorDot);
            colorViews.add(colorDot);
        }
    }
}