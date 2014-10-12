package com.example.android.nsdchat;

import android.app.Activity;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.nsdchat.NsdHelper;

public class NsdChatActivity extends Activity
{

    // Helper
    NsdHelper mNsdHelper;

    // 消息展示列表
    private TextView mStatusView;

    // 主线程消息更新
    private Handler mUpdateHandler;

    public static final String TAG = "NsdChat";

    // 连接聊天连接
    ChatConnection mConnection;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // 展示所有的消息
        mStatusView = (TextView)findViewById(R.id.status);

        // 接收到消息后的展示
        mUpdateHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                // 取出消息
                String chatLine = msg.getData().getString("msg");
                // 添加展示
                addChatLine(chatLine);
            }
        };

        // 启动本地服务
        mConnection = new ChatConnection(mUpdateHandler);

        // 构造及初始化
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();

    }

    // 注册
    // 将本地服务注册广播出去
    public void clickAdvertise(View v)
    {
        if (mConnection.getLocalPort() > -1)
        {
            mNsdHelper.registerService(mConnection.getLocalPort());
        }
        else
        {
            Log.d(TAG, "ServerSocket isn't bound.");
        }
    }

    // 查找
    public void clickDiscover(View v)
    {
        mNsdHelper.discoverServices();
    }

    // 连接
    public void clickConnect(View v)
    {
        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
        if (service != null)
        {
            Log.d(TAG, "Connecting.");
            mConnection.connectToServer(service.getHost(), service.getPort());
        }
        else
        {
            Log.d(TAG, "No service to connect to!");
        }
    }

    // 消息发送
    public void clickSend(View v)
    {
        EditText messageView = (EditText)this.findViewById(R.id.chatInput);
        if (messageView != null)
        {
            String messageString = messageView.getText().toString();
            if (!messageString.isEmpty())
            {
                mConnection.sendMessage(messageString);
            }
            messageView.setText("");
        }
    }

    // 消息更新展示
    public void addChatLine(String line)
    {
        mStatusView.append("\n" + line);
    }

    // 自动发现
    @Override
    protected void onResume()
    {
        super.onResume();
        if (mNsdHelper != null)
        {
            mNsdHelper.discoverServices();
        }
    }

    // 停止自动发现
    @Override
    protected void onPause()
    {
        if (mNsdHelper != null)
        {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        mNsdHelper.tearDown();
        mConnection.tearDown();
        super.onDestroy();
    }
}
