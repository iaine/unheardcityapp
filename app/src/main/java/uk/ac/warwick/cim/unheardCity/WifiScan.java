package uk.ac.warwick.cim.unheardCity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.FractionRes;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;


public class WifiScan  implements Scan {

    protected WifiManager wifiManager;

    protected Context ctx;

    protected File wifiFile;

    private Runnable wifiScanRunner;

    private BroadcastReceiver wifiScanReceiver;

    private BroadcastReceiver wifiRangeReceiver;

    private WifiRttManager wifiRttManager;

    private Handler handler = new Handler();

    //OS throttles to 4 scans in 2 minutes so run at 30 seconds.
    private final static int timeInterval = 30000;

    private FormatData formatData = new FormatData();

    private final static String TAG = "WiFiCScan";

    protected WifiScan (Context context, WifiManager wManager, File file) {
        wifiManager = wManager;
        ctx = context;
        wifiFile = file;
    }

    public void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess(wifiManager, wifiFile);
                } else {
                    scanFailure();
                }
            }
        };

        ctx.registerReceiver(wifiScanReceiver, intentFilter);

        wifiScanRunner = new Runnable() {
            @Override
            public void run() {
                boolean success = wifiManager.startScan();
                if (!success) {
                    // scan failure handling
                    Log.i(TAG, "Could not scan");
                }
                handler.postDelayed(this, timeInterval);
            }
        };
        handler.postDelayed(wifiScanRunner, timeInterval);
    }

    public void stop() {
        ctx.unregisterReceiver(wifiScanReceiver);
        handler.removeCallbacks(wifiScanRunner);
    }

    public void scanSuccess(WifiManager wifiManager, File fileName) {
        Log.i("WIFI", "scanning");
        try {
            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scan: results) {
                String data = formatData.formatWifi(scan);
                new FileConnection(fileName).execute(data);

            }
        }  catch (Exception e ) {
            Log.i("WiFi", e.getMessage());
        }
    }

    public void scanFailure() {
        // handle failure: new scan did NOT succeed
        // Provide a message
        Log.i("WIFI", "Scan wifi failed");
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void startWiFiRanging() {
        Log.i(TAG, "WiFi Ranging ON");

        wifiRttManager = (WifiRttManager) ctx.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        if (ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            //RTT available but is it enabled? User may have changed its state.
            IntentFilter filter = new IntentFilter(wifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);

            wifiRangeReceiver = new BroadcastReceiver() {
                //Suppress the Fine Location permission because we request earlier to run the app.
                @SuppressLint("MissingPermission")
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (wifiRttManager.isAvailable()) {
                        List<ScanResult> results = wifiManager.getScanResults();
                        RangingRequest.Builder builder = new RangingRequest.Builder();
                        builder.addAccessPoints(results);
                        RangingRequest req = builder.build();
                        Executor executor = ctx.getMainExecutor();

                        wifiRttManager.startRanging(req, executor, new RangingResultCallback() {

                            @Override
                            public void onRangingFailure(int code) {
                                Log.i("WiFiRTT", "Ranging failure " + code);
                            }

                            @Override
                            public void onRangingResults(List<RangingResult> results) {
                                for (RangingResult result: results) {
                                    //We have data, let's get some details
                                    result.getDistanceMm();
                                    result.getStatus();
                                }
                            }
                        });
                    } else {
                        Log.i("WiFiRTT", "Ranging not available");
                    }
                }
            };
            ctx.registerReceiver(wifiRangeReceiver, filter);
        }
    }

    private void stopWiFiRanging () {
        Log.i(TAG, "WiFi Ranging OFF");
        ctx.unregisterReceiver(wifiRangeReceiver);
    }

}
