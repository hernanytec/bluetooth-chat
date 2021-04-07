package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatController {
    private static final String APP_NAME = "BluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("85293a76-ff18-4797-b07c-987201c2285e");

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private AcceptThread serverThread;
    private ClientThread clientThread;
    private ReadWriteThread connectedThread;
    private int status;

    static final int STATE_NONE = 0;
    static final int STATE_LISTEN = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;

    public ChatController(Context context, Handler handler) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        status = STATE_NONE;

        this.handler = handler;
    }

    // controla o texto que diz o status da conexão no topo da tela do app
    private synchronized void setStatus(int status) {
        this.status = status;

        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, status, -1).sendToTarget();
    }

    // pega o status atual da conexão
    public synchronized int getStatus() {
        return status;
    }

    // ionicia o serviço
    public synchronized void start() {
        // Cancela as threads
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        // Cancela as threads que estão rodando
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // atualiza o status e inicia uma thread em estado de aceitação
        setStatus(STATE_LISTEN);
        if (serverThread == null) {
            serverThread = new AcceptThread();
            serverThread.start();
        }
    }

    // inicia uma conexão com um dispositivo
    public synchronized void connect(BluetoothDevice device) {
        if (status == STATE_CONNECTING) {
            if (clientThread != null) {
                clientThread.cancel();
                clientThread = null;
            }
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // inicia uma thread para conectar com um dispositivo específico
        clientThread = new ClientThread(device);
        clientThread.start();
        setStatus(STATE_CONNECTING);
    }

    // gerencia a conexão do bluetooth
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        //  Inicia uma thread para gerenciar a conexão e executar a transmissão
        connectedThread = new ReadWriteThread(socket);
        connectedThread.start();

        // Envia o nome do dispositivo conectado pra interface de usuário
        Message msg = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_OBJECT);
        Bundle bundle = new Bundle();
        bundle.putParcelable(MainActivity.DEVICE_OBJECT, device);
        msg.setData(bundle);
        handler.sendMessage(msg);

        setStatus(STATE_CONNECTED);
    }

    // para/cancela todas as threads
    public synchronized void stop() {
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }
        setStatus(STATE_NONE);
    }

    public void write(byte[] out) {
        ReadWriteThread r;
        synchronized (this) {
            if (status != STATE_CONNECTED)
                return;
            r = connectedThread;
        }
        r.write(out);
    }

    private void connectionFailed() {
        Message msg = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Não é possível conectar ao dispositivo.");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Reinicia o serviço para o modo de escuta/listening
        com.example.bluetoothchat.ChatController.this.start();
    }

    private void connectionLost() {
        Message msg = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "A conexão com o dispositivo foi perdida.");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Reinicia o serviço para o modo de escuta/listening
        com.example.bluetoothchat.ChatController.this.start();
    }

    // roda enquanto escuta entradas de conexão
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            serverSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket;
            while (status != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // Se alguma conexão está como aceita
                if (socket != null) {
                    synchronized (com.example.bluetoothchat.ChatController.this) {
                        switch (status) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // inicia uma thread de conexão
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Se não ta pronto pra conectar ou já está conectado, termina o socket
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }
        }
        // cancela o socket
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    // tentar tomar a ação de conectar a alguem
    private class ClientThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ClientThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            // cancela a descoberta pra não deixar muito lento
            bluetoothAdapter.cancelDiscovery();

            // faz a conecção pelo socket
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // Ja estanco conectado reseta a thread de ação
            synchronized (com.example.bluetoothchat.ChatController.this) {
                clientThread = null;
            }

            // inicia a thread com a conexão
            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    // runs during a connection with a remote device
    private class ReadWriteThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream entrada;
        private final OutputStream saida;

        public ReadWriteThread(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
            InputStream tmpEntrada = null;
            OutputStream tmpSaida = null;

            try {
                tmpEntrada = socket.getInputStream();
                tmpSaida = socket.getOutputStream();
            } catch (IOException e) {
            }

            entrada = tmpEntrada;
            saida = tmpSaida;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // mantem sempre olhando a entrada
            while (true) {
                try {
                    // lê a entrada
                    bytes = entrada.read(buffer);

                    // envia a entrada pra ser exibida na interface de usuario
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    // Se algo der errado reinicia o chat
                    com.example.bluetoothchat.ChatController.this.start();
                    break;
                }
            }
        }

        // escreve no fluxo de saida
        public void write(byte[] buffer) {
            try {
                saida.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
