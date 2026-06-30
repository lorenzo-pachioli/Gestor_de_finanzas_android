package com.notificationcapture.app.fragments.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.notificationcapture.app.OnboardingActivity;
import com.notificationcapture.app.R;

public class TermsAndPrivacyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terms_and_privacy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView tvLegal   = view.findViewById(R.id.tvLegalText);
        CheckBox cbAccept  = view.findViewById(R.id.cbAcceptTerms);
        Button   btnAccept = view.findViewById(R.id.btnAcceptAndContinue);

        tvLegal.setText(getString(R.string.terms_and_privacy_full_text));

        btnAccept.setEnabled(false);
        cbAccept.setOnCheckedChangeListener((buttonView, checked) -> btnAccept.setEnabled(checked));

        btnAccept.setOnClickListener(v -> {
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).onTermsAccepted();
            }
        });
    }
}
