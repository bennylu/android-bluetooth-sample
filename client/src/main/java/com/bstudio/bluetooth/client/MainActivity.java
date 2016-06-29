package com.bstudio.bluetooth.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "BT";

    public static final String SERVER_UUID = "00000000-0000-1000-8000-00805F9B34FB";

    private static final int REQUEST_CODE_ENABLE_BT = 5566;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;

    private ArrayList<BluetoothDevice> mPairedDevices;
    private ArrayList<BluetoothDevice> mUnpairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(mListAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                // stop discovering
                mBluetoothAdapter.cancelDiscovery();

                connectAndHandle((BluetoothDevice) mListAdapter.getItem(i));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (prepareBluetooth()) {
            findDevices();
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        closeSocket();
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

        return true;
    }

    private void findDevices() {
        Log.d(TAG, "find BT devices...");

        // query paired devices
        mPairedDevices = getPairedDevices();
        mListAdapter.notifyDataSetChanged();

        // discover devices
        Log.d(TAG, "start discovering devices");
        mUnpairedDevices = new ArrayList<>();
        mBluetoothAdapter.startDiscovery();
    }

    private ArrayList<BluetoothDevice> getPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            return null;
        }

        // dump them
        Log.d(TAG, "dump paired devices");
        for (BluetoothDevice device : pairedDevices) {
            Log.d(TAG, " " + device.getName() + ":" + device.getAddress());
        }

        return new ArrayList<>(pairedDevices);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "onReceive: " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // check if this device is paired already
                if (mPairedDevices != null && mPairedDevices.size() > 0) {
                    for (BluetoothDevice pairedDevice : mPairedDevices) {
                        if (pairedDevice.getAddress().equals(device.getAddress())) {
                            return;
                        }
                    }
                }

                mUnpairedDevices.add(device);
                Log.d(TAG, " found " + device.getName() + ":" + device.getAddress());
                mListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void closeSocket() {
        if (mSocket != null) {
            Log.d(TAG, "close socket");
            try {
                mSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "exception", e);
            }
        }
    }

    private void connectAndHandle(final BluetoothDevice device) {
        Log.d(TAG, "connect to " + device.getName() + " (" + device.getAddress() + ")...");

        new Thread() {
            public void run() {
                closeSocket();

                try {
                    mSocket = device
                            .createRfcommSocketToServiceRecord(UUID.fromString(SERVER_UUID));
                } catch (IOException e) {
                    Log.e(TAG, "exception", e);
                    return;
                }

                try {
                    mSocket.connect();
                } catch (IOException e) {
                    Log.e(TAG, "exception", e);
                    try {
                        mSocket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "exception", e2);
                    }
                    return;
                }

                Log.d(TAG, "connected");

                DataOutputStream dos = null;
                try {
                    dos = new DataOutputStream(mSocket.getOutputStream());
                    dos.writeInt(5566);
                    dos.flush();
                } catch (Exception e) {
                    Log.e(TAG, "exception", e);
                    return;
                }
            }
        }.start();
    }

    private BaseAdapter mListAdapter = new BaseAdapter() {

        private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

        @Override
        public void notifyDataSetChanged() {
            mDevices.clear();

            if (mPairedDevices != null) {
                mDevices.addAll(mPairedDevices);
            }

            if (mUnpairedDevices != null) {
                mDevices.addAll(mUnpairedDevices);
            }

            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public BluetoothDevice getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater()
                        .inflate(android.R.layout.simple_list_item_1, viewGroup, false);
            }

            BluetoothDevice device = getItem(i);

            ((TextView) view.findViewById(android.R.id.text1))
                    .setText(device.getName() + " (" + device.getAddress() + ")");

            return view;
        }
    };

}