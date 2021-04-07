package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;

public class BluetoothFactory {

    private static BluetoothAdapter bluetoothAdapter;

    public static BluetoothAdapter getBluetootAdapter() {
        if ( bluetoothAdapter == null ) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }
}