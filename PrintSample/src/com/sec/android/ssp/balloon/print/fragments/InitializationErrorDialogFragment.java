package com.sec.android.ssp.balloon.print.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.sec.android.ssp.balloon.print.R;

/**
 * Fragment to show error related to incorrect Smart UX SDK initialization,
 * which can be caused by missed
 */
public final class InitializationErrorDialogFragment extends AppCompatDialogFragment {
    private static final String TAG = "InitErrorDialog";
    private static final String SDK_ERROR_TEXT_ARG = "ErrorText";

    /**
     * Create a new instance of InitializationErrorDialogFragment.
     *
     * @param context {@link Context} to access resources
     * @param e {@link Exception} to use as argument
     *
     * @return created DialogFragment
     */
    public static InitializationErrorDialogFragment newInstance(final Context context, final Exception e) {
        InitializationErrorDialogFragment f = new InitializationErrorDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();

        if (e instanceof SsdkUnsupportedException) {
            switch (((SsdkUnsupportedException) e).getType()) {
                case SsdkUnsupportedException.LIBRARY_NOT_INSTALLED:
                case SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED:
                    args.putString(SDK_ERROR_TEXT_ARG, context.getString(R.string.sdk_support_missing));
                    break;

                default:
                    args.putString(SDK_ERROR_TEXT_ARG,
                            context.getString(R.string.unknown_error) + ((SsdkUnsupportedException) e).getType());
                    break;
            }
        } else {
            args.putString(SDK_ERROR_TEXT_ARG, e.getMessage());
        }

        f.setArguments(args);

        f.setCancelable(false);
        return f;
    }

    /**
     * Create a new instance of InitializationErrorDialogFragment.
     *
     * @param errorStr {@link String} with init error string
     *
     * @return created DialogFragment
     */
    public static InitializationErrorDialogFragment newInstance(final String errorStr) {
        InitializationErrorDialogFragment f = new InitializationErrorDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();

        args.putString(SDK_ERROR_TEXT_ARG, errorStr);
        f.setArguments(args);

        f.setCancelable(false);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (args.containsKey(SDK_ERROR_TEXT_ARG)) {
            final String errorText = args.getString(SDK_ERROR_TEXT_ARG, "Not able to execute operation");

            builder.setMessage(errorText);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    getActivity().finish();
                }
            });
        }

        return builder.create();
    }
}
