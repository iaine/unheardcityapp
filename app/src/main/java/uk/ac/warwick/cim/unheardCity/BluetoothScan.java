package uk.ac.warwick.cim.unheardCity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.io.File;

/**
 * Bluetooth scanner.
 *
 * Runs every 5 seconds in a runnable process when stopped and started
 * through the UI.
 */
public class BluetoothScan implements Scan {

    protected Context ctx;

    private BroadcastReceiver receiver;

    private final String TAG = "BluetoothScan";

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final int timeInterval = 5000;

    private final Handler handler = new Handler();

    private Runnable blueScanRunner;

    private final File fName;

    private final FormatData formatData = new FormatData();

    protected BluetoothScan(Context context, File file) {
        //ctx = context;
        ctx = signalCityApplication.getAppContext();
        fName = file;
    }

    public void start() {

        blueScanRunner = new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.startDiscovery();
                handler.postDelayed(this, timeInterval);
            }
        };
        handler.postDelayed(blueScanRunner, timeInterval);

        // Create a BroadcastReceiver for ACTION_FOUND.
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String data = formatData.formatBluetooth(device);
                    writeData(fName, data);
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        ctx.registerReceiver(receiver, filter);
    }

    public void stop() {
        bluetoothAdapter.cancelDiscovery();
        ctx.unregisterReceiver(receiver);
        handler.removeCallbacks(blueScanRunner);
    }



    public void writeData (File fName, String data) {
        try {
            new FileConnection(fName).writeFile(data);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }



}
