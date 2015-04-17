package com.salvadorhm.arduinouno;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inputStream =null;
    private Timer timer;
    private TextView txtVValue;

    private static final String TAG = "Arduino ONE";

    // Well known SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "30:14:12:19:24:55";
    Switch swtLed1;
    Switch swtViewData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swtLed1 = (Switch)findViewById(R.id.swtLed1);
        swtViewData = (Switch)findViewById(R.id.swtViewData);
        txtVValue =(TextView)findViewById(R.id.txtVValue);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

    }

    public void showData(String value){
        txtVValue.setText(value);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickSwitch(View view) {
        if(swtLed1.isChecked()){
            sendData("1");
        }else{
            sendData("0");
        }

    }

    public void onClickSwitchViewData(View view) {
        if(swtViewData.isChecked()){
            swtLed1.setEnabled(false);
            sendData("2");//activa el envio de datos
            timer = new Timer();
            final Runnable setImageRunnable = new Runnable() {
                public void run() {
                    try {
                        byte[] buffer = new byte[256];
                        int bytes;

                        bytes= inputStream.read(buffer);
                        String readData = new String(buffer,0,bytes);
                        Log.d("datos", "valor: " + readData);
                        showData(readData);
                    } catch (IOException e) {
                        Log.d("ERROR InputStream",e.getLocalizedMessage());
                    }
                    catch (Exception e) {
                        Log.d("ERROR InputStream",e.getLocalizedMessage());
                    }
                }
            };

            TimerTask tt = new TimerTask(){
                    public void run() {
                       runOnUiThread(setImageRunnable);
                    }
                };

            timer.schedule(tt,0, 1000);
        }else{
            sendData("3");//desactiva el envio de datos
            timer.cancel();
            swtLed1.setEnabled(true);
        }

    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "...In onResume - Attempting client connect...");
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
        btAdapter.cancelDiscovery();
        Log.d(TAG, "...Connecting to Remote...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }
        Log.d(TAG, "...Creating Socket...");

        try {
            outStream = btSocket.getOutputStream();
            inputStream= btSocket.getInputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth is enabled...");
            } else {
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast msg = Toast.makeText(getBaseContext(),
                title + " - " + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();
        Log.d(TAG, "...Sending data: " + message + "...");
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();

            if (address.equals("00:00:00:00:00:00"))
            msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }
}
