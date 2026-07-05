package com.notificationcapture.app.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Parcel;
import android.os.Parcelable;
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

public class CustomTypeSwitch extends ConstraintLayout {

    private static class SavedState extends BaseSavedState {
        boolean isEgreso;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isEgreso = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (isEgreso ? 1 : 0));
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private View switchTrack;
    private View switchThumb;
    private TextView tvIngreso;
    private TextView tvEgreso;
    private boolean isEgreso = false;
    private OnCheckedChangeListener listener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean isEgreso);
    }

    public CustomTypeSwitch(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CustomTypeSwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomTypeSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_custom_type_switch, this, true);

        switchTrack = findViewById(R.id.switch_track);
        switchThumb = findViewById(R.id.switch_thumb);
        tvIngreso = findViewById(R.id.tv_ingreso_label);
        tvEgreso = findViewById(R.id.tv_egreso_label);

        setOnClickListener(v -> setChecked(!isEgreso, true));

        // Let labels also be clickable to change state
        tvIngreso.setOnClickListener(v -> setChecked(false, true));
        tvEgreso.setOnClickListener(v -> setChecked(true, true));

        // Initial state
        updateUI(false);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return isEgreso;
    }

    public void setChecked(boolean checked, boolean animate) {
        setChecked(checked, animate, true);
    }

    public void setChecked(boolean checked, boolean animate, boolean notifyListener) {
        if (isEgreso == checked)
            return;
        isEgreso = checked;

        if (animate) {
            animateToggle(isEgreso);
        } else {
            updateUI(isEgreso);
        }

        if (notifyListener && listener != null) {
            listener.onCheckedChanged(isEgreso);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.isEgreso = isEgreso;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.isEgreso, false, false);
    }

    private void animateToggle(boolean isEgreso) {
        // Animation for the thumb position using ConstraintSet and TransitionManager
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        if (isEgreso) {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.GONE, ConstraintSet.START);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        } else {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.GONE, ConstraintSet.END);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        }

        androidx.transition.AutoTransition transition = new androidx.transition.AutoTransition();
        transition.setDuration(250);
        transition.setInterpolator(new AccelerateDecelerateInterpolator());
        TransitionManager.beginDelayedTransition(this, transition);
        constraintSet.applyTo(this);

        // Animation for colors
        int colorFrom = isEgreso ? ContextCompat.getColor(getContext(), R.color.green)
                : ContextCompat.getColor(getContext(), R.color.red);
        int colorTo = isEgreso ? ContextCompat.getColor(getContext(), R.color.red)
                : ContextCompat.getColor(getContext(), R.color.green);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(250);
        colorAnimation.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            switchThumb.setBackgroundTintList(ColorStateList.valueOf(color));
            int unselectedColor = ContextCompat.getColor(getContext(), R.color.grey);
            int whiteColor = ContextCompat.getColor(getContext(), R.color.white);
            if (isEgreso) {
                tvEgreso.setTextColor(whiteColor);
                tvIngreso.setTextColor(unselectedColor);
            } else {
                tvIngreso.setTextColor(whiteColor);
                tvEgreso.setTextColor(unselectedColor);
            }
        });
        colorAnimation.start();
    }

    private void updateUI(boolean isEgreso) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        int unselectedColor = ContextCompat.getColor(getContext(), R.color.grey);
        int whiteColor = ContextCompat.getColor(getContext(), R.color.white);

        if (isEgreso) {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.GONE, ConstraintSet.START);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

            int red = ContextCompat.getColor(getContext(), R.color.red);
            switchThumb.setBackgroundTintList(ColorStateList.valueOf(red));
            tvEgreso.setTextColor(whiteColor);
            tvIngreso.setTextColor(unselectedColor);
        } else {
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.END, ConstraintSet.GONE, ConstraintSet.END);
            constraintSet.connect(R.id.switch_thumb, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);

            int green = ContextCompat.getColor(getContext(), R.color.green);
            switchThumb.setBackgroundTintList(ColorStateList.valueOf(green));
            tvIngreso.setTextColor(whiteColor);
            tvEgreso.setTextColor(unselectedColor);
        }
        constraintSet.applyTo(this);
    }
}
