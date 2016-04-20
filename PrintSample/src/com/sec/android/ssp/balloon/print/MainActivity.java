package com.sec.android.ssp.balloon.print;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.sec.android.ngen.common.lib.ssp.CapabilitiesExceededException;
import com.sec.android.ngen.common.lib.ssp.DeviceNotReadyException;
import com.sec.android.ngen.common.lib.ssp.Result;
import com.sec.android.ngen.common.lib.ssp.Ssp;
import com.sec.android.ngen.common.lib.ssp.job.JobService;
import com.sec.android.ngen.common.lib.ssp.job.JobletAttributes;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes.AutoFit;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes.ColorMode;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributes.Duplex;
import com.sec.android.ngen.common.lib.ssp.printer.PrintAttributesCaps;
import com.sec.android.ngen.common.lib.ssp.printer.PrinterService;
import com.sec.android.ngen.common.lib.ssp.printer.Printlet;
import com.sec.android.ngen.common.lib.ssp.printer.PrintletAttributes;
import com.sec.android.ngen.common.lib.ssp.printer.util.PrintUtil;
import com.sec.android.ssp.balloon.print.fragments.InitializationErrorDialogFragment;
import com.sec.android.ssp.balloon.print.fragments.PrintConfigureFragment;

import net.xoaframework.ws.v1.CreatePrintJobStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Main activity for Print Sample.
 * The activity shows the following interactions:<br/>
 * <ol>
 * <li>How to initialize Smart UX SDK</li>
 * <li>How to get the print service to get attribute capability details</li>
 * <li>How to launch print job on MFP</li>
 * </ol>
 */
public final class MainActivity extends AppCompatActivity {


    public static final String ACTION_PRINT_COMPLETED = "com.sec.android.ssp.sample.print.ACTION_PRINT_COMPLETED";

    private static final String ERROR_DIALOG_FRAGMENT = "errorDialogFragment";

    /** Code for printer selection launch */
    private static final int PRINTER_SELECTION_CODE = 1;

    private static final String FILE_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";

    private SharedPreferences mPrefs = null;
    private PrintObserver mObserver = null;

    /** Smart UX SDK initialization errors dialog */
    private InitializationErrorDialogFragment mDialog;

    /** Fragment to display attributes configuration UI */
    private PrintConfigureFragment mFragment = null;

/**
 * merging
 * */
    private static final int GET_PERMIT_REQUEST = 1;
    private static final String DOWNLOAD_FILE_SERVLET = "http://samprinter.cloudapp.net/TestOAuthServer/servlet/DownloadFileServlet";
    //    private static final String DRIVE_ID = "DriveId:CAESHDBCN2FLQXZXVkpRS3Rja2w2U1hGd0xWQjJTV3MYECCcjsvkzFMoAA==";
    //    private static final String RESOURCE_ID="0B7aKAvWVJQKtckl6SXFwLVB2SWs";
//    private static String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
    //    private static final String RECEIVE_MESSAGE_SERVLET = "http://135.23.64.27:8080/TestOAuthServer/servlet/ReceiveMessageServlet";
//    private static final String RECEIVE_MESSAGE_SERVLET = "http://192.168.139.128:8080/TestOAuthServer/servlet/ReceiveMessageServlet";
    private static final String RECEIVE_MESSAGE_SERVLET = "http://samprinter.cloudapp.net/TestOAuthServer/servlet/ReceiveMsgFromServerServlet";
    private static Queue<String> files2StoreList = null;
    private static Queue<String> files2Print = null;
    private static final String TAG = "PrinterListener";
    Button btnStartListening;
    Button btnStopListening;
    TextView txtInformation;
    Thread receiveMsgThread = new ReceiveMessageThread();
    Thread getFileFromServerThread = new GetFileFromServerThread();
    Thread printJobListener = new PrintJobListener();


    /**
     * Observer for Print result and progress.
     */
    private class PrintObserver extends PrinterService.AbstractPrintletObserver {

        private int mJobId = 0;
        private String mRid = "";

        public PrintObserver(final Handler handler) {
            super(handler);
        }

        public void setRid(final String rid) {
            if (rid != null) {
                mRid = rid;
            } else {
                mRid = "";
            }
        }

        @Override
        public void onCancel(final String rid) {
            if (!mRid.equals(rid)) {
                Log.w(TAG, "onProgress: expected rid:" + mRid + " received:" + rid);
                return;
            }

            Log.d(TAG, "Received Print Cancel");
            showToast("Print cancelled!");
        }

