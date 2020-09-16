package example.com.classicbluetooth.service;

/**
 * Created by clara.tong on 2018/4/3.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.icu.text.LocaleDisplayNames;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SyncStateContract;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import example.com.classicbluetooth.MyApplication;
import example.com.classicbluetooth.constant.Constants;

/**
 * 这个类完成了设置和管理蓝牙的所有工作 与其他设备的连接。
 * 它有一个监听传入的线程 连接、用于连接设备的线程和线程
 * 连接时执行数据传输。
 */

public class BluetoothChatService {
    private static final String TAG = "BluetoothChatService";

    //Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";  //安全与不安全的问题
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001132-0000-1000-8000-00805F9B34FB");//不同的连接方式
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001132-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBluetoothAdapter = null;
    private Handler mHandler;

    private int mState;
    private static BluetoothChatService mChatService;

    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    //连接状态
    public static final int STATE_NONE = 0; //无连接
    public static final int STATE_LISTEN = 1; //监听连接状态
    public static final int STATE_CONNECTING = 2;//正在连接
    public static final int STATE_CONNECTED = 3;//已连接


    public BluetoothChatService(Context context, Handler mHandler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        this.mHandler = mHandler;
    }

    public static BluetoothChatService getInstance(Context context, Handler handler) {
        synchronized (BluetoothChatService.class) {
            if (mChatService == null) {
                mChatService = new BluetoothChatService(context, handler);
            }
        }
        return mChatService;
    }

    /**/
    private synchronized void setState(int state) {
        Log.d(TAG, "setState: " + mState + "->" + state);
        mState = state;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    /*连接失败*/
    private void connectionFailed() {
        MyApplication.getInstance().setConnectState(false);
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "连接失败");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        Log.e("socket", "socket==null");
        setState(STATE_NONE);
    }

