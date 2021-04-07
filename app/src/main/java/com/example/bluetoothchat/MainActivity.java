package com.example.bluetoothchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter = null;
    private static final int BLUETOOTH_SOLICITATION = 1;
    public static int SELECT_PAIRED_DEVICE = 2;
    public static int SELECT_DISCOVERED_DEVICE = 3;
    private static final int RECUPERA_DISPOSITIVO = 0;

    Button listarPareados;
    int timeLeft = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.main_toolbar_title);
        setSupportActionBar(toolbar);

        bluetoothAdapter = BluetoothFactory.getBluetootAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Adaptador bluetooth não está funcionando!", Toast.LENGTH_LONG).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_SOLICITATION);
        }

        listarPareados = (Button) findViewById(R.id.button_listar);

        listarPareados.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chamaIntent();
            }
        });
    }


    public void chamaIntent(){
        Intent i = new
                Intent(this, EscolherDispositivo.class);
        startActivity(i);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_SOLICITATION) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth ativado!", Toast.LENGTH_LONG).show();
            } else {

                new CountDownTimer(6000, 2000) {
                    public void onFinish() {
                        finish();
                    }

                    public void onTick(long millisUntilFinished) {
                        Toast.makeText(getApplicationContext(), "Bluethooth não foi ativado, o app será terminado em " + timeLeft--, Toast.LENGTH_SHORT).show();
                    }
                }.start();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
