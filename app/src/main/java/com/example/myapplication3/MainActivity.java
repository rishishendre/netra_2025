package com.example.myapplication3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final UUID appUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private ImageView search;
    private ConnectedThread connectedThread;
    Button back;
    BluetoothDevice godevice;
    List<BluetoothDevice> clickdevice = new ArrayList<>();
    private ListView pairedDevicesList;
    private boolean isScanning = false; // Keeps track of scanning state
    private AcceptThread acceptThread;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };


    private final ActivityResultLauncher<Intent> bluetoothActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                    startAcceptThread();
                } else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);
        requestAllBluetoothPermissions();
        search = findViewById(R.id.imageView2);
        back = findViewById(R.id.backButton);
        if (OpenCVLoader.initDebug()) {
        }
        else{
            System.out.println("OPENCV LOADING FAILED");
        }
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(currentMode==Configuration.UI_MODE_NIGHT_YES){
            search.setImageResource(R.drawable.searchblack);
        }
        else{
            search.setImageResource(R.drawable.searchwhite);
        }
        // Initialize UI components
        pairedDevicesList = findViewById(R.id.listViewAvailable);
        search = findViewById(R.id.imageView2);
        //for dalog
        loading_dialog loading_dialog = new loading_dialog(MainActivity.this);
        // Initialize Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter bondStateFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondStateReceiver, bondStateFilter);

        // Automatically check and enable Bluetooth**
        if (bluetoothAdapter == null) {
            // Bluetooth not supported
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            finish(); // Exit the app
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is off; prompt user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothActivityResultLauncher.launch(enableBtIntent);
        } else {
            // Bluetooth is already enabled, start accepting connections
            startAcceptThread();
        }

        back.setOnClickListener(view -> { startActivity(new Intent(MainActivity.this, Get_Started.class));
        finish();});
        search.setOnClickListener(view -> {
            clickdevice.clear();
            startScanning();
        });
    }

    private void requestAllBluetoothPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Check if the activity was resumed after returning from Menu
        Intent intent = getIntent();
        if (intent.getBooleanExtra("fromMenu", false)) {
            disconnectBluetooth(); // Disconnect Bluetooth when returning from Menu
        }
    }



    private void startAcceptThread() {
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }
    public void disconnectBluetooth() {
        // Logic to disconnect the Bluetooth connection
        if (bluetoothAdapter != null && bluetoothSocket != null) {
            try {
                bluetoothSocket.close(); // Close the Bluetooth socket
                bluetoothSocket = null;
                Log.d("Bluetooth", "Bluetooth disconnected");
            } catch (IOException e) {
                Log.e("Bluetooth", "Error while disconnecting Bluetooth", e);
            }
        }
    }


    private void startScanning() {
        Log.d("DEBUGG", "startScanning: YAHA AYA");
        if (bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1001);
                return;
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if(device!=null) {
                    clickdevice.add(device);
                }
            }

            pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this, R.layout.bt_devices,R.id.device_name,clickdevice){
                @SuppressLint("MissingPermission")
                @NonNull
                @Override
                public View getView(@NonNull int position,@NonNull View convertView, @NonNull ViewGroup parent) {
                    View itemView = super.getView(position, convertView, parent);
                    // Get device name
                    String deviceName = getItem(position).getName();
                    ImageView bluetooth = itemView.findViewById(R.id.bluetooth_icon);
                    TextView nameTextView = itemView.findViewById(R.id.device_name);
                    nameTextView.setText(deviceName);
                    Button connectButton = itemView.findViewById(R.id.connect_button);
                    int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    if(currentMode==Configuration.UI_MODE_NIGHT_YES){
                        nameTextView.setTextColor(Color.parseColor("#E6E3E3"));
                        connectButton.setTextColor(Color.parseColor("#E6E3E3"));
                        bluetooth.setImageResource(R.drawable.bluetoothblack);
                    }
                    else{
                        nameTextView.setTextColor(Color.BLACK);
                        connectButton.setTextColor(Color.BLACK);
                        bluetooth.setImageResource(R.drawable.bluetoothwhite);
                    }
                    connectButton.setOnClickListener(v -> {
                        // Connect to the selected Bluetooth device (you can implement your connection logic here)
//                       connectToDevice(clickdevice.get(position));
//                       godevice = clickdevice.get(position);
                         startActivity(new Intent(MainActivity.this,Menu.class));
                    });
                    return itemView;
                }
            };
            pairedDevicesList.setAdapter(pairedDeviceAdapter);

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(bluetoothDeviceReceiver, filter);

            bluetoothAdapter.startDiscovery();
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please turn on Bluetooth first", Toast.LENGTH_SHORT).show();
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
    private final BroadcastReceiver bluetoothDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    return;
                }
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevices.add(device);
                    if(device.getName()!=null) {
                        clickdevice.add(device);
                    }
                    pairedDeviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };



    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                        return;
                    }
                    int bondState = device.getBondState();
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(context, "Paired with " + device.getName(), Toast.LENGTH_SHORT).show();
                        updatePairedDevicesList();
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        Toast.makeText(context, "Pairing with " + device.getName() + " failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };


    private void updatePairedDevicesList() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<String> pairedDeviceNames = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            if (device != null) {
                clickdevice.add(device);
            }
        }
        pairedDevicesList.setAdapter(pairedDeviceAdapter);
    }


    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Log.d("Bluetooth", "Device is null, cannot connect.");
            return;
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1002);
            return;
        }
        if(bluetoothAdapter!=null) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            loading_dialog.startloading();
            new Thread(() -> {
                try {
                    Log.d("Bluetooth", "Attempting to connect to " + device.getName());
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(appUUID);
                        bluetoothSocket.connect();
                    runOnUiThread(() -> {
                        loading_dialog.dismissloading();
                        Toast.makeText(MainActivity.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                        manageConnectedSocket(bluetoothSocket);
                        BluetoothSingleton.getInstance().notifyConnectionStatus(true);

                    });
                } catch (IOException e) {
                    Log.e("Bluetooth", "Connection failed", e);
                    try {
                        bluetoothSocket.close();
                    } catch (IOException closeException) {
                        Log.e("Bluetooth", "Failed to close socket", closeException);
                    }
                    runOnUiThread(() ->{
                        loading_dialog.dismissloading();
                        Toast.makeText(MainActivity.this, "Failed to connect to " + device.getName(), Toast.LENGTH_SHORT).show();
                });
                }
            }).start();
        }
        else{
            pairDevice(device);
        }
    }
    public void manageConnectedSocket(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        // Store the connected thread in the Singleton
        BluetoothSingleton.getInstance().setConnectedThread(connectedThread);
        BluetoothSingleton.getInstance().notifyConnectionStatus(true);

        // Navigate to Menu activity
        Intent intent = new Intent(MainActivity.this, Menu.class);
        if(godevice!=null) {
            intent.putExtra("mydeviceadd", godevice.getAddress());
        }
        startActivity(intent);
        Log.d("Bluetooth", "Navigated to Menu after connection established");
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothApp", appUUID);
                } catch (IOException e) {
                    Log.e("Bluetooth", "Socket's listen() method failed", e);
                }
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (Exception e) {
                    Log.e("Bluetooth", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    manageConnectedSocket(socket);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e("Bluetooth", "Could not close the server socket", e);
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "Could not close the server socket", e);
            }
        }
    }
    public static class ConnectedThread extends Thread {
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
                if(outStream!=null&&socket!=null&&socket.isConnected()) {
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
            } catch (IOException e) {
                Log.e("Bluetooth", "Could not close the connected socket", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (acceptThread != null) acceptThread.cancel();
        BluetoothSingleton.getInstance().notifyConnectionStatus(false);
        try {
            unregisterReceiver(bluetoothDeviceReceiver); // Unregister the device discovery receiver
            unregisterReceiver(bondStateReceiver); // Unregister the bond state receiver
        } catch (IllegalArgumentException e) {
            Log.e("Bluetooth", "Receiver not registered", e);
        }
    }

}