package uk.ac.warwick.cim.unheardCity;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Build;

import java.io.File;

public class FormatData {

    public void formatBluetoothLE () {}

    public String formatBluetooth (BluetoothDevice device) {
        String data = System.currentTimeMillis()
                + ", " + device.getAddress()
                + ", " + device.getName()
                + ", " + device.getType();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            data += ", " + device.getAlias();
        } else {
            data += ", No alias";
        }
        data +=  "\n";

        return data;
    }

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