        @Override
        public void onComplete(final String rid, final Bundle bundle) {
            if (!mRid.equals(rid)) {
                Log.w(TAG, "onProgress: expected rid:" + mRid + " received:" + rid);
                return;
            }

            Log.d(TAG, "Received Print Complete");
            showToast("Print completed!");

            Log.d(TAG, "onComplete: with data \n" +
                    "  KEY_IMAGE_COUNT: " + bundle.getInt(Printlet.Keys.KEY_IMAGE_COUNT, 0) + "\n" +
                    "  KEY_SET_COUNT: " + bundle.getInt(Printlet.Keys.KEY_SET_COUNT, 0) + "\n" +
                    "  KEY_SHEET_COUNT: " + bundle.getInt(Printlet.Keys.KEY_SHEET_COUNT, 0));
        }

        @Override
        public void onFail(final String rid, final Result result) {
            if (!mRid.equals(rid)) {
                Log.w(TAG, "onProgress: expected rid:" + mRid + " received:" + rid);
                return;
            }

            Log.e(TAG, "Received Print Fail, Result " + result.mCode);
            showToast("Print failed! " + result);

            if (result.mCode == Result.RESULT_WS_FAILURE) {
                final Result.WSCause cause = Result.getWSCause(result);

                if (cause != null) {
                    Log.w(TAG, cause.toString());
                } else {
                    Log.w(TAG, "Failed without any cause");
                }
            } else if (result.mCode == Result.RESULT_WS_EX_STATUS) {
                final List<CreatePrintJobStatus> ls = PrintUtil.getPrintJobStatus(result);

                if (ls != null) {
                    for (CreatePrintJobStatus status : ls) {
                        if (status != null) {
                            Log.w(TAG, status.toString());
                        }
                    }
                }
            }
        }

        @Override
        public void onProgress(final String rid, final Bundle bundle) {
            if (!mRid.equals(rid)) {
                Log.w(TAG, "onProgress: expected rid:" + mRid + " received:" + rid);
                return;
            }

            if (bundle.containsKey(Printlet.Keys.KEY_JOBID)) {
                mJobId = bundle.getInt(Printlet.Keys.KEY_JOBID);
                Log.d(TAG, "onProgress: Received jobID as " + mJobId);
                showToast("Job ID is " + mJobId);

                if (mPrefs.getBoolean(PrintConfigureFragment.PREF_MONITORING_JOB, false)) {
                    final Intent intent = new Intent(getApplicationContext(), JobCompleteReceiver.class);

                    intent.setAction(ACTION_PRINT_COMPLETED);
                    Log.d(TAG, "MonitorJob " + mJobId);
                    // Store Job Id in order to verify it in the Broadcast Receiver
                    mPrefs.edit().putInt(PrintConfigureFragment.CURRENT_JOB_ID, mJobId).apply();

                    final boolean showProgress =
                            mPrefs.getBoolean(PrintConfigureFragment.PREF_SHOW_JOB_PROGRESS, true);

                    // Monitor the job completion
                    final JobletAttributes taskAttributes =
                            new JobletAttributes.Builder().setShowUi(showProgress).build();
                    final String jrid = JobService.monitorJobInForeground(MainActivity.this, mJobId,
                            taskAttributes, intent);

                    Log.d(TAG, "MonitorJob request: " + jrid);
                }

                Log.d(TAG, "onProgress: with data \n" +
                        "  KEY_IMAGE_COUNT: " + bundle.getInt(Printlet.Keys.KEY_IMAGE_COUNT, 0) + "\n" +
                        "  KEY_SET_COUNT: " + bundle.getInt(Printlet.Keys.KEY_SET_COUNT, 0) + "\n" +
                        "  KEY_SHEET_COUNT: " + bundle.getInt(Printlet.Keys.KEY_SHEET_COUNT, 0));
            }
        }
    }

    /**
     * Shows toast
     *
     * @param text {@link String} toasts text
     */
    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Async task to request printers capabilities and launch Print.
     */
    private static final class PrintAsyncTask extends AsyncTask<Void, Void, Void> {
        /** Observer to be used as callback */
        private final WeakReference<PrintObserver> mObserver;
        /** Application Context */
        private final Context mContext;
        /** Preferences to obtain Print Settings */
        private final SharedPreferences mPrefs;
        /** Error Message string to provide to the user */
        private String mErrorMsg = null;