    /**
     * 断开连接
     */
    private void connectionLost() {
        MyApplication.getInstance().setConnectState(false);
        Log.e(TAG, "connectionLost:-------》》》》连接已断开 ");
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "连接已断开");
        msg.setData(bundle);
        setState(Constants.CONNECT_LOST);
        mHandler.sendMessage(msg);
    }

    /**
     * 启动连接线程，连接蓝牙设备
     *
     * @param device
     *            需要连接的目标设备
     * @param secure
     *            Socket Security type - Secure (true) , Insecure (false)
     *            参考官方文档，暂时不知道啥用处
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // 取消任何连接的线程
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // 取消当前运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // 启动线程与目标设备连接
   /*     mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();*/

        mSecureAcceptThread = new AcceptThread(true);
        mSecureAcceptThread.start();
        setState(STATE_CONNECTING);
    }
    /**
     * 启动ConnectedThread线程，开始管理蓝牙连接
     *
     * @param socket 蓝牙套接字
     * @param device 目标设备
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {

        // 取消完成连接的线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 取消当前运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 取消接收线程，只连接到一个设备
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }


        // 发送消息给activity，已经连接到了设备
        Message msg = mHandler.obtainMessage();
        msg.what = Constants.STATE_CONNECTED;
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME,device.getName());
        bundle.putString(Constants.DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
        //在MyApplication设置为已连接状态（全局管理）
        MyApplication.getInstance().setConnectState(true);

        // 启动线程来管理连接并执行数据发送与接收
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

    }

    /**
     * 停止所有线程
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) { //已连接
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) { //服务端线程 获取服务端BluetoothSocket
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
        MyApplication.getInstance().setConnectState(false);
    }
    /**
     * 向目标设备发送数据
     *
     * @param out
     *            数据
     */
    public void write(byte[] out){
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                setState(STATE_NONE);
                return;
            }
            r = mConnectedThread;
        }
        r.write(out);
        mHandler.obtainMessage(Constants.MESSAGE_WRITE,-1,-1,out).sendToTarget();//msg.what,msg.arg1.msg.arg2,msg.obj
    }

    /*服务端线程，获取服务端的BluetoothServiceSocket*/
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            /*1、通过调用listenUsingRfcommWithServiceRecord(String, UUID)获取BluetoothServerSocket。
            * String是你服务的可辨别名称。系统将会自动写入一个新的“服务发现协议(Service Discovery Protocol 简称SDP)”数据库入口至你的设备，该名称可随意命名，
            * UUID也包括SDP的入口，并作为和客户端连接基础。*/
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Socket Type:" + mSocketType + "listen failed");
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            setName("AcceptThread" + mSocketType);
            BluetoothSocket socket = null;
            while (mState != STATE_CONNECTED) {
                try {
                    //通过调用accept()监听连接请求
                    //阻塞的调用，将会在抛出异常或者连接被接受时返回。
                    // 连接只有在远程设备发送一个带有和服务器端已注册的UUID相匹配的连接请求时才会被接受。
                    // 当连接成功时,accept()将会返回一个已经连接的BluetoothSocket。
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    // e.printStackTrace();
                    Log.e(TAG, " Accept Thread run:，socket==null 连接失败");
                    setState(STATE_NONE);
                    connectionFailed();
                    break;
                }

                //连接成功
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                //已经连接
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "不能关闭socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel(){
            Log.i(TAG, "close: activity Thread" );
            try{
                if (mmServerSocket !=null)
                    mmServerSocket.close();
            }catch (IOException e){
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    /*客户端线程，获取客户端BluetoothSocket*/
    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device,boolean secure) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ?"Secure":"Insecure";
            try{
               // 使用BluetoothDevice，通过调用createRfcommSocketToServiceRecord(UUID)得到BluetoothSocket。
                if (secure){
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }else{
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: fail" );
                e.printStackTrace();
            }
            mmSocket = tmp;
            //连接建立之前先配对
            if(device.getBondState() == BluetoothDevice.BOND_NONE){
                Method creMethod;
                try{
                    creMethod = BluetoothDevice.class.getMethod("createBond");
                    creMethod.invoke(device);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }else {

            }
        }

        @Override
        public void run() {
            setName("ConnectThread"+mSocketType);
            if (mBluetoothAdapter != null){
                //取消扫描设备，以免减慢连接速度
                mBluetoothAdapter.cancelDiscovery();
            }
            try{
                //通过调用connect()方法初始化连接。
                mmSocket.connect();
            } catch (IOException e) {
                try{
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "run: unable to close" );
                    e1.printStackTrace();
                }
                Log.e(TAG, "初始化连接失败 " );
                connectionFailed();
                return;
            }
            //重置
            synchronized (BluetoothChatService.this){
                mConnectThread = null;
            }
            //连接建立，开始传输
            connected(mmSocket,mmDevice,mSocketType);
        }

        public void cancel(){
            try{
                if(mmSocket !=null)
                   mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "close() of connect" + mSocketType + "socket failed",e);
            }
        }
    }

/* 数据发送与接收线程
*当成功进行设备间的连接时，每一个设备都持有一个已连接的BluetoothSocket。*/
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket,String socketType) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        //获取输入输出流
        try{
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        Log.d(TAG, " connected run:state "+mState);
        while (mState == STATE_CONNECTED){
            try{
                if (mmInStream != null){
                    //从InputStream读取数据
                    bytes = mmInStream.read(buffer);
                    Log.d(TAG, "receive data: "+bytes);
                    mHandler.obtainMessage(Constants.MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                }
            } catch (IOException e) {
                connectionLost();
                break;
            }
        }
    }

    /*写入到连接的OutStream，写入数据*/
    public void write(byte[] buffer){
        try{
            if (mmOutStream != null){
                mmOutStream.write(buffer);
            }else{
            }
        } catch (IOException e) {
            Log.e(TAG, "disconneted",e );
            connectionLost();
        }
    }

    public void cancel(){
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "close() of connect socket failed",e);
        }
    }
}









}
