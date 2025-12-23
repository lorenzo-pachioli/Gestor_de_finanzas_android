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

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAdd;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargar preferencia de tema
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        // Note: A simple boolean doesn't capture "Follow System".
        // But for this simple implementation:
        // Logic: Check if "dark_mode_set" exists. If not, follow system.

        // Better approach:
        int nightMode = prefs.getInt("night_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAdd = findViewById(R.id.fabAdd);
        fragmentManager = getSupportFragmentManager();

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
                loadFragment(new AgregarFragment());
                bottomNavigation.setSelectedItemId(R.id.fabAdd); // Deseleccionar items del menú
                // fabAdd.setVisibility(View.INVISIBLE);
                changeFabAddView(View.INVISIBLE, getApplicationContext());
            }
        });

        // Cargar el fragmento inicial (Home)
        if (savedInstanceState == null) {
            loadFragment(new InicioFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.commit();
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
}