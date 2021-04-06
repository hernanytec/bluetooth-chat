package com.example.bluetoothchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;

import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter = null;
    private static final int BLUETOOTH_SOLICITATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.main_toolbar_title);
        setSupportActionBar(toolbar);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Adaptador bluetooth não está funcionando!", Toast.LENGTH_LONG).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_SOLICITATION);
        }

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

    public void onFind(View v){
        System.out.println("aqui");
    }
}
