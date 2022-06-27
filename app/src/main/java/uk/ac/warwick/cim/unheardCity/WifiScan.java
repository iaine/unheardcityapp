package uk.ac.warwick.cim.unheardCity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.File;
import java.util.List;


public class WifiScan  implements Scan {

    protected WifiManager wifiManager;

    protected Context ctx;

    protected File wifiFile;

    protected BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
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

    protected WifiScan (Context context, WifiManager wManager, File file) {
        wifiManager = wManager;
        ctx = context;
        wifiFile = file;

    }

    public void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        ctx.registerReceiver(wifiScanReceiver, intentFilter);
    }

    public void stop() {
        ctx.unregisterReceiver(wifiScanReceiver);
    }

    public void scanSuccess(WifiManager wifiManager, File fileName) {
        Log.i("WIFI", "scanning");
        try {
            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scan: results) {

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
            Log.i("WiFi", e.getMessage());
        }
    }

    public void scanFailure() {
        // handle failure: new scan did NOT succeed
        // Provide a message
        Log.i("WIFI", "Scan wifi failed");

    }

}
