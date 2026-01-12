package com.notificationcapture.app.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
            // Logic to handle "MercadoPago" style:
            // 1. Thousands separator: "." (German style)
            // 2. Decimal separator: ","
            // 3. User typing "." should be converted to "," if it's a decimal intent.

            String original = s;

            // Detect if the user explicitly typed a dot at the end to start decimals
            boolean endsWithDot = original.endsWith(".");
            boolean endsWithComma = original.endsWith(",");

            // Check if we already have a comma (decimal part exists)
            int commaIndex = original.indexOf(",");

            String integerPartStr = "";
            String decimalPartStr = "";

            if (commaIndex != -1) {
                // We have a comma. Split.
                // "1.234,56" or "1234,56"
                integerPartStr = original.substring(0, commaIndex);
                if (commaIndex + 1 < original.length()) {
                    decimalPartStr = original.substring(commaIndex + 1);
                }
            } else {
                // No comma.
                // Does it end with dot? If so, treat that dot as the start of decimal mode
                // (convert to comma)
                if (endsWithDot) {
                    integerPartStr = original.substring(0, original.length() - 1); // remove the dot
                    decimalPartStr = ""; // Logic will append comma below
                } else {
                    // Just an integer number, potential thousands dots inside
                    integerPartStr = original;
                }
            }

            // Clean the integer part (remove ALL dots, they are just separators)
            String cleanInteger = integerPartStr.replace(".", "");

            // Clean decimal part (remove non-digits just in case)
            String cleanDecimal = decimalPartStr.replaceAll("[^0-9]", "");

            // prevent empty or excessive generic issues
            if (cleanInteger.isEmpty())
                cleanInteger = "0";

            // Parse and Format Integer
            // Use BigDecimal or Long. Long is safer for simple typing, BigDecimal for huge
            // numbers.
            // But "05" -> "5".
            if (cleanInteger.length() > 1 && cleanInteger.startsWith("0")) {
                cleanInteger = cleanInteger.substring(1);
            }

            // Format with thousands dots
            long val = Long.parseLong(cleanInteger);
            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMANY);
            formatter.applyPattern("#,###");
            String formattedInteger = formatter.format(val);

            // Reconstruct
            StringBuilder result = new StringBuilder(formattedInteger);

            // If we had a comma OR we ended with a dot (which becomes comma)
            if (commaIndex != -1 || endsWithDot || endsWithComma) {
                result.append(",");
                result.append(cleanDecimal);
            }

            // Commit
            if (!result.toString().equals(s)) {
                editText.setText(result.toString());
                editText.setSelection(result.length()); // Simple cursor to end
            }

        } catch (NumberFormatException e) {
            // ignore
        }

        editText.addTextChangedListener(this);
    }

    public static String format(Double amount) {
        if (amount == null)
            return "0,00";
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMANY);
        formatter.applyPattern("#,###.##");
        return formatter.format(amount);
    }
}
