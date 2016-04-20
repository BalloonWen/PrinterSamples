package com.sec.android.ssp.balloon.print.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;

import com.sec.android.ssp.balloon.print.R;

import java.lang.ref.WeakReference;

/**
 * Helper {@link android.preference.EditTextPreference}
 * for convenient numbers input.
 */
public final class EditTextIntegerPreference extends EditTextPreference {
    /** Entered integer value */
    private Integer mInteger;
    /** Minimum value */
    private int mMin = 1;
    /** Max value */
    private int mMax = 100000;
    /** Title string to append min and max to */
    private String mTitle = "";

    public EditTextIntegerPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public EditTextIntegerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditTextIntegerPreference(final Context context) {
        super(context);
        init();
    }

    /**
     * Sets max possible input value
     *
     * @param val int
     */
    public void setMaxVal(final int val) {
        mMax = val;
        applyLimits();
    }

    /**
     * @return max input value
     */
    public int getMaxVal() {
        return mMax;
    }

    /**
     * Sets min possible input value
     *
     * @param val int
     */
    public void setMinVal(final int val) {
        mMin = val;
        applyLimits();
    }

    /**
     * @return min input value
     */
    public int getMinVal() {
        return mMin;
    }

    /**
     * Sets limits for input ints
     *
     * @param min int
     * @param max int
     */
    public void setLimits(final int min, final int max) {
        mMin = min;
        mMax = max;
        applyLimits();
    }

    /**
     * Sets initial properties of input
     *
     */
    private void init() {
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        mTitle = (String) getDialogTitle();
        applyLimits();
    }

    /**
     * Applies min and max limits to the input:
     * it's not possible to enter smaller or bigger values.
     */
    private void applyLimits() {
        final InputFilter curFilters[] = getEditText().getFilters();
        final InputFilterMinMax limitsFilter = new InputFilterMinMax(this, mMin, mMax);

        setDialogTitle(mTitle + String.format(getContext().getString(R.string.title_format), mMin, mMax));

        if (curFilters != null) {
            // Check if we have these filter already
            for (int idx = 0; idx < curFilters.length; idx++) {
                if (curFilters[idx] instanceof InputFilterMinMax) {
                    curFilters[idx] = limitsFilter;
                    return;
                }
            }

            // there's no limitsFilter in the list, add it!
            final InputFilter newFilters[] = new InputFilter[curFilters.length + 1];

            System.arraycopy(curFilters, 0, newFilters, 0, curFilters.length);
            newFilters[curFilters.length] = limitsFilter;
            getEditText().setFilters(newFilters);
        } else {
            getEditText().setFilters(new InputFilter[] { limitsFilter });
        }
    }

    @Override
    public void setText(final String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mInteger = parseInteger(text);

        persistString(mInteger != null ? mInteger.toString() : null);

        final boolean isBlocking = shouldDisableDependents();

        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    @Override
    public String getText() {
        return mInteger != null ? mInteger.toString() : null;
    }

    private static Integer parseInteger(final String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return {@link java.lang.Integer} currently stored
     */
    public Integer getInteger() {
        return mInteger;
    }

    /**
     * Special filter for max and min values
     */
    private static class InputFilterMinMax implements InputFilter {
        private final int min;
        private final int max;

        /** Owner reference */
        private final WeakReference<EditTextPreference> mParentRef;

        /**
         * Default constructor
         *
         * @param parent {@link android.preference.EditTextPreference}
         * @param min int
         * @param max max
         */
        public InputFilterMinMax(final EditTextPreference parent, final int min, final int max) {
            this.min = min;
            this.max = max;
            mParentRef = new WeakReference<EditTextPreference>(parent);
        }

        @Override
        public CharSequence filter(final CharSequence source, final int start, final int end, final Spanned dest,
                                   final int dstart, final int dend) {
            boolean inRange = false;

            try {
                // Remove the string out of destination that is to be replaced
                String replacement = source.subSequence(start, end).toString();

                // Try convert replacement to integer to validate symbol
                try {
                    Integer.parseInt(replacement);
                } catch (NumberFormatException e) {
                    // Not int
                    replacement = "";
                }

                final String newVal = dest.subSequence(0, dstart).toString() + replacement + dest.subSequence(dend, dest.length()).toString();
                final int input = Integer.parseInt(newVal);

                inRange = isInRange(min, max, input);
            } catch (NumberFormatException ignored) { }

            final EditTextPreference parent = mParentRef.get();

            if (parent != null) {
                final Dialog dialog = parent.getDialog();

                if (dialog instanceof AlertDialog) {
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(inRange);
                }
            }

            return null;
        }

        /**
         * Helper to check if number is in range
         *
         * @return true if c is in range [a, b]
         */
        private boolean isInRange(final int a, final int b, final int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }
}