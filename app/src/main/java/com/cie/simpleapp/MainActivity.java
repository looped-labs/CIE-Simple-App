package com.cie.simpleapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cie.btp.BtpConsts;
import com.cie.btp.CieBluetoothPrinter;


public class MainActivity extends Activity {

    private TextView statusMsg;
    private Button btnPrint;

    public static CieBluetoothPrinter mPrinter = CieBluetoothPrinter.INSTANCE;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        statusMsg = (TextView) findViewById(R.id.status_msg);

        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        try {
            mPrinter.initService(MainActivity.this, mMessenger);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        final Button btnSelectPrinter = (Button) findViewById(R.id.btnSelectPrinter);
        btnSelectPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrinter.showDeviceList(MainActivity.this);
            }
        });

        final Button btnClearPrinter = (Button) findViewById(R.id.btnClearPrinter);
        btnClearPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrinter.clearPreferredPrinter();
            }
        });

        btnPrint = (Button) findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncPrint().execute();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPrinter.onActivityResult(requestCode, resultCode, this);
    }

    @Override
    protected void onResume() {
        mPrinter.onActivityResume();
        super.onResume();
    }


    @Override
    protected void onPause() {
        mPrinter.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mPrinter.disconnectFromPrinter();
        mPrinter.onActivityDestroy();
        super.onDestroy();
    }

    final Messenger mMessenger = new Messenger(new PrintSrvMsgHandler());
    private String mConnectedDeviceName = "";

    private class PrintSrvMsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CieBluetoothPrinter.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case CieBluetoothPrinter.STATE_CONNECTED:
                            setStatusMsg("connected: " + mConnectedDeviceName);
                            break;
                        case CieBluetoothPrinter.STATE_CONNECTING:
                            setStatusMsg("connecting...");
                            break;
                        case CieBluetoothPrinter.STATE_LISTEN:
                            setStatusMsg("listening..." + mConnectedDeviceName);
                            break;
                        case CieBluetoothPrinter.STATE_NONE:
                            setStatusMsg("not connected");
                            break;
                    }
                    break;
                case CieBluetoothPrinter.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(
                            CieBluetoothPrinter.DEVICE_NAME);
                    break;
                case CieBluetoothPrinter.MESSAGE_STATUS:
                    setStatusMsg(msg.getData().getString(
                            CieBluetoothPrinter.STATUS_TEXT));
                    break;
                case CieBluetoothPrinter.PRINT_COMPLETE:
                    setStatusMsg("PRINT OK");
                    break;
                case CieBluetoothPrinter.PRINTER_CONNECTION_CLOSED:
                    setStatusMsg("Printer Connection closed");
                    break;
                case CieBluetoothPrinter.PRINTER_DISCONNECTED:
                    setStatusMsg("Printer Connection failed");
                    break;
                default:
                    setStatusMsg("Some un handled message : " + msg.what);
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private void setStatusMsg(String msg) {
        statusMsg.setText(msg);
    }

    private class AsyncPrint extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnPrint.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            //Optional
            mPrinter.setPrintMode(BtpConsts.PRINT_IN_BATCH);
            //mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_48MM);

            mPrinter.resetPrinter();
            mPrinter.setHighIntensity();
            mPrinter.setAlignmentCenter();
            mPrinter.setBold();
            mPrinter.printTextLine("\nMY COMPANY BILL\n");
            mPrinter.setRegular();
            mPrinter.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            mPrinter.printLineFeed();
            // Bill Header End

            // Bill Details Start

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("Customer Name     : Aditya    \n");
            mPrinter.printTextLine("Customer Order ID : 00067     \n");
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printTextLine("  Item      Quantity     Price\n");
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printTextLine("  Item 1          1       1.00\n");
            mPrinter.printTextLine("  Bags           10    2220.00\n");
            mPrinter.printTextLine("  Next Item     999   99999.00\n");
            mPrinter.printLineFeed();
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printTextLine("  Total              107220.00\n");
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printLineFeed();
            mPrinter.setBold();
            mPrinter.printTextLine("    Thank you ! Visit Again   \n");
            mPrinter.setRegular();
            mPrinter.printLineFeed();
            mPrinter.printTextLine("******************************\n");
            mPrinter.printLineFeed();

            mPrinter.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();

            //Clearance for Paper tear
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            //required only if the Batch Mode is set
            //print all commands
            mPrinter.batchPrint();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btnPrint.setEnabled(true);
        }
    }
}
