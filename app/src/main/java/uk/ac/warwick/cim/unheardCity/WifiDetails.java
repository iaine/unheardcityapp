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
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;


public class WifiDetails {

    public Context context;

    private File fileName;

    private WifiManager wifiManager;

    private WifiRttManager wifiRttManager;

    public WifiDetails(Context context, File fName) {
        this.context = context;
        fileName = fName;
        this.initWifiDetails();
    }

    private void initWifiDetails () {
        try {
            Log.i("WIFI", "start");
            wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);

            BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    boolean success = intent.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        scanSuccess();
                    } else {
                        // scan failure handling
                        scanFailure();
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);

            boolean success = wifiManager.startScan();
            if (!success) {
                // scan failure handling
                scanFailure();
            }
        } catch (SecurityException se) {
            Log.i("WIFI", se.toString());
        }
        catch (Exception e) {
            Log.i("WIFI", e.toString());
        }

    }

    private void scanSuccess() {
        Log.i("WIFI", "scanning");
        try {
            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scan: results) {
                String details = scan.toString();
                //String data = System.currentTimeMillis() + " : WiFi; " + details + "\n";
                String data = System.currentTimeMillis()
                        + "," + scan.SSID
                        + "," + scan.capabilities
                        + ", " + scan.is80211mcResponder()
                        + ", " + scan.level
                        + "," + scan.frequency
                        + "\n";
                new FileConnection(fileName).execute(data);

            }
        }  catch (Exception e ) {
            System.out.print(e);
        }
    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        //List<ScanResult> results = wifiManager.getScanResults();
        Log.i("WIFI", "Scan wifi failed");

    }

    /**
     * Function to check if we can do RTT scanning. If so, we use the results.
     * @param results
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void scanRTT(List<ScanResult> results) {

        wifiRttManager = (WifiRttManager) this.context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            //RTT available but is it enabled? User may have changed its state.
            IntentFilter filter =
                    new IntentFilter(wifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);

            BroadcastReceiver myReceiver = new BroadcastReceiver() {
                //Suppress the Fine Location permission because we request earlier to run the app.
                @SuppressLint("MissingPermission")
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (wifiRttManager.isAvailable()) {
                        RangingRequest.Builder builder = new RangingRequest.Builder();
                        builder.addAccessPoints(results);
                        RangingRequest req = builder.build();
                        Executor executor = context.getMainExecutor();

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
                                }
                            }
                        });
                    } else {
                    }
                }
            };
            context.registerReceiver(myReceiver, filter);
        }
    }

}
