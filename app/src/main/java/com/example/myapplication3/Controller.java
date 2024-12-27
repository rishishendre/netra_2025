package com.example.myapplication3;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.example.joystick_lib.Joystick_Lib;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Controller extends AppCompatActivity {
    private Button  A,P, C, Q, Back,Grip,Ungrip,Reconnect;
    private TextView Status, Cammand;
    private Joystick_Lib joystickLib;
    String myadd;
    BluetoothAdapter myadapter;
    BluetoothSocket mysocket;
    UUID appUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Controller.ConnectedThread controlthread;
    int flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable fullscreen immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        // Set the layout
        setContentView(R.layout.activity_controller2);

        // Initialize buttons and other UI elements
        A = findViewById(R.id.Triangle);
        P = findViewById(R.id.Sqaure);
        C = findViewById(R.id.Cross);
        Q = findViewById(R.id.Circle);
        Back = findViewById(R.id.back);
        Status = findViewById(R.id.status);
        Cammand = findViewById(R.id.Cammand);
        Grip = findViewById(R.id.Grip);
        Ungrip = findViewById(R.id.UnGrip);
        joystickLib = findViewById(R.id.joystick_Lib);
        Reconnect = findViewById(R.id.reconnect);
        myadd = getIntent().getStringExtra("mydevice");
        Log.d("ADD BY MENU", ""+myadd);
        myadapter = BluetoothAdapter.getDefaultAdapter();
        flag=0;
        setupButtonTouchListeners(A,"A");
        setupButtonTouchListeners(P,"P");
        setupButtonTouchListeners(C,"C");
        setupButtonTouchListeners(Q,"Q");
        setupButtonTouchListeners(Grip,"G");
        setupButtonTouchListeners(Ungrip,"U");

        // Set connection status listener
        BluetoothSingleton.getInstance().setConnectionStatusListener(isConnected -> {
            runOnUiThread(() -> {
                if (isConnected) {
                    Status.setText("Connected");
                    Status.setTextColor(Color.GREEN);
                } else {
                    Status.setText("Disconnected");
                    Status.setTextColor(Color.RED);
                }
            });
        });

        // Update initial connection status
        new Thread(() -> {
            while (true) {
                if (BluetoothSingleton.getInstance().isConnected()) {
                    Status.setTextColor(Color.GREEN);
                    Status.setText("CONNECTED");
                } else {
                    Status.setTextColor(Color.RED);
                    Status.setText("DISCONNECTED");}
                try {
                    Thread.sleep(1000); // Pause for 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
        updateConnectionStatus();
        // Send "S" when the activity starts
        sendCommand("S");
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(currentMode== Configuration.UI_MODE_NIGHT_YES){
            Back.setTextColor(Color.parseColor("#E6E3E3"));
            Grip.setTextColor(Color.parseColor("#E6E3E3"));
            Ungrip.setTextColor(Color.parseColor("#E6E3E3"));
            joystickLib.setBorderColor(Color.BLACK);
            joystickLib.setButtonColor(Color.WHITE);
            Reconnect.setTextColor(Color.WHITE);
        }
        else{
            Back.setTextColor(Color.BLACK);
            Grip.setTextColor(Color.BLACK);
            Ungrip.setTextColor(Color.BLACK);
            joystickLib.setBorderColor(Color.WHITE);
            joystickLib.setButtonColor(Color.BLACK);
            Reconnect.setTextColor(Color.BLACK);
        }
        // Joystick movement handling
        Back.setOnClickListener(v -> {
            Intent intent = new Intent(Controller.this, Menu.class);
            startActivity(intent);
            finish();
        });
        Reconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BluetoothSingleton.getInstance().isConnected()) {
                    Toast.makeText(Controller.this, "DEVICE IS ALREADY CONNECTED", Toast.LENGTH_SHORT).show();
                } else {
                    if (myadd != null) {
                        flag = 1;
                        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(myadd);
                        connectToDevice(device);
                    } else {
                        Toast.makeText(Controller.this, "CANT GET ADDRESS", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
        joystickLib.setOnMoveListener((angle, strength) -> {
            String direction = getDirection(angle, strength);
            String dataToSend = direction + "*";

            // Display the command in the Cammand TextView
            Cammand.setText("" + dataToSend);
            if(flag==0) {
                BluetoothSingleton.getInstance().sendData(dataToSend);
                Log.d("Bluetooth", "Sent Joystick Data: " + dataToSend+" FLAG IS"+flag);
            }
            else{
                BluetoothSingleton.getInstance().SendData(dataToSend);
                Log.d("Bluetooth", "Sent Joystick Data: " + dataToSend+"FLAG IS "+flag);
            }
        });
    }

    private void setupButtonTouchListeners(Button button, String command) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sendCommand(command);
                    break;
                case MotionEvent.ACTION_UP:
                    sendCommand("S");
                    break;
            }
            return true;
        });
    }

    private void sendCommand(String command){
        String dataToSend = command + "*";

        // Display the command in the Cammand TextView
        Cammand.setText("" + dataToSend);
        Log.d("FLAG", ""+flag);
        if(flag==0) {
            BluetoothSingleton.getInstance().sendData(dataToSend);
        }
        else{
            BluetoothSingleton.getInstance().SendData(dataToSend);
        }
        Log.d("Bluetooth", "Sent Command: " + dataToSend);
    }

    private String getDirection(int angle, int strength) {
        if (strength != 0) {
            if (angle >= 45 && angle < 135) return "F"; // Forward
            if (angle >= 135 && angle < 225) return "L"; // Left
            if (angle >= 225 && angle < 315) return "B"; // Backward
            if ((angle >= 315 && angle < 360) || (angle >= 0 && angle < 45)) return "R"; // Right
        }
        return "S"; // Stop
    }


    private void updateConnectionStatus() {
        if (BluetoothSingleton.getInstance().isConnected()) {
            Status.setText("Connected");
        } else {
            Status.setText("Disconnected");
        }
    }
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Log.d("Bluetooth", "Device is null, cannot connect.");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1002);
            return;
        }
        if(myadapter!=null) {
            if (myadapter.isDiscovering()) {
                myadapter.cancelDiscovery();
            }
        }


        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            loading_dialog.startloading();
            new Thread(() -> {
                try {
                    if(mysocket!=null){
                        Log.d("ADJUSTING", "SOCKET"+mysocket);
                        mysocket.close();
                        mysocket = null;
                    }
                    Log.d("Bluetooth", "Attempting to connect to " + device.getName());
                    mysocket = device.createRfcommSocketToServiceRecord(appUUID);
                    mysocket.connect();
                    runOnUiThread(() -> {
                        loading_dialog.dismissloading();
                        Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                        manageConnectedSocket(mysocket);
                        BluetoothSingleton.getInstance().notifyConnectionStatus(true);

                    });
                } catch (IOException e) {
                    Log.e("Bluetooth", "Connection failed", e);
                    try {
                        mysocket.close();
                    } catch (IOException closeException) {
                        Log.e("Bluetooth", "Failed to close socket", closeException);
                    }
                    runOnUiThread(() ->{
                        loading_dialog.dismissloading();
                        Toast.makeText(this, "Failed to connect to " + device.getName(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
        else {
            pairDevice(device);
        }
    }
    public void pairDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1002);
            return;
        }

        try {
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                device.createBond(); // Initiates pairing
                Toast.makeText(this, "Pairing with " + device.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Device is already paired", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            loading_dialog. dismissloading();
            Log.e("Bluetooth", "Pairing failed", e);
            Toast.makeText(this, "Failed to pair with " + device.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    public void manageConnectedSocket(BluetoothSocket socket) {
        controlthread = new ConnectedThread(socket);
        controlthread.start();
        BluetoothSingleton.getInstance().setControllerThread(controlthread);
        BluetoothSingleton.getInstance().notifyConnectionStatus(true);
    }
    public class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                tmpOut.write(4);
            } catch (IOException e) {
                Log.e("Bluetooth", "Error occurred when creating input and output stream", e);
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d("Bluetooth", "Received: " + incomingMessage);



                } catch (IOException e) {
                    Log.d("Bluetooth", "Input stream was disconnected", e);
                    BluetoothSingleton.getInstance().notifyConnectionStatus(false);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                if(outStream!=null) {
                    outStream.write(bytes);
                }
                else{
                    Log.d("STREAM", "STREAM KA ISSUE");
                }
                Log.d("Bluetooth", "Sent: " + new String(bytes));
            } catch (IOException e) {
                Log.e("Bluetooth", "Error occurred when sending data", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
                Log.d("SOCKET TO CLOSE HO GYA", "cancel: ");
            } catch (IOException e) {
                Log.e("Bluetooth", "Socket close na ho paya", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the listener to avoid memory leaks

        BluetoothSingleton.getInstance().setConnectionStatusListener(null);
    }
}
