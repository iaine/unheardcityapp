package uk.ac.warwick.cim.unheardCity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Build;

public class FormatData {


    @SuppressLint("MissingPermission")
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
        var data = System.currentTimeMillis()
                + "," + scan.SSID
                + "," + scan.capabilities
                + ", " + scan.is80211mcResponder()
                + ", " + scan.level
                + "," + scan.frequency
                + "\n";
        return data;
    }

    public String formatLocation (Location location) {
        String details = System.currentTimeMillis()
                    + "," + location.getLatitude()
                    + "," + location.getLongitude()
                    + "," + location.getAltitude()
                    + "," + location.getBearing()
                    + "," + location.getSpeed();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            details += "," + location.getAccuracy();
        }
        details +=  "," + location.getAccuracy() +"\n";
        return details;
    }
}
