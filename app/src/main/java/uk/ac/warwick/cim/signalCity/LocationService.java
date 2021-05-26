package uk.ac.warwick.cim.signalCity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.tasks.OnSuccessListener;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;


//@todo: optimise the location data. May want to run this app in the foreground
// background will limit both it and the wifi
public class LocationService extends Service {

    private static String TAG = "LOCATION";

    private boolean requestingLocationUpdates;

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    //this.getLastLocation();
    private FusedLocationProviderClient fusedLocationClient;

    private Context context;

    public LocationService(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        //this.getLastLocation();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.i(TAG, location.toString());
                }
            }
        };
        //this.startLocationUpdates();
    }

    public void getLocations(Activity activity) {
        startLocationUpdates(activity);
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation(Activity activity) {
        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }*/
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {

                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                Log.i(TAG, location.toString());
                            }
                        }
                    });
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(Activity activity) {
        System.out.println("Starting updates");

        //check for the relevant permissions
        System.out.println("permissions");

        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }*/
        System.out.println("Last location " + fusedLocationClient.getLastLocation().toString());
        //we have the permissions, now to poll for location updates
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    protected void onPause() {
        //super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    protected void onResume() {
        //super.onResume();
        if (requestingLocationUpdates) {
            //startLocationUpdates();
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
