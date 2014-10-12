package com.example.android.nsdchat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChatConnection
{

    private Handler mUpdateHandler;

    // 本地服务
    private ChatServer mChatServer;

    // 连接到本地服务的Client
    private ChatClient mChatClient;

    private static final String TAG = "ChatConnection";

    // 与本地Server连接的Client侧socket
    private Socket mSocket;

    // 雷打不动服务侧端口
    private int mPort = -1;

    // 构造方法
    public ChatConnection(Handler handler)
    {
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
    }

    // Server类定义
    private class ChatServer
    {
        // Server对于那个的Socket
        ServerSocket mServerSocket = null;

        // Server对应的线程
        Thread mThread = null;

        // 构造方法
        public ChatServer(Handler handler)
        {
            mThread = new Thread(new ServerThread());
            mThread.start();
        }

        // 关闭Server
        public void tearDown()
        {
            mThread.interrupt();
            try
            {
                mServerSocket.close();
            }
            catch (IOException ioe)
            {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        // Server侧的线程
        class ServerThread implements Runnable
        {
            @Override
            public void run()
            {
                try
                {
                    // 让系统自动非配端口号
                    mServerSocket = new ServerSocket(0);
                    // 设置Server的端口号
                    setLocalPort(mServerSocket.getLocalPort());

                    while (!Thread.currentThread().isInterrupted())
                    {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        // 等待Client侧接入
                        setSocket(mServerSocket.accept());
                        Log.d(TAG, "Connected.");
                        // 只能玩一次。。。。。这就是demo
                        if (mChatClient == null)
                        {
                            int port = mSocket.getPort();
                            InetAddress address = mSocket.getInetAddress();
                            // 这个应该命名为connectToClient！
                            connectToServer(address, port);
                        }
                    }
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            }
        }
    }

    // Client接入
    // socket client对应的Socket
    private synchronized void setSocket(Socket socket)
    {
        Log.d(TAG, "setSocket being called.");
        if (socket == null)
        {
            Log.d(TAG, "Setting a null socket.");
        }
        // 如果已经有Client连接到了本机，则先断开
        if (mSocket != null)
        {
            if (mSocket.isConnected())
            {
                try
                {
                    mSocket.close();
                }
                catch (IOException e)
                {
                    // TODO(alexlucas): Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        // 保存新的Client
        mSocket = socket;
    }

    public void tearDown()
    {
        mChatServer.tearDown();
        mChatClient.tearDown();
    }

    // 这个方法是复用的，简直是一坨狗屎
    // 如果本机作为Server时，该方法实现的是连接Client
    // 做为Client是，该方法实现的是连接Server
    public void connectToServer(InetAddress address, int port)
    {
        mChatClient = new ChatClient(address, port);
    }

    // ChatClient类定义
    private class ChatClient
    {

        private InetAddress mAddress;

        private int PORT;

        private final String CLIENT_TAG = "ChatClient";

        private Thread mSendThread;

        private Thread mRecThread;

        // 构造方法
        public ChatClient(InetAddress address, int port)
        {
            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }

        // 跟Client进行收发消息
        class SendingThread implements Runnable
        {

            // 消息队列，用于发送消息
            BlockingQueue<String> mMessageQueue;

            // 长度是10
            private int QUEUE_CAPACITY = 10;

            // 构造方法
            public SendingThread()
            {
                mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
            }

            @Override
            public void run()
            {
                try
                {
                    if (getSocket() == null)
                    {
                        // 等于空的情况，代表Client连接Server
                        setSocket(new Socket(mAddress, PORT));
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");

                    }
                    else
                    {
                        // 代表Server连接到Client
                        Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
                    }

                    // 再起一个线程，专门用来接收消息
                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                }
                catch (UnknownHostException e)
                {
                    Log.d(CLIENT_TAG, "Initializing socket failed, UHE", e);
                }
                catch (IOException e)
                {
                    Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e);
                }

                while (true)
                {
                    try
                    {
                        // 从消息队列中取出消息发送出去
                        String msg = mMessageQueue.take();
                        sendMessage(msg);
                    }
                    catch (InterruptedException ie)
                    {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }

        // 接收消息的线程
        class ReceivingThread implements Runnable
        {
            @Override
            public void run()
            {
                BufferedReader input;
                try
                {
                    // 输入流
                    input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted())
                    {
                        // 一直读取消息
                        String messageStr = null;
                        messageStr = input.readLine();
                        if (messageStr != null)
                        {
                            Log.d(CLIENT_TAG, "Read from the stream: " + messageStr);
                            updateMessages(messageStr, false);
                        }
                        else
                        {
                            Log.d(CLIENT_TAG, "The nulls! The nulls!");
                            break;
                        }
                    }
                    input.close();

                }
                catch (IOException e)
                {
                    Log.e(CLIENT_TAG, "Server loop error: ", e);
                }
            }
        }

        public void tearDown()
        {
            try
            {
                getSocket().close();
            }
            catch (IOException ioe)
            {
                Log.e(CLIENT_TAG, "Error when closing server socket.");
            }
        }

        // 发送消息
        public void sendMessage(String msg)
        {
            try
            {
                // 获取Socket
                Socket socket = getSocket();
                if (socket == null)
                {
                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
                }
                else if (socket.getOutputStream() == null)
                {
                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
                }

                // 获取输出流写消息
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(getSocket()
                        .getOutputStream())), true);
                out.println(msg);
                out.flush();
                // 向外写的同时，也在自己的手机上展示一下
                updateMessages(msg, true);
            }
            catch (UnknownHostException e)
            {
                Log.d(CLIENT_TAG, "Unknown Host", e);
            }
            catch (IOException e)
            {
                Log.d(CLIENT_TAG, "I/O Exception", e);
            }
            catch (Exception e)
            {
                Log.d(CLIENT_TAG, "Error3", e);
            }
            Log.d(CLIENT_TAG, "Client sent message: " + msg);
        }
    }

    // 给Activity用的消息发送方法
    public void sendMessage(String msg)
    {
        if (mChatClient != null)
        {
            mChatClient.sendMessage(msg);
        }
    }

    // 获取本地Service的端口号
    public int getLocalPort()
    {
        return mPort;
    }

    // 设置本地Service的端口号
    public void setLocalPort(int port)
    {
        mPort = port;
    }

    // 手机上的消息更新
    public synchronized void updateMessages(String msg, boolean local)
    {
        Log.e(TAG, "Updating message: " + msg);

        if (local)
        {
            msg = "me: " + msg;
        }
        else
        {
            msg = "them: " + msg;
        }

        Bundle messageBundle = new Bundle();
        messageBundle.putString("msg", msg);

        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);
    }

    private Socket getSocket()
    {
        return mSocket;
    }
}
