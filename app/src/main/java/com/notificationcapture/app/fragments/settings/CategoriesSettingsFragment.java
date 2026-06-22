package com.notificationcapture.app.fragments.settings;

// ARCHIVO NUEVO: app/src/main/java/com/notificationcapture/app/fragments/settings/CategoriesSettingsFragment.java

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.notificationcapture.app.R;
import com.notificationcapture.app.adapters.UniversalSpinnerAdapter;
import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.repositories.CategoryRepository;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.utils.CustomTypeSwitch;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;

public class CategoriesSettingsFragment extends Fragment {

    private Runnable onBackRequested;
    private CategoryRepository categoryRepository;
    private Spinner spinnerCategories;
    private CustomTypeSwitch swCatType;
    private List<Category> currentCategories = new ArrayList<>();
    private int selectedColor;

    public void setOnBackRequested(Runnable r) { this.onBackRequested = r; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        categoryRepository = RepositoryProvider.getInstance().getCategoryRepository();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (onBackRequested != null) onBackRequested.run();
        });

        spinnerCategories = view.findViewById(R.id.spinnerCategories);
        swCatType = view.findViewById(R.id.swCatType);
        Button btnAdd = view.findViewById(R.id.btnAddCategory);

        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
        swCatType.setOnCheckedChangeListener(isEgreso -> loadCategories());

        loadCategories();

        spinnerCategories.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                if (position > 0) {
                    showEditCategoryDialog(currentCategories.get(position - 1));
                    spinnerCategories.setSelection(0);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadCategories() {
        IngresoOEgreso type = swCatType.isChecked() ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
        currentCategories = categoryRepository.getCategories(type);

        List<Category> displayList = new ArrayList<>();
        displayList.add(new Category("Seleccionar para editar/borrar...", type));
        displayList.addAll(currentCategories);

        UniversalSpinnerAdapter<Category> adapter = new UniversalSpinnerAdapter<>(requireContext(), displayList);
        spinnerCategories.setAdapter(adapter);
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        LinearLayout containerColors = dialogView.findViewById(R.id.containerColors);
        IngresoOEgreso type = swCatType.isChecked() ? IngresoOEgreso.EGRESO : IngresoOEgreso.INGRESO;
        int[] colors = CatColors.getColorsByType(type);
        selectedColor = colors[0];
        populateColorPicker(containerColors, colors);

        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCreate.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                categoryRepository.addCategory(new Category(name, selectedColor, type));
                loadCategories();
                Toast.makeText(requireContext(), "Categoría agregada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void showEditCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        LinearLayout containerColors = dialogView.findViewById(R.id.containerColors);
        etName.setText(category.getName());
        selectedColor = category.getDisplayColor();
        populateColorPicker(containerColors, CatColors.getColorsByType(category.getType()));

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
                categoryRepository.updateCategory(new Category(category.getId(), name, selectedColor, category.getType()));
                loadCategories();
                Toast.makeText(requireContext(), "Categoría actualizada", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Categoría")
                .setMessage("¿Estás seguro de eliminar '" + category.getName() + "'?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    categoryRepository.deleteCategory(category.getId());
                    loadCategories();
                    Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null).show());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void populateColorPicker(LinearLayout container, int[] colors) {
        List<View> colorViews = new ArrayList<>();
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int sizePx = Math.round(40 * density);
        int marginPx = Math.round(4 * density);

        for (int color : colors) {
            View colorDot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(marginPx, 0, marginPx, 0);
            colorDot.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            if (color == selectedColor) drawable.setStroke(Math.round(3 * density), Color.BLACK);
            colorDot.setBackground(drawable);

            colorDot.setOnClickListener(v -> {
                selectedColor = color;
                for (View cv : colorViews) ((GradientDrawable) cv.getBackground()).setStroke(0, Color.TRANSPARENT);
                drawable.setStroke(Math.round(3 * density), Color.BLACK);
            });
            container.addView(colorDot);
            colorViews.add(colorDot);
        }
    }
}
