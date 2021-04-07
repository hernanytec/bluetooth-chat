package com.example.bluetoothchat;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.CountDownTimer;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private Button btnConnect;
    private ListView listView;
    private Dialog dialog;
    private EditText inputLayout;
    private BluetoothAdapter bluetoothAdapter;

    public static final int BLUETOOTH_SOLICITATION = 1;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewsByIds();

        // Verifica se o dispositivo suporta bluetooth ou não
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Adaptador bluetooth não está funcionando!", Toast.LENGTH_LONG).show();
            finish();
        }

        // Solicita que o usuário ativa o bluetooth
        else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_SOLICITATION);
        }

        // Exibe dialog com a lista dos dispositivos
        btnConnect.setOnClickListener(view -> showDevicesDialog());
    }

    private void showDevicesDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Dispositivos");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();

        // Inicializando adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        // Recuperando listviews
        ListView pairedDeviceList = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView discoveredDeviceList = (ListView) dialog.findViewById(R.id.discoveredDeviceList);

        // Definindo adapters
        pairedDeviceList.setAdapter(pairedDevicesAdapter);
        discoveredDeviceList.setAdapter(discoveredDevicesAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // Adiciona os dispositivos pareados adapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add("Nenhum dispositivo pareado");
        }

        // Definindo ação de click em um item da lista
        pairedDeviceList.setOnItemClickListener((parent, view, position, id) -> {
            bluetoothAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            String macAddress = info.substring(info.length() - 17);

            connectToDevice(macAddress);
            dialog.dismiss();
        });

        discoveredDeviceList.setOnItemClickListener((adapterView, view, i, l) -> {
            bluetoothAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            connectToDevice(address);
            dialog.dismiss();
        });


        dialog.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        dialog.setCancelable(false);
        dialog.show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_SOLICITATION) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth ativado!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Bluethooth nao foi ativado, o app será terminado em 3 segundos.", Toast.LENGTH_LONG).show();

                new CountDownTimer(3000, 1000) {
                    public void onFinish() {
                        finish();
                    }
                    public void onTick(long millisUntilFinished) {
                        // A_cada_1_segundo_faça_nada
                    }
                }.start();
            }
        }
    }

    private void connectToDevice(String macAddress) {
        bluetoothAdapter.cancelDiscovery();
        // TODO: iniciar conexão
    }

    private void findViewsByIds() {
        status = (TextView) findViewById(R.id.status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        listView = (ListView) findViewById(R.id.list);
        inputLayout = (EditText) findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(view -> {
            if (inputLayout.getText().toString().equals("")) {
                Toast.makeText(MainActivity.this, "Digite algum texto", Toast.LENGTH_SHORT).show();
            } else {
                //TODO: enviar mensagem
                //TODO: limpar campo de texto
            }
        });
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add("Nenhum dispositivo encontrado");
                }
            }
        }
    };
}
