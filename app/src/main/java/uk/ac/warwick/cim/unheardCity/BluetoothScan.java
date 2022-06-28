package uk.ac.warwick.cim.unheardCity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.File;

public class BluetoothScan implements Scan {

    protected Context ctx;

    private BroadcastReceiver receiver;

    private String TAG = "BluetoothScan";

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private int timeInterval = 5000;

    private Handler handler = new Handler();

    private Runnable blueScanRunner;

    private File fName;

    protected BluetoothScan(Context context, File file) {
        ctx = context;
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
                Log.i(TAG, "action is " + action);
                Log.i(TAG, "bluetooth action is " + BluetoothDevice.ACTION_FOUND);
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String data = System.currentTimeMillis()
                            + ", " + device.getAddress()
                            + ", " + device.getName()
                            + ", " + device.getType();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        data += ", " + device.getAlias();
                    }
                    data +=  "\n";
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
            Log.i("BLUETOOTH", e.toString());
        }
    }



}
