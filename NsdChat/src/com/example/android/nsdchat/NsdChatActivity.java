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

    // ��Ϣչʾ�б�
    private TextView mStatusView;

    // ���߳���Ϣ����
    private Handler mUpdateHandler;

    public static final String TAG = "NsdChat";

    // ������������
    ChatConnection mConnection;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // չʾ���е���Ϣ
        mStatusView = (TextView)findViewById(R.id.status);

        // ���յ���Ϣ���չʾ
        mUpdateHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                // ȡ����Ϣ
                String chatLine = msg.getData().getString("msg");
                // ���չʾ
                addChatLine(chatLine);
            }
        };

        // �������ط���
        mConnection = new ChatConnection(mUpdateHandler);

        // ���켰��ʼ��
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();

    }

    // ע��
    // �����ط���ע��㲥��ȥ
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

    // ����
    public void clickDiscover(View v)
    {
        mNsdHelper.discoverServices();
    }

    // ����
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

    // ��Ϣ����
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

    // ��Ϣ����չʾ
    public void addChatLine(String line)
    {
        mStatusView.append("\n" + line);
    }

    // �Զ�����
    @Override
    protected void onResume()
    {
        super.onResume();
        if (mNsdHelper != null)
        {
            mNsdHelper.discoverServices();
        }
    }

    // ֹͣ�Զ�����
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
