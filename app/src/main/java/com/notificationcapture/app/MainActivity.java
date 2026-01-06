package com.notificationcapture.app;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.notificationcapture.app.fragments.AgregarFragment;
import com.notificationcapture.app.fragments.CategoriasFragment;
import com.notificationcapture.app.fragments.HistorialFragment;
import com.notificationcapture.app.fragments.InicioFragment;
import com.notificationcapture.app.fragments.PerfilFragment;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.utils.ErrorDialog;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAdd;
    private FragmentManager fragmentManager;
    private boolean isNavigatingProgrammatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargar preferencia de tema
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int nightMode = prefs.getInt("night_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);

        // Cargar preferencia de idioma
        String savedLanguage = prefs.getString("app_language", "es");
        setLocale(savedLanguage);

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAdd = findViewById(R.id.fabAdd);
        fragmentManager = getSupportFragmentManager();

        // Agregar listener para detectar cambios en el back stack
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                syncBottomNavigationWithCurrentFragment();
            }
        });

        // Configurar el listener de la navegación
        bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new InicioFragment();
                    changeFabAddView(View.VISIBLE, getApplicationContext());
                } else if (itemId == R.id.nav_history) {
                    selectedFragment = new HistorialFragment();
                    changeFabAddView(View.VISIBLE, getApplicationContext());
                } else if (itemId == R.id.fabAdd) {
                    selectedFragment = new AgregarFragment();
                } else if (itemId == R.id.nav_categories) {
                    selectedFragment = new CategoriasFragment();
                    changeFabAddView(View.VISIBLE, getApplicationContext());
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new PerfilFragment();
                    changeFabAddView(View.VISIBLE, getApplicationContext());
                }

                if (selectedFragment != null) {
                    isNavigatingProgrammatically = true;
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });

        // Configurar el listener del FAB
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isNavigatingProgrammatically = true;
                loadFragment(new AgregarFragment());
                bottomNavigation.setSelectedItemId(R.id.fabAdd);
                changeFabAddView(View.INVISIBLE, getApplicationContext());
            }
        });

        // Cargar el fragmento inicial (Home)
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, new InicioFragment())
                    .commit();
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        // Inicializar el RepositoryProvider (SOLO UNA VEZ)
        RepositoryProvider.initialize(this);
    }

    private void loadFragment(Fragment fragment) {
        try {
        // Obtener el fragmento actual
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frameLayout);

        // Si es el mismo tipo de fragmento, no hacer nada
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);

        // Solo agregar al back stack si no es el fragmento inicial (Inicio)
        if (!(fragment instanceof InicioFragment)) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
        } catch (Exception e) {
            // Toast.makeText(context, "Error: " + e.getMessage(), 5);
            ErrorDialog.show("Error: " + e.getMessage());
        }
    }

    private void syncBottomNavigationWithCurrentFragment() {

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frameLayout);
        if (currentFragment == null) return;

        int selectedItemId = R.id.nav_home; // Default

        if (currentFragment instanceof InicioFragment) {
            selectedItemId = R.id.nav_home;
            changeFabAddView(View.VISIBLE, getApplicationContext());
        } else if (currentFragment instanceof HistorialFragment) {
            selectedItemId = R.id.nav_history;
            changeFabAddView(View.VISIBLE, getApplicationContext());
        } else if (currentFragment instanceof AgregarFragment) {
            selectedItemId = R.id.fabAdd;
            changeFabAddView(View.INVISIBLE, getApplicationContext());
        } else if (currentFragment instanceof CategoriasFragment) {
            selectedItemId = R.id.nav_categories;
            changeFabAddView(View.VISIBLE, getApplicationContext());
        } else if (currentFragment instanceof PerfilFragment) {
            selectedItemId = R.id.nav_profile;
            changeFabAddView(View.VISIBLE, getApplicationContext());
        }

        // Actualizar el item seleccionado sin disparar el listener
        bottomNavigation.setSelectedItemId(selectedItemId);
    }

    @Override
    public void onBackPressed() {
        // Si hay fragmentos en el back stack, dejar que el sistema maneje el back
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Si no hay más en el stack, cerrar la app
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