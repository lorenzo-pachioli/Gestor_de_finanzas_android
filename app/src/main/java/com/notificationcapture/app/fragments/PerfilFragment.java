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
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;

import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.CreditCardRepository;
import com.notificationcapture.app.repositories.TransactionRepository;
import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.models.CreditCard;
import com.notificationcapture.app.enums.TransactionType;
import com.notificationcapture.app.repositories.WalletRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerfilFragment extends Fragment {

    private TransactionRepository repository;
    private WalletRepository walletRepository;
    private CreditCardRepository creditCardRepository;
    private CategoryRepository categoryRepository;
    private Spinner spinnerCategories;
    private Spinner spinnerWallets;
    private Spinner spinnerCreditCards;
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> walletAdapter;
    private SwitchCompat swCatType;
    private SwitchCompat swDarkMode;
    private SwitchCompat swLanguage;
    private TextView tvCatIngreso;
    private TextView tvCatEgreso;
    private TextView tvEspanol;
    private TextView tvEnglish;

    // Listas para mantener referencia a los objetos actuales
    private List<Category> currentCategories;
    private List<String> currentWallets;
    private List<CreditCard> currentCreditCards;

    private Button btnShowBottomSheet;

    // Colores predefinidos para selección
    // Colores Cálidos (Ingresos)
    private final int[] INGRESOS_COLORS = {
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#CDDC39"), // Lime
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#795548"), // Brown

    };

    // Colores Fríos (Egresos)
    private final int[] EGRESOS_COLORS = {
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#03A9F4"), // Light Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#607D8B"), // Blue Grey
            Color.parseColor("#9E9E9E") // Grey
    };
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
        repository = new TransactionRepository(requireContext());
        walletRepository = new WalletRepository(requireContext());
        categoryRepository = new CategoryRepository(requireContext());
        creditCardRepository = new CreditCardRepository(requireContext());

        // Initialize UI Views
        spinnerCategories = view.findViewById(R.id.spinnerCategories);
        spinnerWallets = view.findViewById(R.id.spinnerWallets);
        swCatType = view.findViewById(R.id.swCatType);
        swDarkMode = view.findViewById(R.id.switchDarkMode);
        swLanguage = view.findViewById(R.id.swLanguage);
        tvCatIngreso = view.findViewById(R.id.tvCatIngreso);
        tvCatEgreso = view.findViewById(R.id.tvCatEgreso);
        tvEspanol = view.findViewById(R.id.tvEspanol);
        tvEnglish = view.findViewById(R.id.tvEnglish);
        Button btnAddCategory = view.findViewById(R.id.btnAddCategory);
        Button btnAddWallet = view.findViewById(R.id.btnAddWallet);
        Button btnAddCreditCard = view.findViewById(R.id.btnAddCreditCard);
        Button btnShowBottomSheet = view.findViewById(R.id.btnShowBottomSheet);

        spinnerCreditCards = view.findViewById(R.id.spinnerCreditCards);

        btnShowBottomSheet.setOnClickListener(v -> showTestDialog());

        // Setup Listeners
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        btnAddWallet.setOnClickListener(v -> showAddWalletDialog());
        btnAddCreditCard.setOnClickListener(v -> showAddCreditCardDialog());

        // Setup Dark Mode Switch
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", MODE_PRIVATE);
        int nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            swDarkMode.setChecked(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        } else {
            swDarkMode.setChecked(nightMode == AppCompatDelegate.MODE_NIGHT_YES);
        }

        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(mode);
            prefs.edit().putInt("night_mode", mode).apply();
        });

        // Setup Language Switch - Load saved language
        String savedLanguage = prefs.getString("app_language", "es"); // Default to Spanish
        swLanguage.setChecked(savedLanguage.equals("en"));

        swCatType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateCategoryTypeUI(isChecked);
        });

        // Listeners para etiquetas del switch de categorías
        tvCatIngreso.setOnClickListener(v -> swCatType.setChecked(false));
        tvCatEgreso.setOnClickListener(v -> swCatType.setChecked(true));

        // Setup Language Switch
        swLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newLanguage = isChecked ? "en" : "es";
            String currentLanguage = prefs.getString("app_language", "es");

            if (!newLanguage.equals(currentLanguage)) {
                // Save new language preference
                prefs.edit().putString("app_language", newLanguage).apply();

                // Change locale and recreate activity
                setLocale(newLanguage);
                requireActivity().recreate();
            }

            updateLanguageUI(isChecked);
        });

        // Listeners para etiquetas del switch de idioma
        tvEspanol.setOnClickListener(v -> swLanguage.setChecked(false));
        tvEnglish.setOnClickListener(v -> swLanguage.setChecked(true));

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
        if (isEnglish) {
            // English selected (Switch ON, Right)
            tvEspanol.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_unselected));
            tvEnglish.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_main));
        } else {
            // Español selected (Switch OFF, Left)
            tvEspanol.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_main));
            tvEnglish.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_unselected));
        }
    }

    private void setLocale(String languageCode) {
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);

        android.content.res.Resources resources = requireContext().getResources();
        android.content.res.Configuration config = new android.content.res.Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            requireContext().createConfigurationContext(config);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
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
                    String selectedWallet = currentWallets.get(position - 1);
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

        if (isEgreso) {
            // Modo Egreso (Switch ON, Derecha)
            tvCatIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_unselected));
            tvCatEgreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        } else {
            // Modo Ingreso (Switch OFF, Izquierda)
            tvCatIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
            tvCatEgreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_unselected));
        }

        loadCategories();
    }

    private void loadCategories() {
        TransactionType type = swCatType.isChecked()
                ? TransactionType.EGRESO
                : TransactionType.INGRESO;
        currentCategories = categoryRepository.getCategories(type);

        List<Category> displayList = new ArrayList<>();
        // Placeholder
        displayList.add(new Category("Seleccionar para editar/borrar...", 0, type));
        displayList.addAll(currentCategories);

        UniversalSpinnerAdapter<Category> adapter = new UniversalSpinnerAdapter<>(requireContext(), displayList);
        spinnerCategories.setAdapter(adapter);
    }

    private void loadWallets() {
        currentWallets = walletRepository.getWallets();

        List<String> displayList = new ArrayList<>();
        displayList.add("Seleccionar para editar/borrar..."); // Placeholder
        displayList.addAll(currentWallets);

        UniversalSpinnerAdapter<String> adapter = new UniversalSpinnerAdapter<>(requireContext(), displayList);
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
        TransactionType type = swCatType.isChecked()
                ? TransactionType.EGRESO
                : TransactionType.INGRESO;

        int[] colors = (type == TransactionType.INGRESO) ? INGRESOS_COLORS : EGRESOS_COLORS;
        selectedColor = colors[0]; // Default to first

        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreate.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                TransactionType transactionTypeype = swCatType.isChecked()
                        ? TransactionType.EGRESO
                        : TransactionType.INGRESO;
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
                walletRepository.addWallet(name);
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
        LinearLayout containerColors = dialogView.findViewById(R.id.containerCardColors);

        // Populate Colors (use same logic as categories or a separate palette?)
        int[] colors = EGRESOS_COLORS; // Use outcome colors for cards
        selectedColor = colors[0];
        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreateCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelCard);

        btnCreate.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String closingDateStr = etClosingDate.getText().toString().trim();
            int closingDate = 1;
            try {
                closingDate = Integer.parseInt(closingDateStr);
                if (closingDate < 1 || closingDate > 31)
                    closingDate = 1;
            } catch (NumberFormatException e) {
                // Ignore
            }

            if (!name.isEmpty()) {
                CreditCard newCard = new CreditCard(name, closingDate, selectedColor);
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
        selectedColor = category.getColor();

        etName.setText(category.getName());
        selectedColor = category.getColor();

        // Populate Colors
        int[] colors = (category.getType() == TransactionType.INGRESO) ? INGRESOS_COLORS
                : EGRESOS_COLORS;
        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreate.setVisibility(VISIBLE);
        btnDelete.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                Category newCat = new Category(name, selectedColor, category.getType());
                categoryRepository.updateCategory(category, newCat);
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
                        categoryRepository.deleteCategory(category.getName(), category.getType());
                        loadCategories();
                        Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
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

    private void showEditWalletDialog(String currentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_wallet, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etWalletName);
        etName.setText(currentName);

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreate.setVisibility(VISIBLE);
        btnDelete.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                walletRepository.updateWallet(currentName, name);
                loadWallets();
                Toast.makeText(requireContext(), "Billetera actualizada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setView(LayoutInflater.from(requireContext()).inflate(R.layout.delete_confirmation_layout, null))
                    .setTitle("Eliminar Billetera")
                    .setMessage("¿Estás seguro de eliminar '" + currentName + "'?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        walletRepository.deleteWallet(currentName);
                        loadWallets();
                        Toast.makeText(requireContext(), "Billetera eliminada", Toast.LENGTH_SHORT).show();
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
        LinearLayout containerColors = dialogView.findViewById(R.id.containerCardColors);

        etName.setText(card.getName());
        etClosingDate.setText(String.valueOf(card.getClosingDate()));
        selectedColor = card.getColor();

        int[] colors = EGRESOS_COLORS;
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

    private void showTestDialog() {
        MyBottomSheetDialogFragment bottomSheet = MyBottomSheetDialogFragment.newInstance("Test Dialog",
                new ArrayList<>(), new HashMap<>());
        bottomSheet.show(getParentFragmentManager(), "EtiquetaUnica");
    }

}