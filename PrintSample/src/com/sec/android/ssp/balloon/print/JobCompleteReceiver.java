package com.sec.android.ssp.balloon.print;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.sec.android.ngen.common.lib.ssp.job.Joblet;
import com.sec.android.ssp.balloon.print.fragments.PrintConfigureFragment;

/**
 * Simple Broadcast receiver to observe job completion intent
 */
public final class JobCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "JobCompleteReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        final ComponentName component = intent.getComponent();
        // Verify that received Job Id is same as expected one
        final int jobId = intent.getIntExtra(Joblet.Keys.KEY_JOBID, 0);
        final int expectedJobId = PreferenceManager.getDefaultSharedPreferences(context).getInt(PrintConfigureFragment.CURRENT_JOB_ID, -1);

        if (MainActivity.ACTION_PRINT_COMPLETED.equals(action) &&
                component != null && context.getPackageName().equals(component.getPackageName()) &&
                jobId == expectedJobId) {
            Log.d(TAG, "Received pending intent");
            Toast.makeText(context, R.string.received_complete_intent, Toast.LENGTH_LONG).show();
        }
    }
}
