package com.notificationcapture.app.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;

public class MoneyTextWatcher implements TextWatcher {
    private final WeakReference<EditText> editTextWeakReference;

    public MoneyTextWatcher(EditText editText) {
        this.editTextWeakReference = new WeakReference<>(editText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        EditText editText = editTextWeakReference.get();
        if (editText == null)
            return;

        String s = editable.toString();
        if (s.isEmpty())
            return;

        editText.removeTextChangedListener(this);

        try {
            String original = s;

            boolean endsWithDot = original.endsWith(".");
            boolean endsWithComma = original.endsWith(",");

            int commaIndex = original.indexOf(",");

            String integerPartStr = "";
            String decimalPartStr = "";

            if (commaIndex != -1) {
                integerPartStr = original.substring(0, commaIndex);
                if (commaIndex + 1 < original.length()) {
                    decimalPartStr = original.substring(commaIndex + 1);
                }
            } else {
                if (endsWithDot) {
                    integerPartStr = original.substring(0, original.length() - 1);
                    decimalPartStr = "";
                } else {
                    integerPartStr = original;
                }
            }

            String cleanInteger = integerPartStr.replace(".", "");

            String cleanDecimal = decimalPartStr.replaceAll("[^0-9]", "");

            if (cleanInteger.isEmpty())
                cleanInteger = "0";

            if (cleanInteger.length() > 1 && cleanInteger.startsWith("0")) {
                cleanInteger = cleanInteger.substring(1);
            }

            long val = Long.parseLong(cleanInteger);
            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMANY);
            formatter.applyPattern("#,###");
            String formattedInteger = formatter.format(val);

            StringBuilder result = new StringBuilder(formattedInteger);

            if (commaIndex != -1 || endsWithDot || endsWithComma) {
                result.append(",");
                result.append(cleanDecimal);
            }

            if (!result.toString().equals(s)) {
                editText.setText(result.toString());
                editText.setSelection(result.length());
            }

        } catch (NumberFormatException e) {
            // ignore
        }

        editText.addTextChangedListener(this);
    }

    public static String format(BigDecimal amount) {
        if (amount == null)
            return "0,00";
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMANY);
        formatter.applyPattern("#,###.##");
        return formatter.format(amount);
    }

    @Nullable
    public static BigDecimal parse(String formattedAmount) {
        if (formattedAmount == null || formattedAmount.trim().isEmpty()) {
            return null;
        }
        try {
            String clean = formattedAmount.replace(".", "").replace(",", ".");
            BigDecimal value = new BigDecimal(clean);
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}