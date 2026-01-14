package com.notificationcapture.app.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;
import com.notificationcapture.app.R;

public class CustomLanguageSwitch extends ConstraintLayout {

    private View switchThumb;
    private TextView tvLeft;
    private TextView tvRight;
    private boolean isRightSelected = false;
    private OnCheckedChangeListener listener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean isRightSelected);
    }

    public CustomLanguageSwitch(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CustomLanguageSwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomLanguageSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_custom_language_switch, this, true);

        switchThumb = findViewById(R.id.switch_thumb);
        tvLeft = findViewById(R.id.tv_left_label);
        tvRight = findViewById(R.id.tv_right_label);

        setOnClickListener(v -> setChecked(!isRightSelected, true));

        tvLeft.setOnClickListener(v -> setChecked(false, true));
        tvRight.setOnClickListener(v -> setChecked(true, true));

        // Initial state
        updateUI(false);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return isRightSelected;
    }

    public void setChecked(boolean checked, boolean animate) {
        if (isRightSelected == checked)
            return;
        isRightSelected = checked;

        if (animate) {
            animateToggle(isRightSelected);
        } else {
            updateUI(isRightSelected);
            if (listener != null) {
                listener.onCheckedChanged(isRightSelected);
            }
        }
    }

    private void animateToggle(boolean isRightSelected) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        if (isRightSelected) {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.GONE, ConstraintSet.START);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        } else {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.GONE, ConstraintSet.END);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        }

        androidx.transition.AutoTransition transition = new androidx.transition.AutoTransition();
        transition.setDuration(250);
        transition.setInterpolator(new AccelerateDecelerateInterpolator());
        transition.addListener(new androidx.transition.Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull androidx.transition.Transition transition) {
            }

            @Override
            public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                if (listener != null) {
                    listener.onCheckedChanged(isRightSelected);
                }
            }

            @Override
            public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {
            }

            @Override
            public void onTransitionPause(@NonNull androidx.transition.Transition transition) {
            }

            @Override
            public void onTransitionResume(@NonNull androidx.transition.Transition transition) {
            }
        });
        TransitionManager.beginDelayedTransition(this, transition);
        constraintSet.applyTo(this);

        // Animation for colors
        int selectedColor = ContextCompat.getColor(getContext(), R.color.white);
        int unselectedColor = ContextCompat.getColor(getContext(), R.color.grey);

        if (isRightSelected) {
            tvRight.setTextColor(selectedColor);
            tvLeft.setTextColor(unselectedColor);
        } else {
            tvLeft.setTextColor(selectedColor);
            tvRight.setTextColor(unselectedColor);
        }
    }

    private void updateUI(boolean isRightSelected) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        if (isRightSelected) {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.GONE, ConstraintSet.START);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            tvRight.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            tvLeft.setTextColor(ContextCompat.getColor(getContext(), R.color.grey));
        } else {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.GONE, ConstraintSet.END);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            tvLeft.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            tvRight.setTextColor(ContextCompat.getColor(getContext(), R.color.grey));
        }
        constraintSet.applyTo(this);
    }
}