        PrintAsyncTask(final Context context, final PrintObserver observer) {
            mObserver = new WeakReference<PrintObserver>(observer);
            mContext = context;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final PrintAttributesCaps caps = requestCaps(mContext);
            final Resources res = mContext.getResources();

            if (null == caps) {
                mErrorMsg = "Not able to obtain printers capabilities";
                return null;
            }

            try {
                final Duplex duplex = Duplex.valueOf(
                        mPrefs.getString(PrintConfigureFragment.PREF_DUPLEX_MODE,
                                Duplex.DEFAULT.name()));
                Log.i(TAG, "Selected Duplex:" + duplex.name());

                final ColorMode cm = ColorMode.valueOf(
                        mPrefs.getString(PrintConfigureFragment.PREF_COLOR_MODE,
                                         ColorMode.DEFAULT.name()));
                Log.i(TAG, "Selected Color Mode:" + cm.name());

                final String saf = mPrefs.getString(PrintConfigureFragment.PREF_AUTOFIT,
                        AutoFit.DEFAULT.name());
                final AutoFit af = AutoFit.valueOf(saf);
                Log.i(TAG, "Selected af: " + saf);

                final int copies = Integer.valueOf(
                        mPrefs.getString(PrintConfigureFragment.PREF_COPIES, "1"));

//                String filePath = mPrefs.getString(PrintConfigureFragment.PREF_FILENAME, "");
                String filePath = files2Print.peek();
                files2Print.remove();
                if (!TextUtils.isEmpty(filePath)) {
                    filePath = FILE_PATH + filePath;
                }
                Log.i(TAG, "Selected path: " + filePath);

                // Build PrintAttributes based on preferences values
                final PrintAttributes attributes =
                        new PrintAttributes.PrintFromStorageBuilder(Uri.fromFile(new File(filePath)))
                        .setColorMode(cm)
                        .setDuplex(duplex)
                        .setAutoFit(af)
                        .setCopies(copies)
                        .build(caps);

                // Clean stored job id if any
                if (mObserver.get() != null) {
                    // Reset old job id
                    mObserver.get().mJobId = 0;
                }

                final PrintletAttributes taskAttribs = new PrintletAttributes.Builder()
                        .setShowSettingsUi(mPrefs.getBoolean(PrintConfigureFragment.PREF_SHOW_SETTINGS, false))
                        .build();

                // Submit the job
                final String rid = PrinterService.submit(mContext, attributes, taskAttribs);

                // Clean stored job id if any
                if (mObserver.get() != null) {
                    // Reset old job id
                    mObserver.get().setRid(rid);
                }
            } catch (final CapabilitiesExceededException e) {
                Log.e(TAG, "Caps were exceeded: ", e);
                mErrorMsg = "CapabilitiesExceededException: " + e.getMessage();
            } catch (final IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument was provided: ", e);
                mErrorMsg = e.getMessage();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Void aVoid) {
            super.onPostExecute(aVoid);

            if (mErrorMsg != null) {
                Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Async task to request printers capabilities and launch Print.
     */
    private static final class LoadCapabilitiesAsyncTask extends AsyncTask<Void, Void, Void> {
        /** Application Context */
        private final Context mContext;
        /** Error Message string to provide to the user */
        private String mErrorMsg = null;

        private PrintAttributesCaps mCaps = null;
        private PrintConfigureFragment mFragment = null;

        LoadCapabilitiesAsyncTask(final Context context, PrintConfigureFragment fragment) {
            mContext = context;
            mFragment = fragment;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mCaps = requestCaps(mContext);

            if (null == mCaps) {
                mErrorMsg = "Not able to obtain printers capabilities";
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Void aVoid) {
            super.onPostExecute(aVoid);

            if (mErrorMsg != null) {
                Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_SHORT).show();
            } else {
                mFragment.loadCapabilities(mCaps);
                Toast.makeText(mContext, R.string.loaded_caps, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mObserver = new PrintObserver(new Handler());
        files2StoreList = new LinkedList<>();
        files2Print = new LinkedList<>();



                receiveMsgThread.start();
                getFileFromServerThread.start();
                printJobListener.start();


        // Set listener for Print execution
        findViewById(R.id.printButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                executePrint();
            }
        });

        // Set listener for Load
        findViewById(R.id.loadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                loadCapabilities();
            }
        });
    }

    /**
     * Exception in could be because of following reasons
     * <ol>
     * <li>Library is not installed</li>
     * <li>Library update is needed</li>
     * <li>Version issue, unsupported</li>
     * <li> Permission is not granted properly</li>
     * </ol>
     * 
     * @param e {@link java.lang.Exception}
     */
    private void handleInitException(final Exception e) {
        mDialog = InitializationErrorDialogFragment.newInstance(getApplicationContext(), e);
        mDialog.show(getSupportFragmentManager(), ERROR_DIALOG_FRAGMENT);
    }

    /**
     * Launches capabilities loading async task
     */
    private void loadCapabilities() {
        // Pass application context
        new LoadCapabilitiesAsyncTask(getApplicationContext(), mFragment).execute();
    }

    /**
     * Launches Print job
     */
    private void executePrint() {
        // Check if SDK installed
        try {
            Ssp.getInstance().initialize(getApplicationContext());
        } catch (SsdkUnsupportedException e) {
            Log.e(TAG, "SDK is not supported!", e);
            Toast.makeText(getApplicationContext(), "Printer SDK is not installed! Finishing...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Pass application context
        new PrintAsyncTask(getApplicationContext(), mObserver).execute();
    }

    /**
     * Requests printer Print capabilities
     *
     * @param context {@link android.content.Context} to obtain data
     *
     * @return {@link com.sec.android.ngen.common.lib.ssp.printer.PrintAttributesCaps}
     */
    private static PrintAttributesCaps requestCaps(final Context context) {
        final Result result = new Result();
        final PrintAttributesCaps caps = PrinterService.getCapabilities(context, result);

        if (caps != null) {
            Log.d(TAG, "Received Caps as:" +
                    "AutoFit: " + caps.getAutoFitList() +
                    ", ColorMode: " + caps.getColorModeList() +
                    ", Max Copies: " + caps.getMaxCopies() +
                    ", Duplex: " + caps.getDuplexList());
        }

        return caps;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mObserver.register(getApplicationContext());

        try {
            // initialize the SSP with app context
            Ssp.getInstance().initialize(getApplicationContext());
        } catch (final SsdkUnsupportedException e) {
            Log.e(TAG, "SDK is not supported!", e);
            handleInitException(e);
            return;
        } catch (final SecurityException e) {
            Log.e(TAG, "Security exception!", e);
            handleInitException(e);
            return;
        } catch (final DeviceNotReadyException e) {
            Log.e(TAG, "DeviceNotReadyException exception", e);
            handleInitException(e);
            return;
        }

        // Check if Print is supported
        if (!PrinterService.isSupported(getApplicationContext())) {
            mDialog = InitializationErrorDialogFragment.newInstance(getString(R.string.print_not_supported));
            mDialog.show(getSupportFragmentManager(), ERROR_DIALOG_FRAGMENT);
        }

        mFragment = (PrintConfigureFragment)getFragmentManager().findFragmentById(R.id.data_container);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // remove error dialog on Pause
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }

        mObserver.unregister(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (!Ssp.Platform.IS_PRINTER_DEVICE) {
            menu.add(Menu.NONE, R.id.select_printer, 0, R.string.select_printer)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item != null && item.getItemId() == R.id.select_printer) {
            Ssp.Printer.openSelectionActivity(this, PRINTER_SELECTION_CODE, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == PRINTER_SELECTION_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    Log.d(TAG, "Printer connected");
                    break;

                default:
                    Log.d(TAG, "Printer connection was cancelled / failed");
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    private class PrintJobListener extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    if (!files2Print.isEmpty()) {
                        executePrint();
//                        files2Print.remove();
                    }
                    Thread.sleep(2500);
                }
            } catch (Exception e) {

            }
        }
    }

    private class GetFileFromServerThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {

                    if (!files2StoreList.isEmpty()) {
                        JSONObject json = new JSONObject(files2StoreList.peek());
                            String fileName = json.getString("fileName");
                            URL url = new URL(DOWNLOAD_FILE_SERVLET);
                            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                            httpConn.setUseCaches(false);
                            httpConn.setDoOutput(true);
                            httpConn.setRequestMethod("POST");
                            httpConn.setDoInput(true);
                            httpConn.setRequestProperty("fileName", fileName);
                            httpConn.getOutputStream();

                            File file = new File(FILE_PATH + fileName);
                            InputStream inputStream = httpConn.getInputStream();
                            OutputStream outputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int len = -1;
                            while ((len = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                        }
                        inputStream.close();
                        outputStream.close();
                        files2Print.add(fileName);
                        files2StoreList.remove();
                    } else {
                        Thread.sleep(2500);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

        }
    }

    private class ReceiveMessageThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {

                    URL url = new URL(RECEIVE_MESSAGE_SERVLET);
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

                    httpConn.setUseCaches(false);
                    httpConn.setDoOutput(true);
                    httpConn.setRequestMethod("POST");
                    httpConn.setDoInput(true);

                    InputStream inputStream = httpConn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

                    String jsonString = br.readLine();
                    Log.i(TAG, jsonString);
                    Log.i(TAG, files2StoreList.size() + "");
                    JSONArray jsonArray = new JSONArray(jsonString);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        files2StoreList.add(jsonArray.getString(i));

                    }
                    sleep(1500);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
