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

    // ���ط���
    private ChatServer mChatServer;

    // ���ӵ����ط����Client
    private ChatClient mChatClient;

    private static final String TAG = "ChatConnection";

    // �뱾��Server���ӵ�Client��socket
    private Socket mSocket;

    // �״򲻶������˿�
    private int mPort = -1;

    // ���췽��
    public ChatConnection(Handler handler)
    {
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
    }

    // Server�ඨ��
    private class ChatServer
    {
        // Server�����Ǹ���Socket
        ServerSocket mServerSocket = null;

        // Server��Ӧ���߳�
        Thread mThread = null;

        // ���췽��
        public ChatServer(Handler handler)
        {
            mThread = new Thread(new ServerThread());
            mThread.start();
        }

        // �ر�Server
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

        // Server����߳�
        class ServerThread implements Runnable
        {
            @Override
            public void run()
            {
                try
                {
                    // ��ϵͳ�Զ�����˿ں�
                    mServerSocket = new ServerSocket(0);
                    // ����Server�Ķ˿ں�
                    setLocalPort(mServerSocket.getLocalPort());

                    while (!Thread.currentThread().isInterrupted())
                    {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        // �ȴ�Client�����
                        setSocket(mServerSocket.accept());
                        Log.d(TAG, "Connected.");
                        // ֻ����һ�Ρ��������������demo
                        if (mChatClient == null)
                        {
                            int port = mSocket.getPort();
                            InetAddress address = mSocket.getInetAddress();
                            // ���Ӧ������ΪconnectToClient��
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

    // Client����
    // socket client��Ӧ��Socket
    private synchronized void setSocket(Socket socket)
    {
        Log.d(TAG, "setSocket being called.");
        if (socket == null)
        {
            Log.d(TAG, "Setting a null socket.");
        }
        // ����Ѿ���Client���ӵ��˱��������ȶϿ�
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
        // �����µ�Client
        mSocket = socket;
    }

    public void tearDown()
    {
        mChatServer.tearDown();
        mChatClient.tearDown();
    }

    // ��������Ǹ��õģ���ֱ��һ�繷ʺ
    // ���������ΪServerʱ���÷���ʵ�ֵ�������Client
    // ��ΪClient�ǣ��÷���ʵ�ֵ�������Server
    public void connectToServer(InetAddress address, int port)
    {
        mChatClient = new ChatClient(address, port);
    }

    // ChatClient�ඨ��
    private class ChatClient
    {

        private InetAddress mAddress;

        private int PORT;

        private final String CLIENT_TAG = "ChatClient";

        private Thread mSendThread;

        private Thread mRecThread;

        // ���췽��
        public ChatClient(InetAddress address, int port)
        {
            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }

        // ��Client�����շ���Ϣ
        class SendingThread implements Runnable
        {

            // ��Ϣ���У����ڷ�����Ϣ
            BlockingQueue<String> mMessageQueue;

            // ������10
            private int QUEUE_CAPACITY = 10;

            // ���췽��
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
                        // ���ڿյ����������Client����Server
                        setSocket(new Socket(mAddress, PORT));
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");

                    }
                    else
                    {
                        // ����Server���ӵ�Client
                        Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
                    }

                    // ����һ���̣߳�ר������������Ϣ
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
                        // ����Ϣ������ȡ����Ϣ���ͳ�ȥ
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

        // ������Ϣ���߳�
        class ReceivingThread implements Runnable
        {
            @Override
            public void run()
            {
                BufferedReader input;
                try
                {
                    // ������
                    input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted())
                    {
                        // һֱ��ȡ��Ϣ
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

        // ������Ϣ
        public void sendMessage(String msg)
        {
            try
            {
                // ��ȡSocket
                Socket socket = getSocket();
                if (socket == null)
                {
                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
                }
                else if (socket.getOutputStream() == null)
                {
                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
                }

                // ��ȡ�����д��Ϣ
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(getSocket()
                        .getOutputStream())), true);
                out.println(msg);
                out.flush();
                // ����д��ͬʱ��Ҳ���Լ����ֻ���չʾһ��
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

    // ��Activity�õ���Ϣ���ͷ���
    public void sendMessage(String msg)
    {
        if (mChatClient != null)
        {
            mChatClient.sendMessage(msg);
        }
    }

    // ��ȡ����Service�Ķ˿ں�
    public int getLocalPort()
    {
        return mPort;
    }

    // ���ñ���Service�Ķ˿ں�
    public void setLocalPort(int port)
    {
        mPort = port;
    }

    // �ֻ��ϵ���Ϣ����
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
