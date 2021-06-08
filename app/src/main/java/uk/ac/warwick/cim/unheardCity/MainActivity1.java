package uk.ac.warwick.cim.unheardCity;
//https://github.com/android/location-samples/blob/432d3b72b8c058f220416958b444274ddd186abd/LocationUpdatesForegroundService/app/src/main/java/com/google/android/gms/location/sample/locationupdatesforegroundservice/MainActivity.java
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity1 extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 101;
    private static final int PERMISSION_CODE = 102;

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;

    private FusedLocationProviderClient fusedLocationClient;

    private double wayLatitude = 0.0, wayLongitude = 0.0;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private boolean isContinue = false;

    private LocationUpdatesService mService = new LocationUpdatesService();

    // Tracks the bound state of the service.
    private boolean mBound = false;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener((SharedPreferences.OnSharedPreferenceChangeListener) this);

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //setContentView(new TrackerView(this, null));

        // check for Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity1.this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERMISSION_CODE);
            }
        }

        //check the Bluetooth LE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = null;
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //check the permissions are granted
        if (ContextCompat.checkSelfPermission(MainActivity1.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity1.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(MainActivity1.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity1.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }

        //new BluetoothLEDetails();
        //new WifiDetails(this);

        mService.requestLocationUpdates();
        //mapping
        //Uses https://github.com/droidbyme/Location/blob/master/app/src/main/java/com/coders/location/MainActivity.java
        /*fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.i("Location", location.toString());
                }
            }
        };
        //find a way of looping this.
        getLocation();*/

    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity1.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity1.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    //AppConstants.LOCATION_REQUEST);

        } else {
            if (isContinue) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity1.this, location -> {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();
                        //Toast.makeText(this, String.format( "%s - %s", wayLatitude, wayLongitude)).show();
                        Log.i("Location", String.format( "%s - %s", wayLatitude, wayLongitude));
                    } else {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults){
        switch (requestCode){
            case 1: {
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(MainActivity1.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    /**
            * Receiver for broadcasts sent by {@link LocationUpdatesService}.
            */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                Toast.makeText(MainActivity1.this, location.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}