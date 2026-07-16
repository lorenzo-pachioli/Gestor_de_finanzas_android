package com.notificationcapture.app;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.notificationcapture.app.utils.LocaleUtils;
import com.notificationcapture.app.utils.SecurityPreferencesManager;
import com.notificationcapture.app.viewmodels.SettingsViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private CardView fabAdd;
    private ViewPager2 viewPager;
    private View topBar;
    private SettingsViewModel settingsViewModel;
    private SecurityPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Habilitar edge-to-edge ANTES de setContentView para que el contenido
        // se dibuje detrás de las barras del sistema (status bar + nav bar).
        // Sin esto, en dispositivos físicos modernos el nav bar tapa el contenido.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);

        // Inicializar Gestor de Preferencias Seguro y ViewModel
        prefsManager = new SecurityPreferencesManager(this);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Configurar Observadores para cambios de estado (MVVM)
        settingsViewModel.getNightModeState().observe(this, mode -> {
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        settingsViewModel.getLanguageState().observe(this, lang -> {
            LocaleUtils.applyLocale(this, lang);
        });

        // Cargar valores iniciales desde almacenamiento seguro
        int nightMode = prefsManager.getNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        String savedLanguage = prefsManager.getLanguage(null);
        if (savedLanguage == null) {
            savedLanguage = resolveDefaultLanguage();
            prefsManager.saveLanguage(savedLanguage);
        }

        settingsViewModel.setNightMode(nightMode);
        settingsViewModel.setLanguage(savedLanguage);

        setContentView(R.layout.activity_main);

        topBar = findViewById(R.id.topBar);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAdd = findViewById(R.id.fabAdd);
        viewPager = findViewById(R.id.viewPager);

        // Aplicar insets del sistema a las vistas fijas de la Activity.
        // Esto reemplaza los valores hardcodeados que funcionaban en el emulador
        // pero fallaban en el dispositivo físico por diferencias en la altura
        // de la barra de navegación (gestos vs. botones, distintos fabricantes).
        applyWindowInsets();

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
     * Registra un listener de Window Insets sobre el decor view para reposicionar
     * las vistas fijas (topBar, bottomNavigation, fabAdd) según la altura real de
     * las barras del sistema en cada dispositivo.
     *
     * Al retornar los insets sin consumirlos, el sistema continúa dispatching hacia
     * los fragments dentro del ViewPager2, donde cada vista scrolleable aplica su
     * propio paddingBottom dinámico.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // --- TopBar: empujar el bar entero DEBAJO del status bar con marginTop.
            // Usar paddingTop + altura expandida causaba que el ivLogo (match_parent)
            // quedara renderizado desde y=0 y el status bar tapara el logo.
            // Con marginTop el topBar mantiene su altura original (28dp) y baja
            // limpiamente; el ViewPager (layout_below topBar) se ajusta solo.
            RelativeLayout.LayoutParams topBarLp = (RelativeLayout.LayoutParams) topBar.getLayoutParams();
            topBarLp.topMargin = systemBars.top;
            topBar.setLayoutParams(topBarLp);

            // --- BottomNav: extenderse hacia abajo de la barra de gestos/navegación ---
            // El fondo del BottomNavigationView cubre el área del nav bar;
            // el padding interno empuja los ítems hacia arriba, fuera del área de gestos.
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            ViewGroup.LayoutParams bnLp = bottomNavigation.getLayoutParams();
            bnLp.height = getResources().getDimensionPixelSize(R.dimen.size_60dp) + systemBars.bottom;
            bottomNavigation.setLayoutParams(bnLp);

            // --- FAB: subir para quedar visible sobre la barra de navegación ---
            RelativeLayout.LayoutParams fabLp = (RelativeLayout.LayoutParams) fabAdd.getLayoutParams();
            fabLp.bottomMargin = systemBars.bottom
                    + getResources().getDimensionPixelSize(R.dimen.space_32dp);
            fabAdd.setLayoutParams(fabLp);

            // No consumir: los insets siguen propagándose a los fragments
            return windowInsets;
        });
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

    private String resolveDefaultLanguage() {
        String systemLanguage = Locale.getDefault().getLanguage();
        return "es".equalsIgnoreCase(systemLanguage) ? "es" : "en";
    }
}
