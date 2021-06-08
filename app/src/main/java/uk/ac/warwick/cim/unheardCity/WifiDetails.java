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


public class WifiDetails {

    public Context context;

    private File fileName;

    private WifiManager wifiManager;

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

}
