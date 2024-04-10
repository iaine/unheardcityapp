package uk.ac.warwick.cim.unheardCity;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

/**
 * Vibration from the device
 */
public class Haptic {
    private Vibrator vibrator;
    public Haptic (Vibrator vib) {
        vibrator = vib;
    }

    public void vibration (long vibeTime) {

        vibrator.vibrate(VibrationEffect.createOneShot(vibeTime,
                    VibrationEffect.DEFAULT_AMPLITUDE));

    }
}
