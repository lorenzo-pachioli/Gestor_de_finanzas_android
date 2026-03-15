package com.notificationcapture.app;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.notificationcapture.app.fragments.AgregarFragment;
import com.notificationcapture.app.fragments.CategoriasFragment;
import com.notificationcapture.app.fragments.HistorialFragment;
import com.notificationcapture.app.fragments.InicioFragment;
import com.notificationcapture.app.fragments.PerfilFragment;
import com.notificationcapture.app.viewmodels.SettingsViewModel;
import com.notificationcapture.app.utils.SecurityPreferencesManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private CardView fabAdd;
    private ViewPager2 viewPager;
    private SettingsViewModel settingsViewModel;
    private SecurityPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Gestor de Preferencias Seguro y ViewModel
        prefsManager = new SecurityPreferencesManager(this);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Configurar Observadores para cambios de estado (MVVM)
        settingsViewModel.getNightModeState().observe(this, mode -> {
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        settingsViewModel.getLanguageState().observe(this, lang -> {
            setLocale(lang);
        });

        // Cargar valores iniciales desde almacenamiento seguro
        int nightMode = prefsManager.getNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        String savedLanguage = prefsManager.getLanguage("es");
        
        settingsViewModel.setNightMode(nightMode);
        settingsViewModel.setLanguage(savedLanguage);

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAdd = findViewById(R.id.fabAdd);
        viewPager = findViewById(R.id.viewPager);

        // Configurar ViewPager2 con el adaptador
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(true); // Habilitar swipe

        // Sincronizar ViewPager → BottomNavigation
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Actualizar item seleccionado en el menú según la posición
                int menuItemId = getMenuItemIdFromPosition(position);
                bottomNavigation.setSelectedItemId(menuItemId);

                // Actualizar visibilidad del FAB
                if (position == 2) { // AgregarFragment
                    changeFabAddView(View.INVISIBLE, getApplicationContext());
                } else {
                    changeFabAddView(View.VISIBLE, getApplicationContext());
                }
            }
        });

        // Sincronizar BottomNavigation → ViewPager
        bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int position = getPositionFromMenuItemId(item.getItemId());
                if (position != -1) {
                    viewPager.setCurrentItem(position, true); // true = animación suave
                    return true;
                }
                return false;
            }
        });

        // Configurar el listener del FAB
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2, true); // Navegar a AgregarFragment (posición 2)
            }
        });

        // Cargar el fragmento inicial (Home)
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(0, false); // InicioFragment, sin animación
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Adaptador para ViewPager2 que gestiona los 5 fragments
     */
    private class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Retornar fragment según posición
            switch (position) {
                case 0:
                    return new InicioFragment();
                case 1:
                    return new HistorialFragment();
                case 2:
                    return new AgregarFragment();
                case 3:
                    return new CategoriasFragment();
                case 4:
                    return new PerfilFragment();
                default:
                    return new InicioFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5; // Total de fragments
        }
    }

    /**
     * Convierte un ID de menu item a posición en el ViewPager
     */
    private int getPositionFromMenuItemId(int menuItemId) {
        if (menuItemId == R.id.nav_home) {
            return 0;
        } else if (menuItemId == R.id.nav_history) {
            return 1;
        } else if (menuItemId == R.id.fabAddInvisible) {
            return 2;
        } else if (menuItemId == R.id.nav_categories) {
            return 3;
        } else if (menuItemId == R.id.nav_profile) {
            return 4;
        }
        return -1;
    }

    /**
     * Convierte una posición del ViewPager a ID de menu item
     */
    private int getMenuItemIdFromPosition(int position) {
        switch (position) {
            case 0:
                return R.id.nav_home;
            case 1:
                return R.id.nav_history;
            case 2:
                return R.id.fabAddInvisible;
            case 3:
                return R.id.nav_categories;
            case 4:
                return R.id.nav_profile;
            default:
                return R.id.nav_home;
        }
    }

    @Override
    public void onBackPressed() {
        // Si no estamos en la página inicial, volver a ella
        if (viewPager.getCurrentItem() != 0) {
            viewPager.setCurrentItem(0, true);
        } else {
            // Si ya estamos en inicio, cerrar la app
            super.onBackPressed();
        }
    }

    private void changeFabAddView(int visibility, Context context) {
        if (fabAdd.getVisibility() == visibility) {
            return;
        }

        if (visibility == View.VISIBLE) {
            fabAdd.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            fabAdd.startAnimation(fadeIn);
        } else {
            Animation fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fabAdd.setVisibility(visibility);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            fabAdd.startAnimation(fadeOut);
        }
    }

    private void setLocale(String languageCode) {
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);

        android.content.res.Resources resources = getResources();
        android.content.res.Configuration config = new android.content.res.Configuration(resources.getConfiguration());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(locale);
            createConfigurationContext(config);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}