package com.notificationcapture.app.fragments.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.notificationcapture.app.R;

public class OnboardingPageFragment extends Fragment {

    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_TITLE_RES = "arg_title_res";
    private static final String ARG_DESC_RES = "arg_desc_res";

    public static OnboardingPageFragment newInstance(int imageRes, int titleRes, int descRes) {
        OnboardingPageFragment f = new OnboardingPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putInt(ARG_TITLE_RES, titleRes);
        args.putInt(ARG_DESC_RES, descRes);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return;
        ((ImageView) view.findViewById(R.id.imgOnboarding))
                .setImageResource(args.getInt(ARG_IMAGE));
        ((TextView) view.findViewById(R.id.tvOnboardingTitle))
                .setText(args.getInt(ARG_TITLE_RES));
        ((TextView) view.findViewById(R.id.tvOnboardingDesc))
                .setText(args.getInt(ARG_DESC_RES));
    }
}
