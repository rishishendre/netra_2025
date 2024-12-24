package com.example.myapplication3;

import android.util.Log;

public class BluetoothSingleton {
    private static BluetoothSingleton instance;
    private MainActivity.ConnectedThread connectedThread;
    private Controller.ConnectedThread rajkithread;
    String Command;

    private boolean isConnected = false; // Connection status field
    private ConnectionStatusListener connectionStatusListener; // Listener for status updates

    private BluetoothSingleton() {}

    public static synchronized BluetoothSingleton getInstance() {
        if (instance == null) {
            instance = new BluetoothSingleton();
        }
        return instance;
    }

    // Set the ConnectedThread and notify status changes
    public void setConnectedThread(MainActivity.ConnectedThread connectedThread) {
        this.connectedThread = connectedThread;
        boolean newStatus = (connectedThread != null);
        if (isConnected != newStatus) {
            isConnected = newStatus;
            notifyStatusChange(); // Notify listener about the status change
        }
    }
    public void setControllerThread(Controller.ConnectedThread controlThread) {
        this.rajkithread = controlThread;
        boolean newStatus = (controlThread != null);
        if (isConnected != newStatus) {
            isConnected = newStatus;
            notifyStatusChange(); // Notify listener about the status change
        }
    }
    public MainActivity.ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    // Send data if connected
    public void sendData(String data) {
        if (connectedThread != null) {
            connectedThread.write(data.getBytes());
            Log.d("Bluetooth", "DATA SENT"+data);
        } else {
            Log.d("Bluetooth", "ConnectedThread is null. Data not sent.");
        }
    }
    public void SendData(String data) {
        if (rajkithread != null) {
            rajkithread.write(data.getBytes());
            Log.d("RAJ KI THRREAD SE", "DATA SENT"+data);
        } else {
            Log.d("Bluetooth", "ConnectedThread is null. Data not sent.");
        }
    }
    // Method to check connection status
    public boolean isConnected() {
        return isConnected;
    }

    // Listener setup
    public void setConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListener = listener;
    }

    private void notifyStatusChange() {
        if (connectionStatusListener != null) {
            connectionStatusListener.onConnectionStatusChanged(isConnected);
        }
    }

    public String getCommand() {
        return Command;
    }

    public void setCommand(String command) {
        Command = command;
    }

    public void notifyConnectionStatus(boolean status) {
        isConnected = status;

        if (isConnected) {
            Log.d("BluetoothSingleton", "Device connected");
        } else {
            Log.d("BluetoothSingleton", "Device disconnected");
        }
    }

    // Interface for status listener
    public interface ConnectionStatusListener {
        void onConnectionStatusChanged(boolean isConnected);
    }
}
