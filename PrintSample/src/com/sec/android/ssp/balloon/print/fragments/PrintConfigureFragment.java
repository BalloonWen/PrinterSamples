
package com.sec.android.ssp.balloon.print.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes.AutoFit;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes.ColorMode;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes.Duplex;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributesCaps;
import com.sec.android.ssp.balloon.print.R;

import java.util.ArrayList;

/**
 * Simple {@link android.preference.PreferenceFragment} to set Print Attributes and
 * save into preferences.
 */
public final class PrintConfigureFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    // Preferences keys for PrintAttributes
    public static final String PREF_COPIES = "pref_copies";
    public static final String PREF_COLOR_MODE = "pref_colorMode";
    public static final String PREF_DUPLEX_MODE = "pref_duplexMode";
    public static final String PREF_FILENAME = "pref_filename";
    public static final String PREF_AUTOFIT = "pref_autoFit";
    // Feedback settings
    public static final String PREF_MONITORING_JOB = "pref_monitoringJob";
    public static final String PREF_SHOW_JOB_PROGRESS = "pref_showJobProgress";
    public static final String PREF_SHOW_SETTINGS = "pref_showSettings";
    // Preference for current job id
    public static final String CURRENT_JOB_ID = "pref_currentJobId";

    private ListPreference mDuplexPref;
    private ListPreference mCMPref;
    private ListPreference mAFPref;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.print_preferences);

        mDuplexPref = (ListPreference)findPreference(PREF_DUPLEX_MODE);
        mCMPref = (ListPreference)findPreference(PREF_COLOR_MODE);
        mAFPref = (ListPreference)findPreference(PREF_AUTOFIT);

        // Set default limits to single, default, value
        ((EditTextIntegerPreference) findPreference(PREF_COPIES)).setLimits(1, 1);
        // Clear text prefs
        ((EditTextPreference) findPreference(PREF_FILENAME)).setText("");
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, PREF_COLOR_MODE);
        onSharedPreferenceChanged(prefs, PREF_DUPLEX_MODE);
        onSharedPreferenceChanged(prefs, PREF_COPIES);
        onSharedPreferenceChanged(prefs, PREF_FILENAME);
        onSharedPreferenceChanged(prefs, PREF_AUTOFIT);
        onSharedPreferenceChanged(prefs, PREF_SHOW_SETTINGS);
        onSharedPreferenceChanged(prefs, PREF_MONITORING_JOB);
        onSharedPreferenceChanged(prefs, PREF_SHOW_JOB_PROGRESS);
    }

    @Override
    public void onPause() {
        super.onPause();

        final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        final Preference preference = findPreference(key);

        if (preference instanceof ListPreference) {
            final String entry = (String) ((ListPreference) preference).getEntry();

            if (entry == null || entry.length() == 0) {
                ((ListPreference)preference).setValueIndex(0);
                preference.setSummary("%s");
            } else {
                preference.setSummary(entry);
            }
        } else if (preference instanceof EditTextIntegerPreference) {
            final Integer val = ((EditTextIntegerPreference) preference).getInteger();
            final int max = ((EditTextIntegerPreference) preference).getMaxVal();
            final int min = ((EditTextIntegerPreference) preference).getMinVal();

            // Validate stored value and correct if needed
            if (val == null || val > max || val < min) {
                ((EditTextIntegerPreference) preference).setText(String.valueOf(min));
            }

            preference.setSummary(((EditTextIntegerPreference) preference).getText());
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(((EditTextPreference) preference).getText());
        } else if (preference instanceof CheckBoxPreference) {
            if (PREF_MONITORING_JOB.equals(key)) {
                findPreference(PREF_SHOW_JOB_PROGRESS)
                        .setEnabled(((CheckBoxPreference) preference).isChecked());
            }
        }
    }

    /**
     * Fills preferences with received capabilities.
     *
     * @param caps {@link com.sec.android.ngen.common.lib.ssp.printer.PrintAttributesCaps}
     */
    public void loadCapabilities(final PrintAttributesCaps caps) {
        final ArrayList<CharSequence> duplexEntries = new ArrayList<CharSequence>();
        final ArrayList<CharSequence> duplexEntryValues = new ArrayList<CharSequence>();

        // Load duplex
        for (Duplex duplex: caps.getDuplexList()) {
            duplexEntries.add(duplex.name());
            duplexEntryValues.add(duplex.name());
        }

        mDuplexPref.setEntries(duplexEntries.toArray(new CharSequence[duplexEntries.size()]));
        mDuplexPref.setEntryValues(duplexEntryValues.toArray(new CharSequence[duplexEntryValues.size()]));
        mDuplexPref.setDefaultValue(Duplex.DEFAULT.name());
        mDuplexPref.setValueIndex(0);
        mDuplexPref.setSummary("%s");

        // Load color mode
        final ArrayList<CharSequence> cmEntries = new ArrayList<CharSequence>();
        final ArrayList<CharSequence> cmEntryValues = new ArrayList<CharSequence>();

        for (final ColorMode cm: caps.getColorModeList()) {
            cmEntries.add(cm.name());
            cmEntryValues.add(cm.name());
        }

        mCMPref.setEntries(cmEntries.toArray(new CharSequence[cmEntries.size()]));
        mCMPref.setEntryValues(cmEntryValues.toArray(new CharSequence[cmEntryValues.size()]));
        mCMPref.setDefaultValue(ColorMode.DEFAULT.name());
        mCMPref.setValueIndex(0);
        mCMPref.setSummary("%s");

        // Load AutoFit
        final ArrayList<CharSequence> afEntries = new ArrayList<CharSequence>();
        final ArrayList<CharSequence> afEntryValues = new ArrayList<CharSequence>();

        for (final AutoFit af: caps.getAutoFitList()) {
            afEntries.add(af.name());
            afEntryValues.add(af.name());
        }

        mAFPref.setEntries(afEntries.toArray(new CharSequence[afEntries.size()]));
        mAFPref.setEntryValues(afEntryValues.toArray(new CharSequence[afEntryValues.size()]));
        mAFPref.setDefaultValue(AutoFit.DEFAULT.name());
        mAFPref.setValueIndex(0);
        mAFPref.setSummary("%s");

        // Apply Copies limits
        if (caps.getMaxCopies() > 0) {
            ((EditTextIntegerPreference) findPreference(PREF_COPIES)).setLimits(1, caps.getMaxCopies());
        }
    }
}
