package uk.ac.warwick.cim.unheardCity;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Class to write data to file asynchronously
 */

public class FileConnection extends AsyncTask<String,String,String> {
//public class FileConnection {
    private File fileName;

    protected String doInBackground(String... params) {
        this.writeFile(params[0] + "\n");
        return null;
    }

    public FileConnection (File fName) {
        fileName = fName;
    }

    public void writeFile (String params) {
        FileOutputStream outputStream;
        //@todo: check for this on load and create it.
        String filename = "signal.txt";
        try {
            //File file = new File(mContext.getExternalFilesDir(null), filename);
            File file = new File(String.valueOf(fileName));
            // test if file exists.
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file, true);
            outputStream.write(params.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Log.i("FILE", e.toString());
        }
    }
}
