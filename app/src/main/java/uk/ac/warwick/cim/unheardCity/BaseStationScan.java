package uk.ac.warwick.cim.unheardCity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * Class for the Base Stations.
 * This uses TelephonyManager to get information from the masts.
 */
public class BaseStationScan implements Scan {

    protected TelephonyManager telephonyManager;

    private File fName;

    private Context context;

    private static String TAG= "TELEPHONY";

    protected BaseStationScan(Context ctx, File fileName) {
        fName = fileName;
        this.context = ctx;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        telephonyManager = (TelephonyManager) this.context.getSystemService(this.context.TELEPHONY_SERVICE);

        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }*/
        List<CellInfo> stations = telephonyManager.getAllCellInfo();
        if (stations != null) {
            for (final CellInfo station: stations) {
                String data = "";
                if (station instanceof CellInfoGsm) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        CellInfoGsm cellInfoGsm = (CellInfoGsm) station;
                        Log.i(TAG, cellInfoGsm.toString());
                        data += "GSM, ";
                        data += cellInfoGsm.getCellIdentity().getLac()  + ",";
                        data += station.getCellSignalStrength().getDbm() + ",";
                        data += "0,"; // rssi
                        data += station.getCellIdentity().getOperatorAlphaLong().toString() +", ";
                        CellIdentityGsm ciGsm = (CellIdentityGsm) station.getCellIdentity();
                        data += ciGsm.getMobileNetworkOperator().toString() + ", ";
                        data += "0 \n";
                    }
                } else if (station instanceof CellInfoLte) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        data += "LTE, ";
                        CellSignalStrengthLte cellSignalStrength = (CellSignalStrengthLte) station.getCellSignalStrength();
                        Log.i(TAG, cellSignalStrength.toString());
                        data += "0"; //lac
                        data += cellSignalStrength.getDbm(); //dbm
                        data += cellSignalStrength.getRssi(); //rssi
                        data += station.getCellIdentity().getOperatorAlphaLong().toString(); //alpha long
                        CellIdentityLte ciLte = (CellIdentityLte) station.getCellIdentity();
                        Log.i(TAG, ciLte.toString());
                        data += ciLte .getBandwidth();
                        data += ciLte.getMobileNetworkOperator();
                        int[] ibands = ciLte.getBands();
                        String bands = "";
                        for (int band: ibands) {
                            bands += String.valueOf(band) + ";";
                        }
                        data += bands + ", "; //bands
                        data += cellSignalStrength.toString() + "\n"; //identity

                    }

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (station instanceof CellInfoNr) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            data += "NR , "; //5g
                            CellIdentityNr ciNr = (CellIdentityNr) station.getCellIdentity();
                            Log.i(TAG, ciNr.toString());
                            CellSignalStrengthNr cellSignalStrength = (CellSignalStrengthNr) station.getCellSignalStrength();
                            data += cellSignalStrength.getDbm() + ",";
                            data += cellSignalStrength.getLevel() + ",";
                            data += ciNr.getOperatorAlphaLong()  + ",";
                            data += ciNr.getNrarfcn()  + ",";
                            data += ciNr.getMncString() + " " + ciNr.getMccString();
                            int[] ibands = ciNr.getBands();
                            String bands = "";
                            for (int band: ibands) {
                                bands += String.valueOf(band) + ",";
                            }
                            data += bands + ", "; //bands
                            data += cellSignalStrength.toString() + "\n"; //identity

                        }

                    }
                }
                writeData(data);
            }
        }
    }

    public void stop () {}

    private void writeData (String data) {
        try {
            new FileConnection(fName).writeFile(data);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }
}
