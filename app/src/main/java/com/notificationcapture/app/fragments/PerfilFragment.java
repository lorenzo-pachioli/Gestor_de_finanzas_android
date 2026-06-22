package com.notificationcapture.app.fragments;

// ARCHIVO COMPLETO: app/src/main/java/com/notificationcapture/app/fragments/PerfilFragment.java

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.notificationcapture.app.R;
import com.notificationcapture.app.fragments.settings.CardsSettingsFragment;
import com.notificationcapture.app.fragments.settings.CategoriesSettingsFragment;
import com.notificationcapture.app.fragments.settings.WalletsSettingsFragment;
import com.notificationcapture.app.utils.CustomLanguageSwitch;
import com.notificationcapture.app.utils.SecurityPreferencesManager;
import com.notificationcapture.app.viewmodels.SettingsViewModel;

public class PerfilFragment extends Fragment {

    private View layoutSettingsMenu;
    private View settingsDetailContainer;
    private OnBackPressedCallback backCallback;
    private SwitchCompat swDarkMode;
    private CustomLanguageSwitch swLanguage;

    public PerfilFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutSettingsMenu = view.findViewById(R.id.layoutSettingsMenu);
        settingsDetailContainer = view.findViewById(R.id.settingsDetailContainer);
        swDarkMode = view.findViewById(R.id.switchDarkMode);
        swLanguage = view.findViewById(R.id.swLanguage);

/*         android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE); */

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
        });

        view.findViewById(R.id.itemCategorias).setOnClickListener(v -> {
            CategoriesSettingsFragment f = new CategoriesSettingsFragment();
            f.setOnBackRequested(this::showMenu);
            showDetail(f);
        });

        view.findViewById(R.id.itemBilleteras).setOnClickListener(v -> {
            WalletsSettingsFragment f = new WalletsSettingsFragment();
            f.setOnBackRequested(this::showMenu);
            showDetail(f);
        });

        view.findViewById(R.id.itemTarjetas).setOnClickListener(v -> {
            CardsSettingsFragment f = new CardsSettingsFragment();
            f.setOnBackRequested(this::showMenu);
            showDetail(f);
        });

        // Interceptar back del sistema para volver al menú
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showMenu();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
    }

    private void setLocale(String languageCode) {
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Resources resources = requireContext().getResources();
        android.content.res.Configuration config =
                new android.content.res.Configuration(resources.getConfiguration());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(locale);
            requireContext().createConfigurationContext(config);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void showDetail(Fragment fragment) {
        settingsDetailContainer.setVisibility(View.VISIBLE);
        layoutSettingsMenu.setVisibility(View.GONE);
        backCallback.setEnabled(true);

        getChildFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
                .replace(R.id.settingsDetailContainer, fragment)
                .commit();
    }

    private void showMenu() {
        settingsDetailContainer.setVisibility(View.GONE);
        layoutSettingsMenu.setVisibility(View.VISIBLE);
        backCallback.setEnabled(false);

        // Limpiar el fragment del container
        Fragment current = getChildFragmentManager().findFragmentById(R.id.settingsDetailContainer);
        if (current != null) {
            getChildFragmentManager().beginTransaction().remove(current).commit();
        }
    }
}
