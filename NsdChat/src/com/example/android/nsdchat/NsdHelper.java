/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.nsdchat;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;

public class NsdHelper
{
    public static final String TAG = "NsdHelper";

    // 上下文
    Context mContext;

    // nsd service
    NsdManager mNsdManager;

    // 解析监听
    NsdManager.ResolveListener mResolveListener;

    // 查找监听
    NsdManager.DiscoveryListener mDiscoveryListener;

    // 注册监听
    NsdManager.RegistrationListener mRegistrationListener;

    // 注册类型
    public static final String SERVICE_TYPE = "_http._tcp.";

    // 注册服务名称
    public String mServiceName = "NsdChat";

    NsdServiceInfo mService;

    // 构造方法
    public NsdHelper(Context context)
    {
        mContext = context;
        mNsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
    }

    // 初始化方法
    public void initializeNsd()
    {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();
    }

    // 注册方法
    public void registerService(int port)
    {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    // 注册监听
    public void initializeRegistrationListener()
    {
        mRegistrationListener = new NsdManager.RegistrationListener()
        {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo)
            {
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1)
            {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0)
            {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
            }

        };
    }

    // 注册开始发现服务
    public void discoverServices()
    {
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    // 停止发现服务
    public void stopDiscovery()
    {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    // 服务监听
    public void initializeDiscoveryListener()
    {
        mDiscoveryListener = new NsdManager.DiscoveryListener()
        {

            @Override
            public void onDiscoveryStarted(String regType)
            {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service)
            {
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE))
                {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                }
                else if (service.getServiceName().equals(mServiceName))
                {
                    Log.d(TAG, "Same machine: " + mServiceName);
                }
                else if (service.getServiceName().contains(mServiceName))
                {
                    // 只能后面的服务发现前面的服务！！
                    // 这一步解析有什么意义？？？这一步的service中host ip都没有
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service)
            {
                Log.e(TAG, "service lost" + service);
                if (mService == service)
                {
                    mService = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType)
            {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode)
            {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode)
            {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    // 这方法待研究
    public void initializeResolveListener()
    {
        mResolveListener = new NsdManager.ResolveListener()
        {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo)
            {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName))
                {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
            }
        };
    }

    public NsdServiceInfo getChosenServiceInfo()
    {
        return mService;
    }

    public void tearDown()
    {
        mNsdManager.unregisterService(mRegistrationListener);
    }
}
