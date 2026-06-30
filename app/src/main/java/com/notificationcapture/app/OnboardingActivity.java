package com.notificationcapture.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.notificationcapture.app.fragments.onboarding.OnboardingPageFragment;
import com.notificationcapture.app.fragments.onboarding.TermsAndPrivacyFragment;
import com.notificationcapture.app.utils.ConsentPreferencesManager;

public class OnboardingActivity extends AppCompatActivity {

    private static final int TOTAL_PAGES = 5;
    private ViewPager2 viewPager;
    private Button btnNext;
    private Button btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        TabLayout dots = findViewById(R.id.dotsIndicator);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        viewPager.setAdapter(new OnboardingAdapter(this));
        new TabLayoutMediator(dots, viewPager, (tab, pos) -> {}).attach();

        btnNext.setOnClickListener(v -> {
            int next = viewPager.getCurrentItem() + 1;
            if (next < TOTAL_PAGES) viewPager.setCurrentItem(next, true);
        });

        // "Saltar" lleva directo a la pantalla de términos; NUNCA la omite
        btnSkip.setOnClickListener(v -> viewPager.setCurrentItem(TOTAL_PAGES - 1, true));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean isLastPage = position == TOTAL_PAGES - 1;
                btnNext.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
                btnSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
            }
        });
    }

    /** Llamado por TermsAndPrivacyFragment cuando el usuario tilda el checkbox y pulsa Aceptar. */
    public void onTermsAccepted() {
        new ConsentPreferencesManager(this).saveAcceptance();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── Adaptador ──────────────────────────────────────────────────────────────

    private static class OnboardingAdapter extends FragmentStateAdapter {
        OnboardingAdapter(@NonNull FragmentActivity fa) { super(fa); }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            switch (position) {
                case 0: return OnboardingPageFragment.newInstance(
                        R.drawable.fluxus_logo,
                        R.string.onboarding_title_1,
                        R.string.onboarding_desc_1);
                case 1: return OnboardingPageFragment.newInstance(
                        R.drawable.onboarding_add,
                        R.string.onboarding_title_2,
                        R.string.onboarding_desc_2);
                case 2: return OnboardingPageFragment.newInstance(
                        R.drawable.onboarding_categories,
                        R.string.onboarding_title_3,
                        R.string.onboarding_desc_3);
                case 3: return OnboardingPageFragment.newInstance(
                        R.drawable.onboarding_notifications,
                        R.string.onboarding_title_4,
                        R.string.onboarding_desc_4);
                case 4:
                default: return new TermsAndPrivacyFragment();
            }
        }

        @Override
        public int getItemCount() { return TOTAL_PAGES; }
    }
}
