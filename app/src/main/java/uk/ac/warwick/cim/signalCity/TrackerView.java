package uk.ac.warwick.cim.signalCity;

import android.view.View;
import android.content.Context;

import android.util.AttributeSet;

class TrackerView extends View {
    public TrackerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {

        try {
            System.out.println("In view");
            //new LocationDetails(context);
            //new WifiDetails(context);

            //new BluetoothLEDetails();
            //new WifiDetails(context);
        } catch (Exception e) {

        }
    }


}


