package uk.ac.warwick.cim.unheardCity;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.File;
import java.util.List;


public class WifiDetails {

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
