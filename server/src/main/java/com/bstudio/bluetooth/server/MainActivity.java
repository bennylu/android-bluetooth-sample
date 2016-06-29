package com.bstudio.bluetooth.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.bstudio.bluetooth.R;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "BT";

    public static final String SERVER_UUID = "00000000-0000-1000-8000-00805F9B34FB";

    private static final int REQUEST_CODE_ENABLE_BT = 5566;
    private static final int REQUEST_CODE_ENABLE_BT_DISCOVERABLE = 5567;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothServerSocket mServerSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (prepareBluetooth()) {
            createServerSocket();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        closeServerSocket();
    }

    private boolean prepareBluetooth() {
        Log.d(TAG, "prepare BT...");

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "BT is not supported");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "BT is not enabled: Launch BT settings");
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_CODE_ENABLE_BT);
            return false;
        }

        // make this device always discoverable
        Log.d(TAG, "make BT discoverable");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivityForResult(discoverableIntent, REQUEST_CODE_ENABLE_BT_DISCOVERABLE);

        return true;
    }

    private void createServerSocket() {
        Log.d(TAG, "create server socket");

        new Thread() {
            public void run() {
                try {
                    mServerSocket = mBluetoothAdapter
                            .listenUsingRfcommWithServiceRecord("SquareX Time",
                                    UUID.fromString(SERVER_UUID));
                } catch (IOException e) {
                    Log.e(TAG, "exception", e);
                    return;
                }

                while (true) {
                    Log.d(TAG, "waiting for incoming request...");

                    BluetoothSocket socket = null;
                    try {
                        socket = mServerSocket.accept();
                    } catch (IOException e) {
                        Log.e(TAG, "exception", e);
                        break;
                    }

                    handleSocket(socket);
                }
            }
        }.start();
    }

    private void closeServerSocket() {
        Log.d(TAG, "close server socket");

        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (Exception e) {
                Log.e(TAG, "exception", e);
            }
        }
    }

    private void handleSocket(final BluetoothSocket socket) {
        Log.d(TAG, "handle socket...");

        new Thread() {
            public void run() {
                DataInputStream dis = null;
                try {
                    dis = new DataInputStream(socket.getInputStream());
                    int number = dis.readInt();
                    Log.d(TAG, "read from client=" + number);
                } catch (Exception e) {
                    Log.e(TAG, "exception", e);
                    return;
                }
            }
        }.start();
    }

}