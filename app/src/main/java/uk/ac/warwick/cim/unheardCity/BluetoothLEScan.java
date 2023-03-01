package uk.ac.warwick.cim.unheardCity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.content.Context;
import android.util.SparseArray;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * BluetoothLE scanning. Data is written to file.
 *
 * Runs every 5 seconds in a runnable process when stopped and started
 * through the UI. Runs for 2.5 seconds.
 *
 */
public class BluetoothLEScan implements Scan {

    private static final String TAG = "BLUETOOTH";

    private static final int REQUEST_ENABLE_BT = 104;

    private final File fName;

    private Context mContext;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeScanner bluetoothLeScanner;

    private boolean scanning;

    private final Handler handler = new Handler();

    private Runnable bleScanRun;

    // Stops scanning after 2.5 seconds.
    private static final long SCAN_PERIOD = 2500;

    private final int timeInterval = 5000;

    public BluetoothLEScan(File fileName) {
        Log.i(TAG, "In Bluetooth");
        fName = fileName;
    }

    public void stop() { this.stopScan();}

    public void start() {
        //final BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(BluetoothManager.class);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) mContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bleScanRun = new Runnable() {
            @Override
            public void run() {
                scanLeDevice();
                if (scanning) handler.postDelayed(this, timeInterval);
                handler.postDelayed(this, timeInterval);
            }
        };
        handler.postDelayed(bleScanRun, timeInterval);

    }

    private void stopScan() {
        scanning = false;
        bluetoothLeScanner.stopScan(leScanCallback);
        handler.removeCallbacks(bleScanRun);
    }

    private void scanLeDevice() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

            if(bluetoothLeScanner != null) {
                if (!scanning) {
                    // Stops scanning after a pre-defined scan period.
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scanning = false;
                            bluetoothLeScanner.stopScan(leScanCallback);
                        }
                    }, SCAN_PERIOD);

                    scanning = true;
                    bluetoothLeScanner.startScan(leScanCallback);
                } else {
                    this.stopScan();
                }
            }
        }

    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {

                    super.onScanResult(callbackType, result);

                    ScanRecord details = result.getScanRecord();
                    String m = mf(details.getManufacturerSpecificData());

                    String serviceUid = "No service";
                    String serviceData = "No data";

                    List<ParcelUuid> uids = serviceID(details);

                    if (uids != null) {

                        StringBuilder ids = new StringBuilder();
                        for (ParcelUuid pUid: uids) {
                            serviceUid = pUid.getUuid().toString();
                            serviceData = sfd(pUid, details);

                            ids.append(pUid.getUuid().toString() + ",");
                            Log.i(TAG, "UUID " + serviceUid + " file " + pUid.describeContents());
                        }
                        serviceUid = ids.toString();
                    }
                    Log.i(TAG, "Service " +  serviceData);
                    Log.i(TAG, "manufacturer: " + getManufacturerData(details.getManufacturerSpecificData(), details));
                    String data = System.currentTimeMillis()
                            + ", " + result.getDevice()
                            + ", " + result.getRssi()
                            + ", " + result.getPrimaryPhy()
                            + ", " + result.getSecondaryPhy()
                            + ", " + result.getPeriodicAdvertisingInterval()
                            + ", " + details.getDeviceName()
                            + ", " + details.getManufacturerSpecificData().toString()
                            + ", " + details.getTxPowerLevel()
                            + ", " + details.getAdvertiseFlags()
                            + ", " + m
                            + ", " + mfd(m, details )
                            + ", " + serviceUid
                            + ", " + serviceData
                            + "\n";
                    writeData(data);
                    System.out.println(details.toString());
                }
            };

    private void writeData (String data) {
        try {
            new FileConnection(fName).writeFile(data);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    private List<ParcelUuid> serviceID (ScanRecord r) {
        List<ParcelUuid> uids =  r.getServiceUuids();
        return uids;
    }

    private String sfd(ParcelUuid uuid, ScanRecord r) {
        byte[] serviceData = r.getServiceData(uuid);
        if (serviceData != null) {
            String str = new String();
            return str.valueOf(byteArrayToHex(serviceData).toCharArray());
            //return ByteArrayToString(serviceData);
        }

        return "No device";
    }

    private String mf (SparseArray<byte[]> manufacturer) {
        SparseArray<byte[]> manufacturerData = manufacturer;

        final StringBuilder stringBuilder = new StringBuilder(manufacturerData.size() * 2);
        for(int i = 0; i < manufacturerData .size(); i++){
            stringBuilder.append(manufacturerData.keyAt((char) i));
        }

        return stringBuilder.toString();
    }

    private String mfd (String advData, ScanRecord r) {
        String str1 = "";
        if (advData != "") {
            try {
                byte[] specificData = r.getManufacturerSpecificData(Integer.valueOf(advData));
                str1 = new String(specificData, StandardCharsets.UTF_16);
            }catch (Exception e) {
                Log.i(TAG, "String " + e.getMessage());
            }
        }
        return str1;
    }

    //https://stackoverflow.com/questions/45044076/how-to-get-raw-ble-manufacturer-specific-data-on-android
    private String getManufacturerData(SparseArray<byte[]> manufacturer, ScanRecord r) {

        SparseArray<byte[]> manufacturerData = manufacturer;

        final StringBuilder stringBuilder = new StringBuilder(manufacturerData.size() * 2);
        for(int i = 0; i < manufacturerData.size(); i++){
            stringBuilder.append(manufacturerData.keyAt((char) i));
        }

        String advData = stringBuilder.toString();

        if (advData != "") {

            byte[] specificData = r.getManufacturerSpecificData(Integer.valueOf(advData));

            String s = convertByteToHexadecimal(specificData);

            StringBuilder output = new StringBuilder(specificData.length * 2);
            for (int i = 0; i < s.length(); i+=2) {
                String str = s.substring(i, i+2);
                output.append((char)Integer.parseInt(str, 16));
            }
            Log.i(TAG, "Conversion :" + output);

            //convert bytes to hex
            String bytesToHex = byteArrayToHex(specificData);
            char[] hexToChar = bytesToHex.toCharArray();
            System.out.println("---------------");
            System.out.println(hexToChar);
            System.out.println("---------------");
            System.out.println(hexToASCII(bytesToHex));
            System.out.println("---------------");
            System.out.println(ByteArrayToString(specificData));
            System.out.println("---------------");
        }

        return advData;
    }

    public String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append((char)b + " ");

        return hex.toString();
    }

    public static String convertByteToHexadecimal(byte[] byteArray)
    {
        String hex = "";

        // Iterating through each byte in the array
        for (byte i : byteArray) {
            hex += String.format("%02X", i);
        }

        System.out.print(hex);
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < hex.length(); i+=2) {
            str.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        System.out.println(str);
        return hex;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String hexToASCII(String hexValue)
    {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexValue.length(); i += 2)
        {
            String str = hexValue.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    //https://stackoverflow.com/questions/47887029/bluetooth-low-energy-ble-how-to-get-uuids-of-service-characteristic-and-des

    /**
     * Get the characteristics and descriptors of the services.
     * @param bluetoothGatt
     */
    private void defineCharAndDescrUUIDs(BluetoothGatt bluetoothGatt)
    {
        List<BluetoothGattService> servicesList = bluetoothGatt.getServices();

        for (int i = 0; i < servicesList.size(); i++)
        {
            BluetoothGattService bluetoothGattService = servicesList.get(i);

            //if (serviceUUIDsList.contains(bluetoothGattService.getUuid()))
            //{
                List<BluetoothGattCharacteristic> bluetoothGattCharacteristicList = bluetoothGattService.getCharacteristics();

                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattCharacteristicList)
                {
                    //characteristicUUIDsList.add(bluetoothGattCharacteristic.getUuid());
                    List<BluetoothGattDescriptor> bluetoothGattDescriptorsList = bluetoothGattCharacteristic.getDescriptors();

                    for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattDescriptorsList)
                    {
                        //descriptorUUIDsList.add(bluetoothGattDescriptor.getUuid());
                        Log.i(TAG, bluetoothGattDescriptor.getUuid().toString());
                    }
                }
            //}
        }
    }
}
