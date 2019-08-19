package com.mateus.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class ConnectionThread extends Thread {

    private BluetoothSocket bluetoothSocket = null;
    private BluetoothServerSocket bluetoothServerSocket = null;
    private InputStream input;
    private OutputStream output;
    private String bluetoothDevAddress = null;
    private static String myUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private boolean server;
    private boolean running = false;

    // Prepara o dispositivo para trabalhar como um servidor
    ConnectionThread(){
        this.server = true;
    }

    // Prepara o dispositivo para trabalhar como um cliente
    ConnectionThread(String bluetoothDevAddress){ // → Endereço MAC do dispositivo que a conexão é solicitada
        this.server = false;
        this.bluetoothDevAddress = bluetoothDevAddress;
    }

    // Nova Thread
    public void run(){
        this.running = true; // Thread sendo executada
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Dependendo se a thread esta trabalhando como cliente ou servidor, sua atuação será diferente
        if(this.server){
            // Servidor
            try{
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Bluetooth", UUID.fromString(myUUID));
                bluetoothSocket = bluetoothServerSocket.accept();

                if(bluetoothSocket != null) bluetoothServerSocket.close();

            }catch (IOException e){
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }
        }else{
            // Cliente
            Log.d("BluetoothMode", "Client");
            try{
                Log.d("BluetoothAddress", this.bluetoothDevAddress);
                bluetoothAdapter.cancelDiscovery();
                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(this.bluetoothDevAddress);
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(myUUID));

                if(bluetoothSocket != null) {
                    bluetoothSocket.connect();
                    Log.d("BluetoothConnected?", bluetoothSocket.isConnected()+"");
                }
            }catch (IOException e){
                e.printStackTrace();
                toMainActivity("---TO".getBytes());
            }
        }

        if(bluetoothSocket != null){

            toMainActivity("---S".getBytes());
            try{
                input = bluetoothSocket.getInputStream();
                output = bluetoothSocket.getOutputStream();

                byte[] buffer = new byte[1024];
                int bytes;

                while (running){
                    // Enquanto o bluetooth estiver conectado, ler mensagens vindas do bluetooth
                    // e as enviar para o Main onde essas mensagens serão tratadas.
                    bytes = input.read(buffer);
                    toMainActivity(Arrays.copyOfRange(buffer, 0, bytes));
                }

            }catch (IOException e){
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }
        }
    }

    private void toMainActivity(byte[] data) {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", data);
        message.setData(bundle);
        MainActivityBluetooth.handler.sendMessage(message);
    }

    public void write(byte[] data){

        if(output != null){
            try{
                output.write(data);
            }catch (IOException e){
                e.printStackTrace();
            }
        }else{
            toMainActivity("---N".getBytes());
        }

    }


    public void cancel(){

        try{
            running = false;
            bluetoothServerSocket.close();
            bluetoothSocket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        running = false;
    }

}
