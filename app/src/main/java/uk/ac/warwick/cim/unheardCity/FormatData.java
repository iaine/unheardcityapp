package uk.ac.warwick.cim.unheardCity;

import android.location.Location;
import android.net.wifi.ScanResult;

import java.io.File;

public class FormatData {

    public void formatBluetoothLE () {}

    public String formatBluetooth () {}

    public String formatWifi (ScanResult scan) {
        String data = System.currentTimeMillis()
                + "," + scan.SSID
                + "," + scan.capabilities
                + ", " + scan.is80211mcResponder()
                + ", " + scan.level
                + "," + scan.frequency
                + "\n";
        return data;
    }

    public String formatLocation (Location location) {
        String details = "";
        details = System.currentTimeMillis()
                + "," + location.getLatitude()
                + "," + location.getLongitude()
                + "," + location.getAltitude()
                + "," + location.getBearing()
                + "," + location.getSpeed()
                + "," + location.getVerticalAccuracyMeters()
                + "," + location.getAccuracy()
                + "\n";
        return details;
    }
}
